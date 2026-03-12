package com.termux.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.termux.shared.logger.Logger;
import com.termux.shared.view.KeyboardUtils;
import com.termux.view.TerminalView;
import com.termux.launcher.settings.properties.TermuxLauncherAppSharedProperties;
import com.termux.launcher.settings.preferences.TermuxLauncherAppSharedPreferences;

/**
 * Vista principal del launcher que aloja la terminal.
 */
public class TermuxLauncherView extends LinearLayout {

    private TerminalView mTerminalView;

    TermuxLauncherViewClient mTermuxLauncherViewClient;
    TermuxLauncherSessionClient mTermuxLauncherSessionClient;

    private TermuxLauncherAppSharedPreferences mPreferences;
    private TermuxLauncherAppSharedProperties mProperties;

    /** Detector de gestos: doble toque muestra el teclado. */
    private GestureDetector mGestureDetector;

    private static final String LOG_TAG = "TermuxLauncherView";

    public TermuxLauncherView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Debe llamarse cuando el servicio está listo.
     */
    public void initFloatView(TermuxLauncherService service) {
        Logger.logDebug(LOG_TAG, "initFloatView");

        mProperties = new TermuxLauncherAppSharedProperties(getContext());
        mPreferences = TermuxLauncherAppSharedPreferences.build(getContext(), true);
        if (mPreferences == null) {
            Logger.logError(LOG_TAG, "mPreferences nulo — preferencias no disponibles, usando defaults");
        }

        // Siempre inicializamos el cliente de sesión (necesario aunque no haya preferencias)
        mTermuxLauncherSessionClient = new TermuxLauncherSessionClient(service, this);

        // Siempre buscamos la vista del terminal
        mTerminalView = findViewById(R.id.terminal_view);
        if (mTerminalView == null) {
            Logger.logError(LOG_TAG, "terminal_view no encontrado en el layout");
            return;
        }

        mTermuxLauncherViewClient = new TermuxLauncherViewClient(this, mTermuxLauncherSessionClient);
        mTerminalView.setTerminalViewClient(mTermuxLauncherViewClient);
        mTermuxLauncherViewClient.initFloatView();

        // Doble toque para mostrar el teclado táctil
        mGestureDetector = new GestureDetector(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                showTouchKeyboard();
                return true;
            }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (mTermuxLauncherSessionClient != null)
            mTermuxLauncherSessionClient.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTermuxLauncherSessionClient != null)
            mTermuxLauncherSessionClient.onDetachedFromWindow();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mGestureDetector != null) mGestureDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    void showTouchKeyboard() {
        if (mTerminalView == null) return;
        mTerminalView.post(() -> KeyboardUtils.showSoftKeyboard(getContext(), mTerminalView));
    }

    void hideTouchKeyboard() {
        if (mTerminalView == null) return;
        mTerminalView.post(() -> KeyboardUtils.hideSoftKeyboard(getContext(), mTerminalView));
    }

    public boolean isVisible() {
        return isAttachedToWindow() && isShown();
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxLauncherViewClient getTermuxLauncherViewClient() {
        return mTermuxLauncherViewClient;
    }

    public TermuxLauncherSessionClient getTermuxLauncherSessionClient() {
        return mTermuxLauncherSessionClient;
    }

    public TermuxLauncherAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxLauncherAppSharedProperties getProperties() {
        return mProperties;
    }

    public void reloadViewStyling() {
        if (mTermuxLauncherSessionClient != null)
            mTermuxLauncherSessionClient.onReload();
    }
}
