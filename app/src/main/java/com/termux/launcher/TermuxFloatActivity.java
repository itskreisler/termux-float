package com.termux.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;

import com.termux.shared.shell.command.ExecutionCommand;

/**
 * Activity which can act as a launcher and hosts the {@link TermuxFloatView}.
 */
public class TermuxFloatActivity extends Activity {

    private TermuxFloatView mTermuxFloatView;
    private TermuxFloatService mService;
    private boolean mIsBound = false;

    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermuxFloatService.LocalBinder binder = (TermuxFloatService.LocalBinder) service;
            mService = binder.getService();
            mIsBound = true;
            mService.setLauncherActivityActive(true);
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

        // Disable floating window behavior when in Activity
        mTermuxFloatView.setIsLauncherMode(true);

        Intent intent = new Intent(this, TermuxFloatService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    private void initializeTerminal() {
        if (mService == null || mTermuxFloatView == null) return;

        mTermuxFloatView.initFloatView(mService);

        if (mService.getTermuxSession() == null) {
            mService.createTermuxSession(
                new ExecutionCommand(0, null, null, null, mTermuxFloatView.getProperties().getDefaultWorkingDirectory(), ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false), null);
        }

        if (mService.getCurrentSession() != null) {
            mTermuxFloatView.getTerminalView().attachSession(mService.getCurrentSession());
        }
        mTermuxFloatView.reloadViewStyling();
        mTermuxFloatView.showTouchKeyboard();
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxFloatApplication.setLogConfig(this, false);
        if (mTermuxFloatView != null) {
            mTermuxFloatView.showTouchKeyboard();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mIsBound) {
            if (mService != null) {
                mService.setLauncherActivityActive(false);
            }
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    @Override
    public void onBackPressed() {
        // Do nothing to prevent exiting the launcher
    }
}
