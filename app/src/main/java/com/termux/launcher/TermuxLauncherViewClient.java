package com.termux.launcher;

import android.content.Context;
import android.media.AudioManager;
import android.view.InputDevice;
import android.view.KeyEvent;

import com.termux.shared.termux.terminal.TermuxTerminalViewClientBase;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.KeyHandler;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;

public class TermuxLauncherViewClient extends TermuxTerminalViewClientBase {

    private final TermuxLauncherView mView;
    private final TermuxLauncherSessionClient mTermuxLauncherSessionClient;

    /**
     * Keeping track of the special keys acting as Ctrl, Fn and Alt for the soft keyboard and other hardware keys.
     */
    private boolean mVirtualControlKeyDown, mVirtualFnKeyDown, mVirtualAltKeyDown;

    public TermuxLauncherViewClient(TermuxLauncherView view, TermuxLauncherSessionClient termuxLauncherSessionClient) {
        mView = view;
        mTermuxLauncherSessionClient = termuxLauncherSessionClient;
    }

    /**
     * Should be called when TermuxLauncherView.initFloatView() is called
     */
    public void initFloatView() {
        if (mView.getPreferences() != null) {
            mView.getTerminalView().setTextSize(mView.getPreferences().getFontSize());
            boolean isKeyLoggingEnabled = mView.getPreferences().isTerminalViewKeyLoggingEnabled(true);
            mView.getTerminalView().setIsTerminalViewKeyLoggingEnabled(isKeyLoggingEnabled);
        }
    }

    /**
     * Should be called when {@link com.termux.view.TerminalView#mEmulator} is set
     */
    @Override
    public void onEmulatorSet() {
        // Evitar aplicar estilos/fuentes demasiado pronto durante el arranque.
    }



    @Override
    public float onScale(float scale) {
        if (scale < 0.9f || scale > 1.1f) {
            boolean increase = scale > 1.f;
            changeFontSize(increase);
            return 1.0f;
        }
        return scale;
    }

    @Override
    public boolean shouldBackButtonBeMappedToEscape() {
        return mView.getProperties() != null && mView.getProperties().isBackKeyTheEscapeKey();
    }

    @Override
    public boolean shouldEnforceCharBasedInput() {
        return mView.getProperties() != null && mView.getProperties().isEnforcingCharBasedInput();
    }

    @Override
    public boolean shouldUseCtrlSpaceWorkaround() {
        return mView.getProperties() != null && mView.getProperties().isUsingCtrlSpaceWorkaround();
    }

    @Override
    public void copyModeChanged(boolean copyMode) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e, TerminalSession session) {
        if (handleVirtualKeys(keyCode, e, true)) return true;

        if ((mView.getProperties() == null || !mView.getProperties().areHardwareKeyboardShortcutsDisabled()) &&
                e.isCtrlPressed() && e.isAltPressed()) {
            // Get the unmodified code point:
            int unicodeChar = e.getUnicodeChar(0);

            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN || unicodeChar == 'n'/* next */) {
                // TODO: Toggle minimized or not.
            } else if (unicodeChar == 'f'/* full screen */) {
                // TODO: Toggle full screen.
            } else if (unicodeChar == 'k'/* keyboard */) {
                KeyboardUtils.toggleSoftKeyboard(mView.getContext());
            } else if (unicodeChar == '+' || e.getUnicodeChar(KeyEvent.META_SHIFT_ON) == '+') {
                // We also check for the shifted char here since shift may be required to produce '+',
                // see https://github.com/termux/termux-api/issues/2
                changeFontSize(true);
            } else if (unicodeChar == '-') {
                changeFontSize(false);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent e) {
        return handleVirtualKeys(keyCode, e, false);
    }


    @Override
    public boolean readControlKey() {
        return mVirtualControlKeyDown;
    }

    public void setVirtualControlKeyDown(boolean down) {
        mVirtualControlKeyDown = down;
    }

    @Override
    public boolean readAltKey() {
        return mVirtualAltKeyDown;
    }

    public void setVirtualAltKeyDown(boolean down) {
        mVirtualAltKeyDown = down;
    }

    @Override
    public boolean onCodePoint(int codePoint, boolean ctrlDown, TerminalSession session) {
        if (mVirtualFnKeyDown) {
            int resultingKeyCode = -1;
            int resultingCodePoint = -1;
            boolean altDown = false;
            int lowerCase = Character.toLowerCase(codePoint);
            switch (lowerCase) {
                // Arrow keys.
                case 'w':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_UP;
                    break;
                case 'a':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                    break;
                case 's':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                    break;
                case 'd':
                    resultingKeyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                    break;

                // Page up and down.
                case 'p':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_UP;
                    break;
                case 'n':
                    resultingKeyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                    break;

                // Some special keys:
                case 't':
                    resultingKeyCode = KeyEvent.KEYCODE_TAB;
                    break;
                case 'i':
                    resultingKeyCode = KeyEvent.KEYCODE_INSERT;
                    break;
                case 'h':
                    resultingCodePoint = '~';
                    break;

                // Special characters to input.
                case 'u':
                    resultingCodePoint = '_';
                    break;
                case 'l':
                    resultingCodePoint = '|';
                    break;

                // Function keys.
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    resultingKeyCode = (codePoint - '1') + KeyEvent.KEYCODE_F1;
                    break;
                case '0':
                    resultingKeyCode = KeyEvent.KEYCODE_F10;
                    break;

                // Other special keys.
                case 'e':
                    resultingCodePoint = /*Escape*/ 27;
                    break;
                case '.':
                    resultingCodePoint = /*^.*/ 28;
                    break;

                case 'b': // alt+b, jumping backward in readline.
                case 'f': // alf+f, jumping forward in readline.
                case 'x': // alt+x, common in emacs.
                    resultingCodePoint = lowerCase;
                    altDown = true;
                    break;

                // Volume control.
                case 'v':
                    resultingCodePoint = -1;
                    AudioManager audio = (AudioManager) mView.getContext().getSystemService(Context.AUDIO_SERVICE);
                    audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME, AudioManager.USE_DEFAULT_STREAM_TYPE, AudioManager.FLAG_SHOW_UI);
                    break;
            }

            if (resultingKeyCode != -1) {
                TerminalEmulator term = session.getEmulator();
                session.write(KeyHandler.getCode(resultingKeyCode, 0, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode()));
            } else if (resultingCodePoint != -1) {
                session.writeCodePoint(altDown, resultingCodePoint);
            }
            return true;
        }

        return false;
    }

    /**
     * Handle dedicated volume buttons as virtual keys if applicable.
     */
    private boolean handleVirtualKeys(int keyCode, KeyEvent event, boolean down) {
        InputDevice inputDevice = event.getDevice();
        if (mView.getProperties() != null && mView.getProperties().areVirtualVolumeKeysDisabled()) {
            return false;
        } else if (inputDevice != null && inputDevice.getKeyboardType() == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
            // Do not steal dedicated buttons from a full external keyboard.
            return false;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            mVirtualControlKeyDown = down;
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            mVirtualFnKeyDown = down;
            return true;
        }
        return false;
    }



    public void changeFontSize(boolean increase) {
        if (mView.getPreferences() == null) return;
        mView.getPreferences().changeFontSize(increase);
        mView.getTerminalView().setTextSize(mView.getPreferences().getFontSize());
    }
}
