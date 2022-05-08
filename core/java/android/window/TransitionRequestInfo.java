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

package android.window;

import android.annotation.Nullable;
import android.app.ActivityManager;
import android.os.Parcelable;
import android.view.WindowManager;

import com.android.internal.util.DataClass;

/**
 * Used to communicate information about what is changing during a transition to a TransitionPlayer.
 * @hide
 */
@DataClass(genToString = true, genSetters = true, genAidl = true)
public final class TransitionRequestInfo implements Parcelable {

    /** The type of the transition being requested. */
    private final @WindowManager.TransitionType int mType;

    /**
     * If non-null, If non-null, the task containing the activity whose lifecycle change (start or
     * finish) has caused this transition to occur.
     */
    private @Nullable ActivityManager.RunningTaskInfo mTriggerTask;

    /** If non-null, a remote-transition associated with the source of this transition. */
    private @Nullable RemoteTransition mRemoteTransition;



    // Code below generated by codegen v1.0.23.
    //
    // DO NOT MODIFY!
    // CHECKSTYLE:OFF Generated code
    //
    // To regenerate run:
    // $ codegen $ANDROID_BUILD_TOP/frameworks/base/core/java/android/window/TransitionRequestInfo.java
    //
    // To exclude the generated code from IntelliJ auto-formatting enable (one-time):
    //   Settings > Editor > Code Style > Formatter Control
    //@formatter:off


    /**
     * Creates a new TransitionRequestInfo.
     *
     * @param type
     *   The type of the transition being requested.
     * @param triggerTask
     *   If non-null, If non-null, the task containing the activity whose lifecycle change (start or
     *   finish) has caused this transition to occur.
     * @param remoteTransition
     *   If non-null, a remote-transition associated with the source of this transition.
     */
    @DataClass.Generated.Member
    public TransitionRequestInfo(
            @WindowManager.TransitionType int type,
            @Nullable ActivityManager.RunningTaskInfo triggerTask,
            @Nullable RemoteTransition remoteTransition) {
        this.mType = type;
        com.android.internal.util.AnnotationValidations.validate(
                WindowManager.TransitionType.class, null, mType);
        this.mTriggerTask = triggerTask;
        this.mRemoteTransition = remoteTransition;

        // onConstructed(); // You can define this method to get a callback
    }

    /**
     * The type of the transition being requested.
     */
    @DataClass.Generated.Member
    public @WindowManager.TransitionType int getType() {
        return mType;
    }

    /**
     * If non-null, If non-null, the task containing the activity whose lifecycle change (start or
     * finish) has caused this transition to occur.
     */
    @DataClass.Generated.Member
    public @Nullable ActivityManager.RunningTaskInfo getTriggerTask() {
        return mTriggerTask;
    }

    /**
     * If non-null, a remote-transition associated with the source of this transition.
     */
    @DataClass.Generated.Member
    public @Nullable RemoteTransition getRemoteTransition() {
        return mRemoteTransition;
    }

    /**
     * If non-null, If non-null, the task containing the activity whose lifecycle change (start or
     * finish) has caused this transition to occur.
     */
    @DataClass.Generated.Member
    public @android.annotation.NonNull TransitionRequestInfo setTriggerTask(@android.annotation.NonNull ActivityManager.RunningTaskInfo value) {
        mTriggerTask = value;
        return this;
    }

    /**
     * If non-null, a remote-transition associated with the source of this transition.
     */
    @DataClass.Generated.Member
    public @android.annotation.NonNull TransitionRequestInfo setRemoteTransition(@android.annotation.NonNull RemoteTransition value) {
        mRemoteTransition = value;
        return this;
    }

    @Override
    @DataClass.Generated.Member
    public String toString() {
        // You can override field toString logic by defining methods like:
        // String fieldNameToString() { ... }

        return "TransitionRequestInfo { " +
                "type = " + mType + ", " +
                "triggerTask = " + mTriggerTask + ", " +
                "remoteTransition = " + mRemoteTransition +
        " }";
    }

    @Override
    @DataClass.Generated.Member
    public void writeToParcel(@android.annotation.NonNull android.os.Parcel dest, int flags) {
        // You can override field parcelling by defining methods like:
        // void parcelFieldName(Parcel dest, int flags) { ... }

        byte flg = 0;
        if (mTriggerTask != null) flg |= 0x2;
        if (mRemoteTransition != null) flg |= 0x4;
        dest.writeByte(flg);
        dest.writeInt(mType);
        if (mTriggerTask != null) dest.writeTypedObject(mTriggerTask, flags);
        if (mRemoteTransition != null) dest.writeTypedObject(mRemoteTransition, flags);
    }

    @Override
    @DataClass.Generated.Member
    public int describeContents() { return 0; }

    /** @hide */
    @SuppressWarnings({"unchecked", "RedundantCast"})
    @DataClass.Generated.Member
    /* package-private */ TransitionRequestInfo(@android.annotation.NonNull android.os.Parcel in) {
        // You can override field unparcelling by defining methods like:
        // static FieldType unparcelFieldName(Parcel in) { ... }

        byte flg = in.readByte();
        int type = in.readInt();
        ActivityManager.RunningTaskInfo triggerTask = (flg & 0x2) == 0 ? null : (ActivityManager.RunningTaskInfo) in.readTypedObject(ActivityManager.RunningTaskInfo.CREATOR);
        RemoteTransition remoteTransition = (flg & 0x4) == 0 ? null : (RemoteTransition) in.readTypedObject(RemoteTransition.CREATOR);

        this.mType = type;
        com.android.internal.util.AnnotationValidations.validate(
                WindowManager.TransitionType.class, null, mType);
        this.mTriggerTask = triggerTask;
        this.mRemoteTransition = remoteTransition;

        // onConstructed(); // You can define this method to get a callback
    }

    @DataClass.Generated.Member
    public static final @android.annotation.NonNull Parcelable.Creator<TransitionRequestInfo> CREATOR
            = new Parcelable.Creator<TransitionRequestInfo>() {
        @Override
        public TransitionRequestInfo[] newArray(int size) {
            return new TransitionRequestInfo[size];
        }

        @Override
        public TransitionRequestInfo createFromParcel(@android.annotation.NonNull android.os.Parcel in) {
            return new TransitionRequestInfo(in);
        }
    };

    @DataClass.Generated(
            time = 1629321632222L,
            codegenVersion = "1.0.23",
            sourceFile = "frameworks/base/core/java/android/window/TransitionRequestInfo.java",
            inputSignatures = "private final @android.view.WindowManager.TransitionType int mType\nprivate @android.annotation.Nullable android.app.ActivityManager.RunningTaskInfo mTriggerTask\nprivate @android.annotation.Nullable android.window.RemoteTransition mRemoteTransition\nclass TransitionRequestInfo extends java.lang.Object implements [android.os.Parcelable]\n@com.android.internal.util.DataClass(genToString=true, genSetters=true, genAidl=true)")
    @Deprecated
    private void __metadata() {}


    //@formatter:on
    // End of generated code

}
