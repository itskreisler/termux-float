package com.termux.launcher;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.termux.shared.logger.Logger;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE;
import com.termux.shared.termux.shell.command.environment.TermuxShellEnvironment;
import com.termux.shared.termux.shell.command.runner.terminal.TermuxSession;
import com.termux.terminal.TerminalSession;

public class TermuxFloatService extends Service {

    /** Vista de terminal activa (vinculada desde la actividad). */
    private TermuxFloatView mTermuxFloatView;

    private TermuxSession mSession;

    private static final String LOG_TAG = "TermuxFloatService";

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        TermuxFloatService getService() {
            return TermuxFloatService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        runStartForeground();
        TermuxFloatApplication.setLogConfig(this, false);
        Logger.logVerbose(LOG_TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");
        runStartForeground();

        if (intent != null) {
            String action = intent.getAction();
            if (TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE.equals(action)) {
                actionStopService();
            }
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.logVerbose(LOG_TAG, "onDestroy");
        runStopForeground();
    }

    /** Asocia la vista de terminal con el servicio para recibir callbacks. */
    public void setTermuxFloatView(TermuxFloatView view) {
        this.mTermuxFloatView = view;
    }

    /** Solicita detener el servicio. */
    public void requestStopService() {
        Logger.logDebug(LOG_TAG, "requestStopService");
        runStopForeground();
        stopSelf();
    }

    private void actionStopService() {
        if (mSession != null)
            mSession.killIfExecuting(this, false);
        requestStopService();
    }

    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_ID, buildNotification());
    }

    private void runStopForeground() {
        stopForeground(true);
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationUtils.setupNotificationChannel(this,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW);
    }

    private Notification buildNotification() {
        String notificationText = getString(R.string.notification_message_running);

        Intent exitIntent = new Intent(this, TermuxFloatService.class)
                .setAction(TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE);
        PendingIntent exitPendingIntent = PendingIntent.getService(this, 0, exitIntent,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0);

        Notification.Builder builder = NotificationUtils.geNotificationBuilder(this,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID,
                Notification.PRIORITY_LOW,
                TermuxConstants.TERMUX_FLOAT_APP_NAME,
                notificationText, null, null, null,
                NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null) return null;

        builder.setShowWhen(false);
        builder.setSmallIcon(R.mipmap.ic_service_notification);
        builder.setColor(0xFF000000);
        builder.setOngoing(true);
        builder.addAction(android.R.drawable.ic_delete,
                getString(R.string.notification_action_exit), exitPendingIntent);

        return builder.build();
    }

    public void ensureSessionExists() {
        if (mSession == null) {
            createTermuxSession(
                    new ExecutionCommand(0, null, null, null, null,
                            ExecutionCommand.Runner.TERMINAL_SESSION.getName(), false),
                    null);
        }
    }

    /** Crea una nueva {@link TermuxSession}. */
    @Nullable
    public synchronized TermuxSession createTermuxSession(ExecutionCommand executionCommand,
                                                          String sessionName) {
        if (executionCommand == null) return null;
        Logger.logDebug(LOG_TAG, "createTermuxSession: " + executionCommand.getCommandIdAndLabelLogString());

        if (ExecutionCommand.Runner.APP_SHELL.getName().equals(executionCommand.runner)) {
            Logger.logDebug(LOG_TAG, "Ignorando comando de fondo");
            return null;
        }

        executionCommand.shellName = sessionName;
        executionCommand.terminalTranscriptRows = (mTermuxFloatView != null && mTermuxFloatView.getProperties() != null)
                ? mTermuxFloatView.getProperties().getTerminalTranscriptRows()
                : 2000;

        TermuxFloatSessionClient sessionClient = (mTermuxFloatView != null
            && mTermuxFloatView.getTermuxFloatSessionClient() != null)
                ? mTermuxFloatView.getTermuxFloatSessionClient()
                : new TermuxFloatSessionClient(this, null);

        TermuxSession newSession = TermuxSession.execute(this, executionCommand,
                sessionClient, null, new TermuxShellEnvironment(),
                null, executionCommand.isPluginExecutionCommand);

        if (newSession == null) {
            Logger.logError(LOG_TAG, "No se pudo crear la sesión: " + executionCommand.getCommandIdAndLabelLogString());
            return null;
        }

        mSession = newSession;

        return mSession;
    }

    public TermuxSession getTermuxSession() {
        return mSession;
    }

    public TerminalSession getCurrentSession() {
        return mSession != null ? mSession.getTerminalSession() : null;
    }
}
