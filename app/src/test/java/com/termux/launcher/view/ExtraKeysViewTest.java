package com.termux.launcher.view;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class ExtraKeysViewTest {

    private ExtraKeysView extraKeysView;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        extraKeysView = new ExtraKeysView(context, null);
    }

    @Test
    public void testKeysAreInitialized() {
        LinearLayout container = (LinearLayout) extraKeysView.getChildAt(0);
        assertNotNull(container);

        // Expected keys: "ESC", "CTRL", "ALT", "TAB", "HOME", "END", "PGUP", "PGDN", "↑", "↓", "←", "→"
        String[] expectedKeys = {"ESC", "CTRL", "ALT", "TAB", "HOME", "END", "PGUP", "PGDN", "↑", "↓", "←", "→"};
        assertEquals(expectedKeys.length, container.getChildCount());

        for (int i = 0; i < expectedKeys.length; i++) {
            Button button = (Button) container.getChildAt(i);
            assertEquals(expectedKeys[i], button.getText().toString());
        }
    }
}
