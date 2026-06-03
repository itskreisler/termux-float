package com.termux.launcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class EspressoTest {

    @Rule
    public ActivityScenarioRule<TermuxLauncherActivity> activityRule =
            new ActivityScenarioRule<>(TermuxLauncherActivity.class);

    @Test
    public void testMainViewIsDisplayed() {
        onView(withId(R.id.window_layout)).check(matches(isDisplayed()));
    }
}
