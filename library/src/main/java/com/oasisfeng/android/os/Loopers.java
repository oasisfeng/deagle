package com.oasisfeng.android.os;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.MessageQueue;

/**
 * Utilities for {@link android.os.Looper}
 *
 * Created by Oasis on 2017/9/13.
 */
public class Loopers {

	public static void addIdleTask(final Handler handler, final Runnable task) {
		getQueue(handler).addIdleHandler(new MessageQueue.IdleHandler() {
			@Override public boolean queueIdle() {
				task.run();
				return false;
			}
		});
	}

	@SuppressLint("NewApi")	// Looper.getQueue is hidden before Android M.
	private static MessageQueue getQueue(final Handler handler) {
		return handler.getLooper().getQueue();
	}
}
