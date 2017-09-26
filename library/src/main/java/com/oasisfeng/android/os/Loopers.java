package com.oasisfeng.android.os;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.MessageQueue;

/**
 * Utilities for {@link android.os.Looper}
 *
 * Created by Oasis on 2017/9/13.
 */
public class Loopers {

	public static void addIdleTask(final Handler handler, final Runnable task) {
		addIdleTask(handler.getLooper(), task);
	}

	public static void addIdleTask(final Runnable task) {
		addIdleTask(Looper.getMainLooper(), task);
	}

	public static void addIdleTask(final Looper looper, final Runnable task) {
		getQueue(looper).addIdleHandler(new MessageQueue.IdleHandler() {
			@Override public boolean queueIdle() {
				task.run();
				return false;
			}

			@Override public String toString() {
				return "IdleTask[" + task.toString() + "]";
			}
		});
	}

	@SuppressLint("NewApi")	// Looper.getQueue is hidden before Android M.
	private static MessageQueue getQueue(final Looper looper) {
		return looper.getQueue();
	}
}
