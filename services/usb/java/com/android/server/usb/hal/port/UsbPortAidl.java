/*
 * Copyright (C) 2021 The Android Open Source Project
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
package com.android.server.usb.hal.port;

import static android.hardware.usb.UsbManager.USB_HAL_V2_0;
import static android.hardware.usb.UsbOperationInternal.USB_OPERATION_ERROR_INTERNAL;
import static android.hardware.usb.UsbOperationInternal.USB_OPERATION_SUCCESS;

import static com.android.server.usb.UsbPortManager.logAndPrint;
import static com.android.server.usb.UsbPortManager.logAndPrintException;

import android.annotation.Nullable;
import android.hardware.usb.ContaminantProtectionStatus;
import android.hardware.usb.IUsb;
import android.hardware.usb.IUsbOperationInternal;
import android.hardware.usb.UsbManager.UsbHalVersion;
import android.hardware.usb.UsbPort;
import android.hardware.usb.UsbPortStatus;
import android.hardware.usb.PortMode;
import android.hardware.usb.Status;
import android.hardware.usb.IUsbCallback;
import android.hardware.usb.PortRole;
import android.hardware.usb.PortStatus;
import android.os.ServiceManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.Slog;

import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.IndentingPrintWriter;
import com.android.server.usb.UsbPortManager;
import com.android.server.usb.hal.port.RawPortInfo;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Implements the methods to interact with AIDL USB HAL.
 */
public final class UsbPortAidl implements UsbPortHal {
    private static final String TAG = UsbPortAidl.class.getSimpleName();
    private static final String USB_AIDL_SERVICE =
            "android.hardware.usb.IUsb/default";
    private static final LongSparseArray<IUsbOperationInternal>
                sCallbacks = new LongSparseArray<>();
    // Proxy object for the usb hal daemon.
    @GuardedBy("mLock")
    private IUsb mProxy;
    private UsbPortManager mPortManager;
    public IndentingPrintWriter mPw;
    // Mutex for all mutable shared state.
    private final Object mLock = new Object();
    // Callback when the UsbPort status is changed by the kernel.
    private HALCallback mHALCallback;
    private IBinder mBinder;
    private boolean mSystemReady;
    private long mTransactionId;

    public @UsbHalVersion int getUsbHalVersion() throws RemoteException {
        synchronized (mLock) {
            if (mProxy == null) {
                throw new RemoteException("IUsb not initialized yet");
            }
        }
        logAndPrint(Log.INFO, null, "USB HAL AIDL version: USB_HAL_V2_0");
        return USB_HAL_V2_0;
    }

    @Override
    public void systemReady() {
        mSystemReady = true;
    }

    public void serviceDied() {
        logAndPrint(Log.ERROR, mPw, "Usb AIDL hal service died");
        synchronized (mLock) {
            mProxy = null;
        }
        connectToProxy(null);
    }

    private void connectToProxy(IndentingPrintWriter pw) {
        synchronized (mLock) {
            if (mProxy != null) {
                return;
            }

            try {
                mBinder = ServiceManager.waitForService(USB_AIDL_SERVICE);
                mProxy = IUsb.Stub.asInterface(mBinder);
                mBinder.linkToDeath(this::serviceDied, 0);
                mProxy.setCallback(mHALCallback);
                mProxy.queryPortStatus(++mTransactionId);
            } catch (NoSuchElementException e) {
                logAndPrintException(pw, "connectToProxy: usb hal service not found."
                        + " Did the service fail to start?", e);
            } catch (RemoteException e) {
                logAndPrintException(pw, "connectToProxy: usb hal service not responding", e);
            }
        }
    }

    static boolean isServicePresent(IndentingPrintWriter pw) {
        try {
            return ServiceManager.isDeclared(USB_AIDL_SERVICE);
        } catch (NoSuchElementException e) {
            logAndPrintException(pw, "connectToProxy: usb Aidl hal service not found.", e);
        }

        return false;
    }

    public UsbPortAidl(UsbPortManager portManager, IndentingPrintWriter pw) {
        mPortManager = Objects.requireNonNull(portManager);
        mPw = pw;
        mHALCallback = new HALCallback(null, mPortManager, this);
        connectToProxy(mPw);
    }

    @Override
    public void enableContaminantPresenceDetection(String portName, boolean enable,
            long operationID) {
        synchronized (mLock) {
            if (mProxy == null) {
                logAndPrint(Log.ERROR, mPw, "Proxy is null. Retry ! opID: "
                        + operationID);
                return;
            }

            try {
                // Oneway call into the hal. Use the castFrom method from HIDL.
                mProxy.enableContaminantPresenceDetection(portName, enable, operationID);
            } catch (RemoteException e) {
                logAndPrintException(mPw, "Failed to set contaminant detection. opID:"
                        + operationID, e);
            }
        }
    }

    @Override
    public void queryPortStatus(long operationID) {
        synchronized (mLock) {
            if (mProxy == null) {
                logAndPrint(Log.ERROR, mPw, "Proxy is null. Retry ! opID:"
                        + operationID);
                return;
            }

            try {
                mProxy.queryPortStatus(operationID);
            } catch (RemoteException e) {
                logAndPrintException(null, "ServiceStart: Failed to query port status. opID:"
                        + operationID, e);
            }
       }
    }

    @Override
    public void switchMode(String portId, @HalUsbPortMode int newMode, long operationID) {
        synchronized (mLock) {
            if (mProxy == null) {
                logAndPrint(Log.ERROR, mPw, "Proxy is null. Retry ! opID:"
                        + operationID);
                return;
            }

            PortRole newRole = new PortRole();
            newRole.setMode((byte)newMode);
            try {
                mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                logAndPrintException(mPw, "Failed to set the USB port mode: "
                        + "portId=" + portId
                        + ", newMode=" + UsbPort.modeToString(newMode)
                        + "opID:" + operationID, e);
            }
        }
    }

    @Override
    public void switchPowerRole(String portId, @HalUsbPowerRole int newPowerRole,
            long operationID) {
        synchronized (mLock) {
            if (mProxy == null) {
                logAndPrint(Log.ERROR, mPw, "Proxy is null. Retry ! opID:"
                        + operationID);
                return;
            }

            PortRole newRole = new PortRole();
            newRole.setPowerRole((byte)newPowerRole);
            try {
                mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                logAndPrintException(mPw, "Failed to set the USB power role: portId=" + portId
                        + ", newPowerRole=" + UsbPort.powerRoleToString(newPowerRole)
                        + "opID:" + operationID, e);
            }
        }
    }

    @Override
    public void switchDataRole(String portId, @HalUsbDataRole int newDataRole, long operationID) {
        synchronized (mLock) {
            if (mProxy == null) {
                logAndPrint(Log.ERROR, mPw, "Proxy is null. Retry ! opID:"
                        + operationID);
                return;
            }

            PortRole newRole = new PortRole();
            newRole.setDataRole((byte)newDataRole);
            try {
                mProxy.switchRole(portId, newRole, operationID);
            } catch (RemoteException e) {
                logAndPrintException(mPw, "Failed to set the USB data role: portId=" + portId
                        + ", newDataRole=" + UsbPort.dataRoleToString(newDataRole)
                        + "opID:" + operationID, e);
            }
        }
    }

    @Override
    public boolean enableUsbData(String portName, boolean enable, long operationID,
            IUsbOperationInternal callback) {
        Objects.requireNonNull(portName);
        Objects.requireNonNull(callback);
        long key = operationID;
        synchronized (mLock) {
            try {
                if (mProxy == null) {
                    logAndPrint(Log.ERROR, mPw,
                            "enableUsbData: Proxy is null. Retry !opID:"
                            + operationID);
                    callback.onOperationComplete(USB_OPERATION_ERROR_INTERNAL);
                    return false;
                }
                while (sCallbacks.get(key) != null) {
                    key = ThreadLocalRandom.current().nextInt();
                }
                if (key != operationID) {
                    logAndPrint(Log.INFO, mPw, "enableUsbData: operationID exists ! opID:"
                            + operationID + " key:" + key);
                }
                try {
                    sCallbacks.put(key, callback);
                    mProxy.enableUsbData(portName, enable, key);
                } catch (RemoteException e) {
                    logAndPrintException(mPw,
                            "enableUsbData: Failed to invoke enableUsbData: portID="
                            + portName + "opID:" + operationID, e);
                    callback.onOperationComplete(USB_OPERATION_ERROR_INTERNAL);
                    sCallbacks.remove(key);
                    return false;
                }
            } catch (RemoteException e) {
                logAndPrintException(mPw,
                        "enableUsbData: Failed to call onOperationComplete portID="
                        + portName + "opID:" + operationID, e);
                sCallbacks.remove(key);
                return false;
            }
            return true;
        }
    }

    private static class HALCallback extends IUsbCallback.Stub {
        public IndentingPrintWriter mPw;
        public UsbPortManager mPortManager;
        public UsbPortAidl mUsbPortAidl;

        HALCallback(IndentingPrintWriter pw, UsbPortManager portManager, UsbPortAidl usbPortAidl) {
            this.mPw = pw;
            this.mPortManager = portManager;
            this.mUsbPortAidl = usbPortAidl;
        }

        /**
         * Converts from AIDL defined mode constants to UsbPortStatus constants.
         * AIDL does not gracefully support bitfield when combined with enums.
         */
        private int toPortMode(byte aidlPortMode) {
            switch (aidlPortMode) {
                case PortMode.NONE:
                    return UsbPortStatus.MODE_NONE;
                case PortMode.UFP:
                    return UsbPortStatus.MODE_UFP;
                case PortMode.DFP:
                    return UsbPortStatus.MODE_DFP;
                case PortMode.DRP:
                    return UsbPortStatus.MODE_DUAL;
                case PortMode.AUDIO_ACCESSORY:
                    return UsbPortStatus.MODE_AUDIO_ACCESSORY;
                case PortMode.DEBUG_ACCESSORY:
                    return UsbPortStatus.MODE_DEBUG_ACCESSORY;
                default:
                    UsbPortManager.logAndPrint(Log.ERROR, mPw, "Unrecognized aidlPortMode:"
                            + aidlPortMode);
                    return UsbPortStatus.MODE_NONE;
            }
        }

        private int toSupportedModes(byte[] aidlPortModes) {
            int supportedModes = UsbPortStatus.MODE_NONE;

            for (byte aidlPortMode : aidlPortModes) {
                supportedModes |= toPortMode(aidlPortMode);
            }

            return supportedModes;
        }

        /**
         * Converts from AIDL defined contaminant protection constants to UsbPortStatus constants.
         * AIDL does not gracefully support bitfield when combined with enums.
         * Common to both ContaminantProtectionMode and ContaminantProtectionStatus.
         */
        private int toContaminantProtectionStatus(byte aidlContaminantProtection) {
            switch (aidlContaminantProtection) {
                case ContaminantProtectionStatus.NONE:
                    return UsbPortStatus.CONTAMINANT_PROTECTION_NONE;
                case ContaminantProtectionStatus.FORCE_SINK:
                    return UsbPortStatus.CONTAMINANT_PROTECTION_SINK;
                case ContaminantProtectionStatus.FORCE_SOURCE:
                    return UsbPortStatus.CONTAMINANT_PROTECTION_SOURCE;
                case ContaminantProtectionStatus.FORCE_DISABLE:
                    return UsbPortStatus.CONTAMINANT_PROTECTION_FORCE_DISABLE;
                case ContaminantProtectionStatus.DISABLED:
                    return UsbPortStatus.CONTAMINANT_PROTECTION_DISABLED;
                default:
                    UsbPortManager.logAndPrint(Log.ERROR, mPw,
                            "Unrecognized aidlContaminantProtection:"
                            + aidlContaminantProtection);
                    return UsbPortStatus.CONTAMINANT_PROTECTION_NONE;
            }
        }

        private int toSupportedContaminantProtectionModes(byte[] aidlModes) {
            int supportedContaminantProtectionModes = UsbPortStatus.CONTAMINANT_PROTECTION_NONE;

            for (byte aidlMode : aidlModes) {
                supportedContaminantProtectionModes |= toContaminantProtectionStatus(aidlMode);
            }

            return supportedContaminantProtectionModes;
        }

        @Override
        public void notifyPortStatusChange(
               android.hardware.usb.PortStatus[] currentPortStatus, int retval) {
            if (!mUsbPortAidl.mSystemReady) {
                return;
            }

            if (retval != Status.SUCCESS) {
                UsbPortManager.logAndPrint(Log.ERROR, mPw, "port status enquiry failed");
                return;
            }

            ArrayList<RawPortInfo> newPortInfo = new ArrayList<>();

            int numStatus = currentPortStatus.length;
            for (int i = 0; i < numStatus; i++) {
                PortStatus current = currentPortStatus[i];
                RawPortInfo temp = new RawPortInfo(current.portName,
                        toSupportedModes(current.supportedModes),
                        toSupportedContaminantProtectionModes(current
                                .supportedContaminantProtectionModes),
                        toPortMode(current.currentMode),
                        current.canChangeMode,
                        current.currentPowerRole,
                        current.canChangePowerRole,
                        current.currentDataRole,
                        current.canChangeDataRole,
                        current.supportsEnableContaminantPresenceProtection,
                        toContaminantProtectionStatus(current.contaminantProtectionStatus),
                        current.supportsEnableContaminantPresenceDetection,
                        current.contaminantDetectionStatus,
                        current.usbDataEnabled);
                newPortInfo.add(temp);
                UsbPortManager.logAndPrint(Log.INFO, mPw, "ClientCallback AIDL V1: "
                        + current.portName);
            }
            mPortManager.updatePorts(newPortInfo);
        }

        @Override
        public void notifyRoleSwitchStatus(String portName, PortRole role, int retval,
                long operationID) {
            if (retval == Status.SUCCESS) {
                UsbPortManager.logAndPrint(Log.INFO, mPw, portName
                        + " role switch successful. opID:"
                        + operationID);
            } else {
                UsbPortManager.logAndPrint(Log.ERROR, mPw, portName + " role switch failed. err:"
                        + retval
                        + "opID:" + operationID);
            }
        }

        @Override
        public void notifyQueryPortStatus(String portName, int retval, long operationID) {
            if (retval == Status.SUCCESS) {
                UsbPortManager.logAndPrint(Log.INFO, mPw, portName + ": opID:"
                        + operationID + " successful");
            } else {
                UsbPortManager.logAndPrint(Log.ERROR, mPw, portName + ": opID:"
                        + operationID + " failed. err:" + retval);
            }
        }

        @Override
        public void notifyEnableUsbDataStatus(String portName, boolean enable, int retval,
                long operationID) {
            if (retval == Status.SUCCESS) {
                UsbPortManager.logAndPrint(Log.INFO, mPw, "notifyEnableUsbDataStatus:"
                        + portName + ": opID:"
                        + operationID + " enable:" + enable);
            } else {
                UsbPortManager.logAndPrint(Log.ERROR, mPw, portName
                        + "notifyEnableUsbDataStatus: opID:"
                        + operationID + " failed. err:" + retval);
            }
            try {
                sCallbacks.get(operationID).onOperationComplete(retval == Status.SUCCESS
                        ? USB_OPERATION_SUCCESS
                        : USB_OPERATION_ERROR_INTERNAL);
            } catch (RemoteException e) {
                logAndPrintException(mPw,
                        "notifyEnableUsbDataStatus: Failed to call onOperationComplete",
                        e);
            }
        }

        @Override
        public void notifyContaminantEnabledStatus(String portName, boolean enable, int retval,
                long operationID) {
            if (retval == Status.SUCCESS) {
                UsbPortManager.logAndPrint(Log.INFO, mPw, "notifyContaminantEnabledStatus:"
                        + portName + ": opID:"
                        + operationID + " enable:" + enable);
            } else {
                UsbPortManager.logAndPrint(Log.ERROR, mPw, portName
                        + "notifyContaminantEnabledStatus: opID:"
                        + operationID + " failed. err:" + retval);
            }
        }

        @Override
        public String getInterfaceHash() {
            return IUsbCallback.HASH;
        }

        @Override
        public int getInterfaceVersion() {
            return IUsbCallback.VERSION;
        }
    }
}
