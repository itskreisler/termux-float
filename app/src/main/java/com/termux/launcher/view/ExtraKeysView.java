package com.termux.launcher.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import com.termux.view.TerminalView;
import com.termux.terminal.KeyHandler;
import com.termux.terminal.TerminalEmulator;
import com.termux.terminal.TerminalSession;
import com.termux.launcher.TermuxLauncherView;

import java.util.ArrayList;
import java.util.List;

/**
 * View that displays extra keys for the terminal, such as ESC, CTRL, ALT, etc.
 */
public class ExtraKeysView extends HorizontalScrollView {

    public interface SpecialKeyListener {
        boolean readControlKey();
        boolean readAltKey();
        void onTerminalInteraction();
    }

    private LinearLayout mContainer;
    private TerminalView mTerminalView;
    private SpecialKeyListener mSpecialKeyListener;

    public ExtraKeysView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContainer = new LinearLayout(context);
        mContainer.setOrientation(LinearLayout.HORIZONTAL);
        mContainer.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
        mContainer.setGravity(Gravity.CENTER_VERTICAL);
        addView(mContainer);
        setHorizontalScrollBarEnabled(false);

        setupDefaultKeys();
    }

    public void setTerminalView(TerminalView terminalView) {
        mTerminalView = terminalView;
    }

    public void setSpecialKeyListener(SpecialKeyListener listener) {
        mSpecialKeyListener = listener;
    }

    private void setupDefaultKeys() {
        String[] keys = {"ESC", "CTRL", "ALT", "TAB", "HOME", "END", "PGUP", "PGDN", "↑", "↓", "←", "→"};
        for (String key : keys) {
            addButton(key);
        }
    }

    private void toggleControl() {
        if (mTerminalView == null || !(mTerminalView.getParent() instanceof TermuxLauncherView)) return;
        TermuxLauncherView launcherView = (TermuxLauncherView) mTerminalView.getParent();
        if (launcherView.getTermuxLauncherViewClient() == null) return;

        com.termux.launcher.TermuxLauncherViewClient client = launcherView.getTermuxLauncherViewClient();
        boolean newState = !client.readControlKey();
        client.mVirtualControlKeyDown = newState;
        updateButtonsHighlight();
    }

    private void toggleAlt() {
        if (mTerminalView == null || !(mTerminalView.getParent() instanceof TermuxLauncherView)) return;
        TermuxLauncherView launcherView = (TermuxLauncherView) mTerminalView.getParent();
        if (launcherView.getTermuxLauncherViewClient() == null) return;

        com.termux.launcher.TermuxLauncherViewClient client = launcherView.getTermuxLauncherViewClient();
        boolean newState = !client.readAltKey();
        client.mVirtualAltKeyDown = newState;
        updateButtonsHighlight();
    }

    private void updateButtonsHighlight() {
        for (int i = 0; i < mContainer.getChildCount(); i++) {
            View v = mContainer.getChildAt(i);
            if (v instanceof Button) {
                Button b = (Button) v;
                String text = b.getText().toString();
                if ("CTRL".equals(text)) {
                    boolean active = (mSpecialKeyListener != null && mSpecialKeyListener.readControlKey());
                    b.setBackgroundColor(active ? 0x88FFFFFF : 0x00000000);
                } else if ("ALT".equals(text)) {
                    boolean active = (mSpecialKeyListener != null && mSpecialKeyListener.readAltKey());
                    b.setBackgroundColor(active ? 0x88FFFFFF : 0x00000000);
                }
            }
        }
    }

    private void addButton(final String text) {
        Button button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
        button.setText(text);
        button.setTextColor(0xFFC0B18B); // Match SuggestionBarView.TEXT_COLOR
        button.setTextSize(12f);
        button.setPadding(0, 0, 0, 0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        params.setMargins(8, 0, 8, 0);
        button.setLayoutParams(params);

        button.setOnClickListener(v -> {
            if (mTerminalView == null) return;
            TerminalSession session = mTerminalView.getCurrentSession();
            if (session == null) return;

            if (mSpecialKeyListener != null) {
                mSpecialKeyListener.onTerminalInteraction();
            }

            int keyCode = -1;

            switch (text) {
                case "ESC": keyCode = KeyEvent.KEYCODE_ESCAPE; break;
                case "TAB": keyCode = KeyEvent.KEYCODE_TAB; break;
                case "HOME": keyCode = KeyEvent.KEYCODE_MOVE_HOME; break;
                case "END": keyCode = KeyEvent.KEYCODE_MOVE_END; break;
                case "PGUP": keyCode = KeyEvent.KEYCODE_PAGE_UP; break;
                case "PGDN": keyCode = KeyEvent.KEYCODE_PAGE_DOWN; break;
                case "↑": keyCode = KeyEvent.KEYCODE_DPAD_UP; break;
                case "↓": keyCode = KeyEvent.KEYCODE_DPAD_DOWN; break;
                case "←": keyCode = KeyEvent.KEYCODE_DPAD_LEFT; break;
                case "→": keyCode = KeyEvent.KEYCODE_DPAD_RIGHT; break;
                case "CTRL":
                    toggleControl();
                    return;
                case "ALT":
                    toggleAlt();
                    return;
            }

            if (keyCode != -1) {
                TerminalEmulator term = session.getEmulator();
                boolean controlDown = (mSpecialKeyListener != null && mSpecialKeyListener.readControlKey());
                boolean altDown = (mSpecialKeyListener != null && mSpecialKeyListener.readAltKey());

                int metaState = 0;
                if (controlDown) metaState |= KeyEvent.META_CTRL_ON | KeyEvent.META_CTRL_LEFT_ON;
                if (altDown) metaState |= KeyEvent.META_ALT_ON | KeyEvent.META_ALT_LEFT_ON;

                session.write(KeyHandler.getCode(keyCode, metaState, term.isCursorKeysApplicationMode(), term.isKeypadApplicationMode()));
            }
        });

        mContainer.addView(button);
    }
}
