package com.termux.launcher.launcher.model;

import androidx.annotation.NonNull;

public final class AppRef {
    public final String packageName;
    public final String activityName;
    public final int userId;
    public final long userSerialNumber;
    public final boolean clonedProfile;
    public final String profileLabel;

    public AppRef(@NonNull String packageName, @NonNull String activityName) {
        this(packageName, activityName, -1, -1L, false, "");
    }

    public AppRef(@NonNull String packageName, @NonNull String activityName,
                  int userId, long userSerialNumber, boolean clonedProfile,
                  @NonNull String profileLabel) {
        this.packageName = packageName;
        this.activityName = activityName;
        this.userId = userId;
        this.userSerialNumber = userSerialNumber;
        this.clonedProfile = clonedProfile;
        this.profileLabel = profileLabel;
    }

    @NonNull
    public String stableId() {
        String base = packageName + "/" + activityName;
        if (userId < 0) {
            if (clonedProfile && userSerialNumber >= 0) {
                return base + "#userSerial=" + userSerialNumber;
            }
            return base;
        }
        return base + "#user=" + userId;
    }

    @NonNull
    public AppRef copy() {
        return new AppRef(packageName, activityName, userId, userSerialNumber, clonedProfile, profileLabel);
    }
}
