package com.termux.launcher.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;

/**
 * Propiedades locales mínimas para el launcher.
 * Evita cargar configuración desde otros paquetes de Termux.
 */
public class TermuxLauncherAppSharedProperties {

    public TermuxLauncherAppSharedProperties(@NonNull Context context) {
    }

    @Nullable
    public String getDefaultWorkingDirectory() {
        return null;
    }

    public int getTerminalTranscriptRows() {
        return 2000;
    }

    public boolean isBackKeyTheEscapeKey() {
        return false;
    }

    public boolean isEnforcingCharBasedInput() {
        return false;
    }

    public boolean isUsingCtrlSpaceWorkaround() {
        return false;
    }

    public boolean areHardwareKeyboardShortcutsDisabled() {
        return false;
    }

    public boolean areVirtualVolumeKeysDisabled() {
        return false;
    }

    public int getBellBehaviour() {
        return TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_IGNORE;
    }

    @Nullable
    public Integer getTerminalCursorStyle() {
        return null;
    }
}
