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
import android.view.KeyEvent;
import android.view.ViewTreeObserver;

import com.termux.shared.shell.command.ExecutionCommand;
import com.termux.launcher.launcher.data.LauncherAppDataProvider;
import com.termux.launcher.launcher.data.LauncherConfigRepository;
import com.termux.launcher.view.ExtraKeysView;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Actividad que puede actuar como launcher y aloja la vista {@link TermuxLauncherView}.
 */
public class TermuxLauncherActivity extends Activity {

    private TermuxLauncherView mTermuxLauncherView;
    private TermuxLauncherService mService;
    private boolean mIsBound = false;
    private SuggestionBarView mSuggestionBarView;
    private AzScrubRowView mAzScrubRowView;
    private ExtraKeysView mExtraKeysView;

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
        mSuggestionBarView = findViewById(R.id.suggestion_bar);
        mAzScrubRowView = findViewById(R.id.az_scrub_row);
        mExtraKeysView = findViewById(R.id.extra_keys);
        setupSuggestionBar();
        setupAzScrubRow();
        setupExtraKeys();
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

    private void setupSuggestionBar() {
        if (mSuggestionBarView == null) return;

        final android.content.SharedPreferences prefs = getSharedPreferences("termux_launcher_prefs", MODE_PRIVATE);
        LauncherConfigRepository.PreferencesStore store = new LauncherConfigRepository.PreferencesStore() {
            @Override
            public String getPinnedItemsV2() {
                return prefs.getString("app_launcher_pinned_items_v2", "");
            }

            @Override
            public void setPinnedItemsV2(String value) {
                prefs.edit().putString("app_launcher_pinned_items_v2", value).apply();
            }

            @Override
            public void setPinnedItemsSchemaVersion(int version) {
                prefs.edit().putInt("app_launcher_pinned_items_schema_version", version).apply();
            }

            @Override
            public String getLegacyDefaultButtons() {
                return prefs.getString("app_launcher_default_buttons", "");
            }
        };

        mSuggestionBarView.setAppDataProvider(new LauncherAppDataProvider(this));
        mSuggestionBarView.setConfigRepository(new LauncherConfigRepository(store));
        List<String> defaults = new ArrayList<>();
        defaults.add("Apps");
        defaults.add("Search");
        defaults.add("Tools");
        mSuggestionBarView.setDefaultButtons(defaults);
        mSuggestionBarView.reloadAllApps();
        mSuggestionBarView.reload();
        syncAzScrubLettersAndTint();
    }

    private void setupAzScrubRow() {
        if (mAzScrubRowView == null) return;
        mAzScrubRowView.setScrubCallback(new AzScrubRowView.ScrubCallback() {
            @Override
            public void onScrub(char letter, int selectionIndex, boolean commit) {
                if (mSuggestionBarView == null) return;
                if (letter == AzScrubRowView.PINNED_APPS_SYMBOL) {
                    mSuggestionBarView.clearAzPreview();
                    return;
                }
                mSuggestionBarView.persistAzPreview(letter, selectionIndex);
            }

            @Override
            public void onCancel() {
                if (mSuggestionBarView != null) {
                    mSuggestionBarView.clearAzPreview();
                }
            }
        });
        syncAzScrubLettersAndTint();
    }

    private void setupExtraKeys() {
        if (mExtraKeysView == null || mTermuxLauncherView == null) return;
        mExtraKeysView.setTerminalView(mTermuxLauncherView.getTerminalView());
        mExtraKeysView.setSpecialKeyListener(new ExtraKeysView.SpecialKeyListener() {
            @Override
            public boolean readControlKey() {
                if (mTermuxLauncherView != null && mTermuxLauncherView.getTermuxLauncherViewClient() != null) {
                    return mTermuxLauncherView.getTermuxLauncherViewClient().readControlKey();
                }
                return false;
            }

            @Override
            public boolean readAltKey() {
                if (mTermuxLauncherView != null && mTermuxLauncherView.getTermuxLauncherViewClient() != null) {
                    return mTermuxLauncherView.getTermuxLauncherViewClient().readAltKey();
                }
                return false;
            }

            @Override
            public void onTerminalInteraction() {
                if (mSuggestionBarView != null) {
                    mSuggestionBarView.onTerminalInteraction();
                }
            }
        });
    }

    private void syncAzScrubLettersAndTint() {
        if (mAzScrubRowView == null || mSuggestionBarView == null) return;
        Set<Character> letters = new LinkedHashSet<>(mSuggestionBarView.getAvailableAzLetters());
        mAzScrubRowView.setVisibleLetters(letters);
        mAzScrubRowView.setTextColor(getResources().getColor(android.R.color.darker_gray));
        mAzScrubRowView.setInteractionAccentColor(getResources().getColor(android.R.color.white));
        mAzScrubRowView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
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

        if (mSuggestionBarView != null) {
            mSuggestionBarView.reloadWithInput("", mTermuxLauncherView.getTerminalView());
            syncAzScrubLettersAndTint();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TermuxLauncherApplication.setLogConfig(this, false);
        if (mTermuxLauncherView != null && mTermuxLauncherView.getTerminalView() != null) {
            mTermuxLauncherView.getTerminalView().requestFocus();
            if (mSuggestionBarView != null) {
                mSuggestionBarView.reloadWithInput("", mTermuxLauncherView.getTerminalView());
                syncAzScrubLettersAndTint();
            }
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mSuggestionBarView != null && event != null) {
            int unicode = event.getUnicodeChar();
            if (unicode > 0) {
                char inputChar = (char) unicode;
                if (Character.isLetterOrDigit(inputChar) || inputChar == '#') {
                    mSuggestionBarView.previewAzLetter(inputChar, 0, false);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
