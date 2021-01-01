package com.jw.studio.geckodevmaster;


import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SessionSwitchTest {

    @Rule
    public ActivityTestRule<CompatibilityActivity> mActivityTestRule = new ActivityTestRule<>(CompatibilityActivity.class);

    public static ViewAction waitFor(long delay) {
        return new ViewAction() {
            @Override public Matcher<View> getConstraints() {
                return ViewMatchers.isRoot();
            }

            @Override public String getDescription() {
                return "wait for " + delay + "milliseconds";
            }

            @Override public void perform(UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(delay);
            }
        };
    }

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.WRITE_EXTERNAL_STORAGE");

    @Test
    public void sessionActivityTest() {
        ViewInteraction button = onView(
                allOf(withId(R.id.button_google), withText("Google"),
                        childAtPosition(
                                allOf(withId(R.id.webshortcut_row),
                                        childAtPosition(
                                                withId(R.id.home_layout),
                                                0)),
                                0),
                        isDisplayed()));
        button.perform(click());

        ViewInteraction imageButton = onView(
                allOf(withId(R.id.menu_button),
                        childAtPosition(
                                allOf(withId(R.id.main),
                                        childAtPosition(
                                                withId(android.R.id.content),
                                                0)),
                                2),
                        isDisplayed()));
        imageButton.perform(click());

        ViewInteraction button2 = onView(
                allOf(withId(R.id.newtab_button), withText(R.string.new_tab),
                        childAtPosition(
                                withId(R.id.app_popupmenu),
                                1),
                        isDisplayed()));
        button2.perform(click());
        onView(isRoot()).perform(waitFor(1500));

        ViewInteraction button3 = onView(
                allOf(withId(R.id.button_youtube), withText("Youtube"),
                        childAtPosition(
                                allOf(withId(R.id.webshortcut_row),
                                        childAtPosition(
                                                withId(R.id.home_layout),
                                                0)),
                                1),
                        isDisplayed()));
        button3.perform(click());
        onView(isRoot()).perform(waitFor(1500));

        ViewInteraction button4 = onView(
                allOf(withId(R.id.tabs_button), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar_layout),
                                        childAtPosition(
                                                withId(R.id.toolbar),
                                                0)),
                                2),
                        isDisplayed()));
        button4.perform(click());
        onView(isRoot()).perform(waitFor(1500));

        ViewInteraction textView = onView(
                allOf(withId(android.R.id.title), withText("Google"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        textView.perform(click());
        onView(isRoot()).perform(waitFor(1500));

        ViewInteraction button5 = onView(
                allOf(withId(R.id.tabs_button), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar_layout),
                                        childAtPosition(
                                                withId(R.id.toolbar),
                                                0)),
                                2),
                        isDisplayed()));
        button5.perform(click());
        onView(isRoot()).perform(waitFor(1000));

        ViewInteraction textView2 = onView(
                allOf(withId(android.R.id.title), withText("홈 - YouTube"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        textView2.perform(click());

        ViewInteraction button6 = onView(
                allOf(withId(R.id.tabs_button), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar_layout),
                                        childAtPosition(
                                                withId(R.id.toolbar),
                                                0)),
                                2),
                        isDisplayed()));
        button6.perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(android.R.id.title), withText("Google"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                0),
                        isDisplayed()));
        textView3.perform(click());
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
