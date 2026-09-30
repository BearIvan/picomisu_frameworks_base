// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.pico.ns;

/**
 * Binder transaction codes of the PICO native shell ("NS") window service
 * (descriptor "com.bytedance.IRemoteCallback").
 * @hide
 */
public interface NsTransactionInterface {
    /** Client to NS server requests. */
    int CODE_CLIENT_BASE = 100000;
    int CODE_CLIENT_GET_NS_SERVER = CODE_CLIENT_BASE;
    int CODE_CLIENT_CREATE = CODE_CLIENT_BASE + 1;
    int CODE_CLIENT_RESIZE = CODE_CLIENT_BASE + 2;
    int CODE_CLIENT_VISIBLE = CODE_CLIENT_BASE + 3;
    int CODE_CLIENT_DESTROY = CODE_CLIENT_BASE + 4;
    int CODE_CLIENT_REGISTER_RUNNING_APP_CALLBACK = CODE_CLIENT_BASE + 6;
    int CODE_CLIENT_UNREGISTER_RUNNING_APP_CALLBACK = CODE_CLIENT_BASE + 7;
    int CODE_CLIENT_EXEC_COMMAND = CODE_CLIENT_BASE + 8;
    int CODE_CLIENT_UPDATE_TOUCH_REGION = CODE_CLIENT_BASE + 9;
    int CODE_CLIENT_ACQUIRE_SURFACE = CODE_CLIENT_BASE + 10;
    int CODE_CLIENT_RELEASE_SURFACE = CODE_CLIENT_BASE + 11;

    /** NS server to client callbacks. */
    int CODE_NS_BASE = 200000;
    int CODE_NS_SURFACE_ON_CREATED = CODE_NS_BASE + 2;
    int CODE_NS_DISPATCH_EVENT = CODE_NS_BASE + 3;
    int CODE_NS_SURFACE_ON_DESTROYED = CODE_NS_BASE + 4;
    int CODE_NS_ON_VISIBLE_CHANGED = CODE_NS_BASE + 5;
    int CODE_NS_ON_FLOATING_WINDOW_READY = CODE_NS_BASE + 6;

    /** Running application list callbacks. */
    int CODE_RUNNING_APP_BASE = 300000;
    int CODE_DISPATCH_RUNNING_APP_LIST = CODE_RUNNING_APP_BASE + 1;

    /** System transactions. */
    int CODE_SYS_BASE = 400000;
    int CODE_CLIENT_INPUT_METHOD_STATUS_CHANGED = CODE_SYS_BASE + 1;
    int CODE_SYS_TRANSACTION = CODE_SYS_BASE + 2;
}
