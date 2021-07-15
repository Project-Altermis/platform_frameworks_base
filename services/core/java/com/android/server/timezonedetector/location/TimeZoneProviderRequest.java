/*
 * Copyright (C) 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.server.timezonedetector.location;

import android.annotation.NonNull;
import android.annotation.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * This class encapsulates a request to a provider.
 */
final class TimeZoneProviderRequest {

    @NonNull
    private static final TimeZoneProviderRequest STOP_UPDATES =
            new TimeZoneProviderRequest(
                    false /* sendUpdates */,
                    null /* initializationTimeout */);

    private final boolean mSendUpdates;

    @Nullable
    private final Duration mInitializationTimeout;

    private TimeZoneProviderRequest(
            boolean sendUpdates, @Nullable Duration initializationTimeout) {
        mSendUpdates = sendUpdates;
        mInitializationTimeout = initializationTimeout;
    }

    /** Creates a request to start updates with the specified timeout. */
    public static TimeZoneProviderRequest createStartUpdatesRequest(
            @NonNull Duration initializationTimeout) {
        return new TimeZoneProviderRequest(true, Objects.requireNonNull(initializationTimeout));
    }

    /** Creates a request to stop updates. */
    public static TimeZoneProviderRequest createStopUpdatesRequest() {
        return STOP_UPDATES;
    }

    /**
     * Returns {@code true} if the provider should send updates related to the device's current
     * time zone, {@code false} otherwise.
     */
    public boolean sendUpdates() {
        return mSendUpdates;
    }

    /**
     * Returns the maximum time that the provider is allowed to initialize before it is expected to
     * send an event of any sort. Only valid when {@link #sendUpdates()} is {@code true}. Failure to
     * send an event in this time (with some fuzz) may be interpreted as if the provider is
     * uncertain of the time zone, and/or it could lead to the provider being stopped.
     */
    @Nullable
    public Duration getInitializationTimeout() {
        return mInitializationTimeout;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TimeZoneProviderRequest
                that = (TimeZoneProviderRequest) o;
        return mSendUpdates == that.mSendUpdates
                && mInitializationTimeout == that.mInitializationTimeout;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSendUpdates, mInitializationTimeout);
    }

    @Override
    public String toString() {
        return "TimeZoneProviderRequest{"
                + "mSendUpdates=" + mSendUpdates
                + ", mInitializationTimeout=" + mInitializationTimeout
                + "}";
    }
}
