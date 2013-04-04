package com.oasisfeng.android.base;

import android.app.Notification;

/** @author Oasis */
public class Notifier {

    public static class NotificationRecord {
        Notification mNotification;
    }

    public static NotificationRecord createNotification() {
        return new NotificationRecord();
    }
}
