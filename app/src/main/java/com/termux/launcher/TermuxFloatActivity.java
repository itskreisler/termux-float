package com.termux.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewTreeObserver;

import com.termux.shared.shell.command.ExecutionCommand;

/**
 * Actividad que puede actuar como launcher y aloja la vista {@link TermuxFloatView}.
 */
public class TermuxFloatActivity extends Activity {

    private TermuxFloatView mTermuxFloatView;
    private TermuxFloatService mService;
    private boolean mIsBound = false;

    private void attachTerminalSessionWhenReady() {
        if (mService == null || mTermuxFloatView == null || mTermuxFloatView.getTerminalView() == null) return;
        if (mService.getCurrentSession() == null) return;

        mTermuxFloatView.getTerminalView().post(() -> {
            if (mTermuxFloatView == null || mTermuxFloatView.getTerminalView() == null || mService == null) return;
            if (mService.getCurrentSession() == null) return;

            mTermuxFloatView.getTerminalView().attachSession(mService.getCurrentSession());
            mTermuxFloatView.getTerminalView().requestFocus();
        });
    }

    // Conexión al servicio para gestionar la sesión de terminal
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermuxFloatService.LocalBinder binder = (TermuxFloatService.LocalBinder) service;
            mService = binder.getService();
            mIsBound = true;
            initializeTerminal();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mIsBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTermuxFloatView = findViewById(R.id.window_layout);

        // Iniciamos y nos vinculamos al servicio
        Intent intent = new Intent(this, TermuxFloatService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Refrescar el terminal cada vez que el layout cambia (ej: teclado abre/cierra)
        mTermuxFloatView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mTermuxFloatView != null && mTermuxFloatView.getTerminalView() != null) {

                    mTermuxFloatView.getTerminalView().postInvalidate();
                }
            }
        });
    }

    /**
     * Inicializa la terminal y vincula la sesión actual.
     */
    private void initializeTerminal() {
        if (mService == null || mTermuxFloatView == null) return;

        mTermuxFloatView.initFloatView(mService);
        if (mTermuxFloatView.getTerminalView() == null) return;

        // Vincula la vista al servicio para usar el cliente de sesión correcto
        mService.setTermuxFloatView(mTermuxFloatView);

        // Desactivar la capa de hardware para forzar redibujado correcto en tiempo real
        mTermuxFloatView.getTerminalView().setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);

        // Creamos una nueva sesión si no existe una
        if (mService.getTermuxSession() == null) {
            String defaultWorkingDirectory = mTermuxFloatView.getProperties() != null
                ? mTermuxFloatView.getProperties().getDefaultWorkingDirectory()
                : null;
            mService.createTermuxSession(
                new ExecutionCommand(0, null, null, null, defaultWorkingDirectory, ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false), null);
        }

        attachTerminalSessionWhenReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxFloatApplication.setLogConfig(this, false);
        if (mTermuxFloatView != null && mTermuxFloatView.getTerminalView() != null) {
            mTermuxFloatView.getTerminalView().requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        // No hacemos nada para evitar que el usuario salga del launcher con el botón atrás
    }
}
