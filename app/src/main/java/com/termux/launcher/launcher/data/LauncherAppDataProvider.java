package com.termux.launcher.launcher.data;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;

import androidx.annotation.NonNull;

import com.termux.launcher.launcher.model.AppRef;
import com.termux.launcher.launcher.model.LauncherAppEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class LauncherAppDataProvider {
    private final Context context;
    private final List<LauncherAppEntry> cachedApps = new ArrayList<>();
    private final Map<String, LauncherAppEntry> cachedById = new LinkedHashMap<>();
    private final Map<Character, List<LauncherAppEntry>> letterBuckets = new HashMap<>();

    public LauncherAppDataProvider(@NonNull Context context) {
        this.context = context;
    }

    public synchronized void invalidate() {
        cachedApps.clear();
        cachedById.clear();
        letterBuckets.clear();
    }

    public synchronized List<LauncherAppEntry> getAllApps() {
        if (cachedApps.isEmpty()) {
            loadAppsLocked();
        }
        return new ArrayList<>(cachedApps);
    }

    public synchronized LauncherAppEntry findByRef(@NonNull AppRef ref) {
        if (cachedApps.isEmpty()) {
            loadAppsLocked();
        }
        return cachedById.get(ref.stableId());
    }

    public synchronized List<LauncherAppEntry> getAppsForLetter(char letter) {
        if (cachedApps.isEmpty()) {
            loadAppsLocked();
        }
        char normalized = normalizeLetter(letter);
        List<LauncherAppEntry> bucket = letterBuckets.get(normalized);
        return bucket == null ? new ArrayList<>() : new ArrayList<>(bucket);
    }

    private void loadAppsLocked() {
        PackageManager packageManager = context.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> launchables = packageManager.queryIntentActivities(main, 0);
        Collections.sort(launchables, new ResolveInfo.DisplayNameComparator(packageManager));

        for (ResolveInfo resolveInfo : launchables) {
            ActivityInfo info = resolveInfo.activityInfo;
            if (info == null || info.packageName == null || info.name == null) continue;
            String label = info.loadLabel(packageManager) != null ? info.loadLabel(packageManager).toString() : info.packageName;
            AppRef ref = new AppRef(info.packageName, info.name);
            LauncherAppEntry entry = new LauncherAppEntry(ref, label, info.loadIcon(packageManager));
            addEntryToCache(entry);
        }

        addProfileApps();
    }

    private void addEntryToCache(LauncherAppEntry entry) {
        cachedApps.add(entry);
        cachedById.put(entry.appRef.stableId(), entry);
        char key = normalizeLetter(entry.label.isEmpty() ? '#' : entry.label.charAt(0));
        List<LauncherAppEntry> bucket = letterBuckets.get(key);
        if (bucket == null) {
            bucket = new ArrayList<>();
            letterBuckets.put(key, bucket);
        }
        bucket.add(entry);
    }

    private void addProfileApps() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        try {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
            if (launcherApps == null || userManager == null) {
                return;
            }
            LinkedHashSet<UserHandle> profiles = new LinkedHashSet<>();
            List<UserHandle> userManagerProfiles = userManager.getUserProfiles();
            if (userManagerProfiles != null) {
                profiles.addAll(userManagerProfiles);
            }
            List<UserHandle> launcherProfiles = launcherApps.getProfiles();
            if (launcherProfiles != null) {
                profiles.addAll(launcherProfiles);
            }
            if (profiles.isEmpty()) {
                return;
            }
            UserHandle currentUser = Process.myUserHandle();
            for (UserHandle profile : profiles) {
                if (profile == null || profile.equals(currentUser)) {
                    continue;
                }
                addProfileAppsForUser(launcherApps, userManager, profile);
            }
        } catch (Throwable ignored) {
        }
    }

    private void addProfileAppsForUser(LauncherApps launcherApps, UserManager userManager, UserHandle profile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        try {
            List<LauncherActivityInfo> activities = launcherApps.getActivityList(null, profile);
            if (activities == null || activities.isEmpty()) {
                return;
            }
            int userId = userIdOf(profile);
            long serial = userManager.getSerialNumberForUser(profile);
            String suffix = profileSuffix(userId, serial);
            for (LauncherActivityInfo activity : activities) {
                if (activity == null || activity.getComponentName() == null) continue;
                String packageName = activity.getComponentName().getPackageName();
                String activityName = activity.getComponentName().getClassName();
                if (packageName == null || packageName.isEmpty() || activityName == null || activityName.isEmpty()) {
                    continue;
                }
                String rawLabel = activity.getLabel() != null ? activity.getLabel().toString() : packageName;
                String label = rawLabel + suffix;
                AppRef ref = new AppRef(packageName, activityName, userId, serial, true, suffix.trim());
                Drawable icon = null;
                try {
                    icon = activity.getIcon(0);
                } catch (Throwable ignored) {
                }
                LauncherAppEntry entry = new LauncherAppEntry(ref, label, icon);
                addEntryToCache(entry);
            }
        } catch (Throwable ignored) {
        }
    }

    @NonNull
    private static String profileSuffix(int userId, long serial) {
        if (userId >= 0) {
            return " · Clon " + userId;
        }
        if (serial >= 0) {
            return " · Clon " + serial;
        }
        return " · Clon";
    }

    private static java.lang.reflect.Method sGetIdentifierMethod;
    private static boolean sGetIdentifierResolved;

    public static int userIdOf(@NonNull UserHandle userHandle) {
        try {
            if (!sGetIdentifierResolved) {
                sGetIdentifierResolved = true;
                sGetIdentifierMethod = UserHandle.class.getMethod("getIdentifier");
            }
            if (sGetIdentifierMethod == null) return -1;
            Object result = sGetIdentifierMethod.invoke(userHandle);
            return result instanceof Integer ? (Integer) result : -1;
        } catch (Throwable ignored) {
            sGetIdentifierMethod = null;
            return -1;
        }
    }

    private static char normalizeLetter(char c) {
        char upper = Character.toUpperCase(c);
        if (upper >= 'A' && upper <= 'Z') {
            return upper;
        }
        return '#';
    }

    public static char normalizeLetter(@NonNull String label) {
        if (label.isEmpty()) return '#';
        return normalizeLetter(label.toUpperCase(Locale.US).charAt(0));
    }
}
