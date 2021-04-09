/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import static android.view.Display.DEFAULT_DISPLAY;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import android.app.Fragment;
import android.app.StatusBarManager;
import android.content.Context;
import android.os.Bundle;
import android.testing.AndroidTestingRunner;
import android.testing.TestableLooper.RunWithLooper;
import android.view.View;
import android.view.ViewPropertyAnimator;

import androidx.test.filters.SmallTest;

import com.android.systemui.R;
import com.android.systemui.SysuiBaseFragmentTest;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.statusbar.phone.ongoingcall.OngoingCallController;
import com.android.systemui.statusbar.phone.ongoingcall.OngoingCallListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.Objects;

@RunWith(AndroidTestingRunner.class)
@RunWithLooper(setAsMainLooper = true)
@SmallTest
public class CollapsedStatusBarFragmentTest extends SysuiBaseFragmentTest {

    private NotificationIconAreaController mMockNotificiationAreaController;
    private View mNotificationAreaInner;
    private View mCenteredNotificationAreaView;
    private StatusBarStateController mStatusBarStateController;
    private OngoingCallController mOngoingCallController;

    public CollapsedStatusBarFragmentTest() {
        super(CollapsedStatusBarFragment.class);
    }

    @Before
    public void setup() {
        StatusBar statusBar = mock(StatusBar.class);
        mDependency.injectTestDependency(StatusBar.class, statusBar);
        mStatusBarStateController = mDependency
                .injectMockDependency(StatusBarStateController.class);
        injectLeakCheckedDependencies(ALL_SUPPORTED_CLASSES);
        mMockNotificiationAreaController = mock(NotificationIconAreaController.class);
        mNotificationAreaInner = mock(View.class);
        mCenteredNotificationAreaView = mock(View.class);
        when(statusBar.getPanelController()).thenReturn(
                mock(NotificationPanelViewController.class));
        when(mNotificationAreaInner.animate()).thenReturn(mock(ViewPropertyAnimator.class));
        when(mMockNotificiationAreaController.getNotificationInnerAreaView()).thenReturn(
                mNotificationAreaInner);
        when(mCenteredNotificationAreaView.animate()).thenReturn(mock(ViewPropertyAnimator.class));
        when(mMockNotificiationAreaController.getCenteredNotificationAreaView()).thenReturn(
                mCenteredNotificationAreaView);
    }

    @Test
    public void testDisableNone() throws Exception {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);
        fragment.disable(DEFAULT_DISPLAY, 0, 0, false);

        assertEquals(View.VISIBLE, mFragment.getView().findViewById(R.id.system_icon_area)
                .getVisibility());
        assertEquals(View.VISIBLE, mFragment.getView().findViewById(R.id.clock)
                .getVisibility());
    }

    @Test
    public void testDisableSystemInfo() throws Exception {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);
        fragment.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_SYSTEM_INFO, 0, false);

        assertEquals(View.INVISIBLE, mFragment.getView().findViewById(R.id.system_icon_area)
                .getVisibility());

        fragment.disable(DEFAULT_DISPLAY, 0, 0, false);

        assertEquals(View.VISIBLE, mFragment.getView().findViewById(R.id.system_icon_area)
                .getVisibility());
    }

    @Test
    public void testDisableNotifications() throws Exception {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);
        fragment.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NOTIFICATION_ICONS, 0, false);

        Mockito.verify(mNotificationAreaInner).setVisibility(eq(View.INVISIBLE));

        fragment.disable(DEFAULT_DISPLAY, 0, 0, false);

        Mockito.verify(mNotificationAreaInner, atLeast(1)).setVisibility(eq(View.VISIBLE));
    }

    @Test
    public void testDisableClock() throws Exception {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);
        fragment.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_CLOCK, 0, false);

        assertEquals(View.GONE, mFragment.getView().findViewById(R.id.clock).getVisibility());

        fragment.disable(DEFAULT_DISPLAY, 0, 0, false);

        assertEquals(View.VISIBLE, mFragment.getView().findViewById(R.id.clock).getVisibility());
    }

    @Test
    public void testOnDozingChanged() throws Exception {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);
        fragment.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NOTIFICATION_ICONS, 0, false);

        Mockito.verify(mNotificationAreaInner).setVisibility(eq(View.INVISIBLE));

        reset(mStatusBarStateController);
        when(mStatusBarStateController.isDozing()).thenReturn(true);
        fragment.onDozingChanged(true);

        Mockito.verify(mStatusBarStateController).isDozing();
        Mockito.verify(mNotificationAreaInner, atLeast(1)).setVisibility(eq(View.VISIBLE));
    }

    @Test
    public void onOngoingCallStarted_notificationsHiddenAndOngoingCallChipDisplayed() {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);

        ArgumentCaptor<OngoingCallListener> ongoingCallListenerCaptor = ArgumentCaptor.forClass(
                OngoingCallListener.class);
        Mockito.verify(mOngoingCallController).addCallback(ongoingCallListenerCaptor.capture());
        OngoingCallListener listener = Objects.requireNonNull(ongoingCallListenerCaptor.getValue());

        when(mOngoingCallController.getHasOngoingCall()).thenReturn(true);
        listener.onOngoingCallStarted(/* animate= */ false);

        assertEquals(View.VISIBLE,
                mFragment.getView().findViewById(R.id.ongoing_call_chip).getVisibility());
        Mockito.verify(mNotificationAreaInner, atLeast(1)).setVisibility(eq(View.INVISIBLE));
    }

    @Test
    public void onOngoingCallEnded_notificationsDisplayedAndOngoingCallChipHidden() {
        mFragments.dispatchResume();
        processAllMessages();

        CollapsedStatusBarFragment fragment = (CollapsedStatusBarFragment) mFragment;
        fragment.initNotificationIconArea(mMockNotificiationAreaController);

        ArgumentCaptor<OngoingCallListener> ongoingCallListenerCaptor = ArgumentCaptor.forClass(
                OngoingCallListener.class);
        Mockito.verify(mOngoingCallController).addCallback(ongoingCallListenerCaptor.capture());
        OngoingCallListener listener = Objects.requireNonNull(ongoingCallListenerCaptor.getValue());

        when(mOngoingCallController.getHasOngoingCall()).thenReturn(false);
        listener.onOngoingCallEnded(/* animate= */ false);

        assertEquals(View.GONE,
                mFragment.getView().findViewById(R.id.ongoing_call_chip).getVisibility());
        Mockito.verify(mNotificationAreaInner, atLeast(1)).setVisibility(eq(View.VISIBLE));
    }

    @Override
    protected Fragment instantiate(Context context, String className, Bundle arguments) {
        mOngoingCallController = mock(OngoingCallController.class);
        return new CollapsedStatusBarFragment(mOngoingCallController);
    }
}
