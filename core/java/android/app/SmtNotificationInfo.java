// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0
package android.app;

import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Smartisan push-notification description (factory PICO OS 5.13.7
 * android.app.SmtNotificationInfo).
 * @hide
 */
public class SmtNotificationInfo implements Parcelable {
    public static final String MESSAGE_CENTER_PUSH = "message_center_push";
    public static final String IS_HEADS_UP = "is_heads_up";

    private String pkg;
    private int notifyId;
    private String title;
    private String contentText;
    private boolean autoCancel;
    private PendingIntent contentIntent;
    private Bitmap customBitmap;
    private String customText;
    private boolean useSmtPushFlag;
    private boolean messageCenterPush;
    private boolean isHeadsUp;

    public SmtNotificationInfo() {
    }

    public SmtNotificationInfo(String pkg, int notifyId, String title, String contentText,
            boolean autoCancel, PendingIntent contentIntent, Bitmap customBitmap,
            String customText, boolean useSmtPushFlag) {
        this.pkg = pkg;
        this.notifyId = notifyId;
        this.title = title;
        this.contentText = contentText;
        this.autoCancel = autoCancel;
        this.contentIntent = contentIntent;
        this.customBitmap = customBitmap;
        this.customText = customText;
        this.useSmtPushFlag = useSmtPushFlag;
        if (contentIntent != null && contentIntent.getIntent() != null) {
            this.messageCenterPush =
                    contentIntent.getIntent().getBooleanExtra(MESSAGE_CENTER_PUSH, false);
            this.isHeadsUp = contentIntent.getIntent().getBooleanExtra(IS_HEADS_UP, false);
        }
    }

    protected SmtNotificationInfo(Parcel in) {
        pkg = in.readStringNoHelper();
        notifyId = in.readInt();
        title = in.readStringNoHelper();
        contentText = in.readStringNoHelper();
        autoCancel = in.readByte() != 0;
        contentIntent = in.readParcelable(PendingIntent.class.getClassLoader());
        customBitmap = in.readParcelable(Bitmap.class.getClassLoader());
        customText = in.readStringNoHelper();
        useSmtPushFlag = in.readByte() != 0;
        messageCenterPush = in.readByte() != 0;
        isHeadsUp = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeStringNoHelper(pkg);
        parcel.writeInt(notifyId);
        parcel.writeStringNoHelper(title);
        parcel.writeStringNoHelper(contentText);
        parcel.writeByte((byte) (autoCancel ? 1 : 0));
        parcel.writeParcelable(contentIntent, i);
        parcel.writeParcelable(customBitmap, i);
        parcel.writeStringNoHelper(customText);
        parcel.writeByte((byte) (useSmtPushFlag ? 1 : 0));
        parcel.writeByte((byte) (messageCenterPush ? 1 : 0));
        parcel.writeByte((byte) (isHeadsUp ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<SmtNotificationInfo> CREATOR =
            new Parcelable.Creator<SmtNotificationInfo>() {
        @Override
        public SmtNotificationInfo createFromParcel(Parcel in) {
            return new SmtNotificationInfo(in);
        }

        @Override
        public SmtNotificationInfo[] newArray(int size) {
            return new SmtNotificationInfo[size];
        }
    };

    public SmtNotificationInfo setPkg(String pkg) {
        this.pkg = pkg;
        return this;
    }

    public SmtNotificationInfo setNotifyId(int notifyId) {
        this.notifyId = notifyId;
        return this;
    }

    public SmtNotificationInfo setTitle(String title) {
        this.title = title;
        return this;
    }

    public SmtNotificationInfo setContentText(String contentText) {
        this.contentText = contentText;
        return this;
    }

    public SmtNotificationInfo setAutoCancel(boolean autoCancel) {
        this.autoCancel = autoCancel;
        return this;
    }

    public SmtNotificationInfo setContentIntent(PendingIntent contentIntent) {
        this.contentIntent = contentIntent;
        return this;
    }

    public SmtNotificationInfo setCustomBitmap(Bitmap customBitmap) {
        this.customBitmap = customBitmap;
        return this;
    }

    public SmtNotificationInfo setUseSmtPushFlag(boolean useSmtPushFlag) {
        this.useSmtPushFlag = useSmtPushFlag;
        return this;
    }

    public SmtNotificationInfo setCustomText(String customText) {
        this.customText = customText;
        return this;
    }

    public String getCustomText() {
        return customText;
    }

    public boolean isUseSmtPushFlag() {
        return useSmtPushFlag;
    }

    public boolean isMessageCenterPush() {
        return messageCenterPush;
    }

    public boolean isHeadsUp() {
        return isHeadsUp;
    }

    public String getPkg() {
        return pkg;
    }

    public int getNotifyId() {
        return notifyId;
    }

    public String getTitle() {
        return title;
    }

    public String getContentText() {
        return contentText;
    }

    public boolean isAutoCancel() {
        return autoCancel;
    }

    public PendingIntent getContentIntent() {
        return contentIntent;
    }

    public Bitmap getCustomBitmap() {
        return customBitmap;
    }
}
