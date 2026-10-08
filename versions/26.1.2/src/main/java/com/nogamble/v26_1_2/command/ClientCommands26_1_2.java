package com.nogamble.v26_1_2.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.nogamble.NoGamble;
import com.nogamble.config.ModConfig;
import com.nogamble.profile.ProfileManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Set;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class ClientCommands26_1_2 {

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> {
            dispatcher.register(literal("nogamble")
                .then(literal("status").executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    NoGamble mod = NoGamble.getInstance();
                    ModConfig config = mod.getConfig();
                    ProfileManager pm = mod.getProfileManager();

                    boolean enabled = config != null && config.isEnabled();
                    source.sendFeedback(Component.literal("§6[NoGamble] §eStatus: " + (enabled ? "§aENABLED" : "§cDISABLED")));

                    if (pm != null) {
                        String srv = pm.getCurrentServerAddress();
                        source.sendFeedback(Component.literal("§6[NoGamble] §eServer: §f" + (srv.isEmpty() ? "Singleplayer / None" : srv)));
                        List<String> profiles = pm.getActiveProfileNames();
                        source.sendFeedback(Component.literal("§6[NoGamble] §eActive Profile(s): §f" + (profiles.isEmpty() ? "None" : String.join(", ", profiles))));
                    }

                    Set<String> blocked = mod.getAllCurrentlyBlockedCommands();
                    source.sendFeedback(Component.literal("§6[NoGamble] §eTotal Blocked Commands: §f" + blocked.size()));
                    return 1;
                }))
                .then(literal("refresh").executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    source.sendFeedback(Component.literal("§6[NoGamble] §eRefreshing profiles from remote repository..."));
                    NoGamble.getInstance().manualRefresh(msg -> {
                        source.sendFeedback(Component.literal("§6[NoGamble] §f" + msg));
                    });
                    return 1;
                }))
                .then(literal("list").executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    Set<String> blocked = NoGamble.getInstance().getAllCurrentlyBlockedCommands();
                    if (blocked.isEmpty()) {
                        source.sendFeedback(Component.literal("§6[NoGamble] §eNo commands are currently blocked."));
                    } else {
                        source.sendFeedback(Component.literal("§6[NoGamble] §eBlocked commands (" + blocked.size() + "): §f/" + String.join(", /", blocked)));
                    }
                    return 1;
                }))
                .then(literal("toggle").executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    boolean newState = NoGamble.getInstance().toggleEnabled();
                    source.sendFeedback(Component.literal("§6[NoGamble] §eMod is now " + (newState ? "§aENABLED" : "§cDISABLED")));
                    return 1;
                }))
                .then(literal("add").then(argument("command", StringArgumentType.word()).executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    String cmd = StringArgumentType.getString(ctx, "command");
                    boolean added = NoGamble.getInstance().addLocalBlocked(cmd);
                    if (added) {
                        source.sendFeedback(Component.literal("§6[NoGamble] §aAdded §f/" + cmd + " §ato local blocked list."));
                    } else {
                        source.sendFeedback(Component.literal("§6[NoGamble] §eCommand §f/" + cmd + " §ewas already on local blocked list."));
                    }
                    return 1;
                })))
                .then(literal("remove").then(argument("command", StringArgumentType.word()).executes(ctx -> {
                    FabricClientCommandSource source = ctx.getSource();
                    String cmd = StringArgumentType.getString(ctx, "command");
                    boolean removed = NoGamble.getInstance().removeLocalBlocked(cmd);
                    if (removed) {
                        source.sendFeedback(Component.literal("§6[NoGamble] §aRemoved §f/" + cmd + " §afrom local blocked list."));
                    } else {
                        source.sendFeedback(Component.literal("§6[NoGamble] §eCommand §f/" + cmd + " §ewas not found in local blocked list."));
                    }
                    return 1;
                })))
            );
        });
    }
}
