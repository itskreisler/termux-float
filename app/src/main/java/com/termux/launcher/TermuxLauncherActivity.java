package com.termux.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.ViewTreeObserver;

import com.termux.shared.shell.command.ExecutionCommand;

/**
 * Actividad que puede actuar como launcher y aloja la vista {@link TermuxLauncherView}.
 */
public class TermuxLauncherActivity extends Activity {

    private TermuxLauncherView mTermuxLauncherView;
    private TermuxLauncherService mService;
    private boolean mIsBound = false;

    private final BroadcastReceiver mExitReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!TermuxLauncherService.ACTION_EXIT_APP.equals(intent.getAction())) return;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                finishAndRemoveTask();
            } else {
                finish();
            }
        }
    };

    private void attachTerminalSessionWhenReady() {
        if (mService == null || mTermuxLauncherView == null || mTermuxLauncherView.getTerminalView() == null) return;
        if (mService.getCurrentSession() == null) return;

        mTermuxLauncherView.getTerminalView().post(() -> {
            if (mTermuxLauncherView == null || mTermuxLauncherView.getTerminalView() == null || mService == null) return;
            if (mService.getCurrentSession() == null) return;

            mTermuxLauncherView.getTerminalView().attachSession(mService.getCurrentSession());
            mTermuxLauncherView.getTerminalView().requestFocus();
        });
    }

    // Conexión al servicio para gestionar la sesión de terminal
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermuxLauncherService.LocalBinder binder = (TermuxLauncherService.LocalBinder) service;
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
        mTermuxLauncherView = findViewById(R.id.window_layout);
        registerReceiver(mExitReceiver, new IntentFilter(TermuxLauncherService.ACTION_EXIT_APP));

        // Iniciamos y nos vinculamos al servicio
        Intent intent = new Intent(this, TermuxLauncherService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        // Refrescar el terminal cada vez que el layout cambia (ej: teclado abre/cierra)
        mTermuxLauncherView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (mTermuxLauncherView != null && mTermuxLauncherView.getTerminalView() != null) {

                    mTermuxLauncherView.getTerminalView().postInvalidate();
                }
            }
        });
    }

    /**
     * Inicializa la terminal y vincula la sesión actual.
     */
    private void initializeTerminal() {
        if (mService == null || mTermuxLauncherView == null) return;

        mTermuxLauncherView.initFloatView(mService);
        if (mTermuxLauncherView.getTerminalView() == null) return;

        // Vincula la vista al servicio para usar el cliente de sesión correcto
        mService.setLauncherView(mTermuxLauncherView);

        // Desactivar la capa de hardware para forzar redibujado correcto en tiempo real
        mTermuxLauncherView.getTerminalView().setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null);

        // Creamos una nueva sesión si no existe una
        if (mService.getTermuxSession() == null) {
            String defaultWorkingDirectory = mTermuxLauncherView.getProperties() != null
                ? mTermuxLauncherView.getProperties().getDefaultWorkingDirectory()
                : null;
            mService.createTermuxSession(
                new ExecutionCommand(0, null, null, null, defaultWorkingDirectory, ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false), null);
        }

        attachTerminalSessionWhenReady();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxLauncherApplication.setLogConfig(this, false);
        if (mTermuxLauncherView != null && mTermuxLauncherView.getTerminalView() != null) {
            mTermuxLauncherView.getTerminalView().requestFocus();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mExitReceiver);
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
