package com.termux.launcher;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;

import com.termux.shared.logger.Logger;
import com.termux.shared.view.KeyboardUtils;
import com.termux.view.TerminalView;
import com.termux.launcher.settings.properties.TermuxFloatAppSharedProperties;
import com.termux.launcher.settings.preferences.TermuxFloatAppSharedPreferences;

/**
 * Vista principal del launcher que aloja la terminal.
 */
public class TermuxFloatView extends LinearLayout {

    private TerminalView mTerminalView;

    TermuxFloatViewClient mTermuxFloatViewClient;
    TermuxFloatSessionClient mTermuxFloatSessionClient;

    private TermuxFloatAppSharedPreferences mPreferences;
    private TermuxFloatAppSharedProperties mProperties;

    /** Detector de gestos: doble toque muestra el teclado. */
    private GestureDetector mGestureDetector;

    private static final String LOG_TAG = "TermuxFloatView";

    public TermuxFloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * Debe llamarse cuando el servicio está listo.
     */
    public void initFloatView(TermuxFloatService service) {
        Logger.logDebug(LOG_TAG, "initFloatView");

        mProperties = new TermuxFloatAppSharedProperties(getContext());
        mPreferences = TermuxFloatAppSharedPreferences.build(getContext(), true);
        if (mPreferences == null) {
            Logger.logError(LOG_TAG, "mPreferences nulo — preferencias no disponibles, usando defaults");
        }

        // Siempre inicializamos el cliente de sesión (necesario aunque no haya preferencias)
        mTermuxFloatSessionClient = new TermuxFloatSessionClient(service, this);

        // Siempre buscamos la vista del terminal
        mTerminalView = findViewById(R.id.terminal_view);
        if (mTerminalView == null) {
            Logger.logError(LOG_TAG, "terminal_view no encontrado en el layout");
            return;
        }

        mTermuxFloatViewClient = new TermuxFloatViewClient(this, mTermuxFloatSessionClient);
        mTerminalView.setTerminalViewClient(mTermuxFloatViewClient);
        mTermuxFloatViewClient.initFloatView();

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
        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onDetachedFromWindow();
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

    public TermuxFloatViewClient getTermuxFloatViewClient() {
        return mTermuxFloatViewClient;
    }

    public TermuxFloatSessionClient getTermuxFloatSessionClient() {
        return mTermuxFloatSessionClient;
    }

    public TermuxFloatAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxFloatAppSharedProperties getProperties() {
        return mProperties;
    }

    public void reloadViewStyling() {
        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onReload();
    }
}
