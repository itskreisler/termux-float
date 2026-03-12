package com.termux.launcher.settings.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Preferencias locales mínimas para el launcher.
 * Evita depender de paquetes externos como com.termux.window.
 */
public class TermuxFloatAppSharedPreferences {

    private static final String PREFS_NAME = "termux_launcher_prefs";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_TERMINAL_VIEW_KEY_LOGGING = "terminal_view_key_logging";

    private static final int DEFAULT_FONT_SIZE = 14;
    private static final int MIN_FONT_SIZE = 6;
    private static final int MAX_FONT_SIZE = 32;

    private final SharedPreferences mPrefs;

    private TermuxFloatAppSharedPreferences(@NonNull Context context) {
        mPrefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    @Nullable
    public static TermuxFloatAppSharedPreferences build(@NonNull Context context) {
        return new TermuxFloatAppSharedPreferences(context);
    }

    @Nullable
    public static TermuxFloatAppSharedPreferences build(@NonNull Context context, boolean ignoreErrors) {
        return new TermuxFloatAppSharedPreferences(context);
    }

    public int getFontSize() {
        return mPrefs.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE);
    }

    public boolean isTerminalViewKeyLoggingEnabled(boolean defaultValue) {
        return mPrefs.getBoolean(KEY_TERMINAL_VIEW_KEY_LOGGING, defaultValue);
    }

    public void changeFontSize(boolean increase) {
        int current = getFontSize();
        int updated = increase ? current + 1 : current - 1;
        updated = Math.max(MIN_FONT_SIZE, Math.min(MAX_FONT_SIZE, updated));
        mPrefs.edit().putInt(KEY_FONT_SIZE, updated).apply();
    }
}
