# NoGamble 🎰🚫

A client-side Fabric mod for Minecraft that blocks in-game gambling commands (such as `/cf`, `/coinflip`, `/jackpot`) to prevent accidental or impulsive gambling on servers.

Supports **Minecraft 1.21.11** (Java 21) and **Minecraft 26.1.2** (Java 25) from a clean, unified multi-version codebase.

---

## 🎯 Features

- **Outgoing Command Interception**: Intercepts command executions before they are sent over the network using Fabric API's `ClientSendMessageEvents.ALLOW_COMMAND`.
- **Intelligent Command Matching**:
  - Case-insensitive (matches `/cf`, `/CF`, `/Cf`).
  - Argument agnostic (matches `/cf 100`, `/cf heads 50000`).
  - Namespace tolerant (matches `/server:cf`, `/minecraft:cf`).
- **Tab-Complete / Suggestion Filtering**: Removes blocked commands from autocomplete and chat suggestions where feasible via client mixins.
- **Offline / Built-in Default Profile**: Defaults to blocking `/cf` offline or prior to downloading remote profiles.
- **Auto-Updating Remote Profiles**:
  - Periodically fetches updated community or personal profiles from a raw GitHub JSON URL in the background.
  - Supports `ETag` / `If-None-Match` caching to save bandwidth.
  - Local caching at `config/nogamble/profiles.cache.json` with seamless offline fallback.
  - Server wildcard pattern matching (e.g., `*.donutsmp.net`, `donutsmp.net`, `*`).
  - Active server profiles automatically merge with the `global` profile.
- **Client Commands**: Full in-game control via `/nogamble` commands.
- **Local Overrides**: Add or remove individual commands locally via config or in-game commands.
- **Client-Side Only**: Does not require any server-side companion mod or plugins.

---

## 💻 In-Game Commands

All commands are client-side only:

| Command | Description |
|---|---|
| `/nogamble status` | Shows mod status (enabled/disabled), active server, active profiles, and total blocked command count. |
| `/nogamble refresh` | Triggers an immediate background profile refresh from GitHub and displays the result. |
| `/nogamble list` | Lists all currently blocked commands and aliases. |
| `/nogamble toggle` | Toggles the mod on or off. |
| `/nogamble add <cmd>` | Adds a personal command to the local extra blocked list. |
| `/nogamble remove <cmd>` | Removes a personal command from the local extra blocked list. |

---

## ⚙️ Configuration (`config/nogamble.json`)

```json
{
  "enabled": true,
  "profileUrl": "https://raw.githubusercontent.com/itz0cat/NoGamble/main/profiles.json",
  "refreshMinutes": 60,
  "showBlockMessages": true,
  "localExtraBlocked": [],
  "localAllowlist": []
}
```

- `enabled`: Global toggle for all blocking behavior (`true` / `false`).
- `profileUrl`: Raw HTTPS URL to your hosted `profiles.json`.
- `refreshMinutes`: Frequency of background profile updates (default: 60 minutes).
- `showBlockMessages`: Displays `[NoGamble] Blocked /<cmd>` in chat when an attempt is prevented.
- `localExtraBlocked`: Array of additional commands to block locally.
- `localAllowlist`: Array of commands to always allow, overriding remote profiles.

---

## 🌐 Hosting & Editing Remote `profiles.json`

You can host your profiles directly on GitHub (in a public repo or GitHub Gist).

### 1. JSON Schema

```json
{
  "schema": 1,
  "updated": "2026-10-08",
  "profiles": [
    {
      "id": "donut",
      "name": "DonutSMP",
      "servers": ["donutsmp.net", "*.donutsmp.net"],
      "blockedCommands": ["cf", "coinflip", "jackpot"],
      "blockedAliases": ["flip"]
    },
    {
      "id": "global",
      "name": "Global Default",
      "servers": ["*"],
      "blockedCommands": ["cf"],
      "blockedAliases": []
    }
  ]
}
```

### 2. Matching Rules
- `servers`:
  - `*`: Matches every server (global fallback).
  - `*.domain.com`: Matches `domain.com` and all subdomains like `play.domain.com`.
  - `domain.com`: Matches `domain.com` (port is automatically stripped).
- When connecting to a server, NoGamble activates every profile matching that server and merges them with the `global` profile.

### 3. Setup on GitHub
1. Fork or create a repository on GitHub (or a public Gist).
2. Commit your `profiles.json` to the default branch (e.g. `main`).
3. Click **Raw** on GitHub to get the direct raw URL (e.g. `https://raw.githubusercontent.com/itz0cat/NoGamble/main/profiles.json`).
4. Set `"profileUrl"` in `config/nogamble.json` to your raw URL, or run `/nogamble refresh`.

---

## 🏗️ Project Architecture & Builds

This repository targets two distinct Minecraft versions from a single codebase:
- **Minecraft 1.21.11**: Java 21, mapped with Yarn (`1.21.11+build.6`), Fabric API `0.141.6+1.21.11`.
- **Minecraft 26.1.2**: Java 25, unobfuscated Mojang / official names, non-remap Loom setup, Fabric API `0.155.3+26.1.2`.

### Directory Layout

```
nogamble/
├── .github/workflows/build.yml   # CI pipeline for building & publishing releases
├── common/                       # Shared code (config, profiles, HTTP fetcher, matcher)
├── versions/
│   ├── 1.21.11/                  # 1.21.11 mod entrypoint, mixins & compat
│   └── 26.1.2/                   # 26.1.2 mod entrypoint, mixins & compat
├── profiles.json                 # Reference profiles file
└── build.gradle                  # Root build script
```

### Building Locally

#### Build Minecraft 1.21.11 (Requires JDK 21)
```bash
./gradlew :versions:1.21.11:build
```
Output: `versions/1.21.11/build/libs/nogamble-1.0.0+mc1.21.11.jar`

#### Build Minecraft 26.1.2 (Requires JDK 25)
```bash
./gradlew :versions:26.1.2:build
```
Output: `versions/26.1.2/build/libs/nogamble-1.0.0+mc26.1.2.jar`

#### Build Both Versions
```bash
./gradlew buildMods
```
Collects both compiled jars into the root `build/libs/` directory.

---

## 🚀 GitHub Actions & CI/CD

A fully automated CI/CD pipeline is configured at `.github/workflows/build.yml`:

- **Gradle Wrapper Validation**: Verifies Gradle wrapper checksums for security.
- **Gradle Caching**: Caches dependencies and build outputs across runs.
- **Matrix Builds**:
  - Compiles `1.21.11` on Java 21 (`actions/setup-java` Temurin 21).
  - Compiles `26.1.2` on Java 25 (`actions/setup-java` Temurin 25).
- **Artifact Uploads**: Jars are uploaded as workflow artifacts on every `push` and `pull_request`.
- **Automated GitHub Releases**:
  - Pushing a version tag (e.g. `v1.0.0`) automatically publishes a GitHub Release with both mod jars attached.

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## 📄 License

This mod is open-source under the [MIT License](LICENSE).
