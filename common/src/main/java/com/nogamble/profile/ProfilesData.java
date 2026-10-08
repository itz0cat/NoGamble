package com.nogamble.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-level structure of the profiles JSON document.
 */
public class ProfilesData {
    private int schema = 1;
    private String updated;
    private List<Profile> profiles = new ArrayList<>();

    public ProfilesData() {
    }

    public ProfilesData(int schema, String updated, List<Profile> profiles) {
        this.schema = schema;
        this.updated = updated;
        this.profiles = profiles != null ? profiles : new ArrayList<>();
    }

    public int getSchema() {
        return schema;
    }

    public String getUpdated() {
        return updated;
    }

    public List<Profile> getProfiles() {
        return profiles != null ? Collections.unmodifiableList(profiles) : Collections.emptyList();
    }
}
