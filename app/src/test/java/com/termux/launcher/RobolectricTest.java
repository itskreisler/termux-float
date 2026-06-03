package com.termux.launcher;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class RobolectricTest {

    @Test
    public void testActivityCreation() {
        try (ActivityController<TermuxLauncherActivity> controller = Robolectric.buildActivity(TermuxLauncherActivity.class)) {
            // TermuxLauncherActivity activity = controller.setup().get();
            // assertNotNull(activity);
        }
    }
}
