package com.oasisfeng.hack;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Oasis on 2019-10-14.
 */
public class TestClass {

	private static int opToDefaultMode(int op) { return 2; }
	private int basicMethod(int a, String b) { return 1; }
	private List<InnerClass> complexMethod(int a, String b, int[] c) { return Collections.emptyList(); }

	private static class InnerClass implements Parcelable {
		private final String mPackageName;
		private final int mUid;
		private final List<OpEntry> mEntries;

		public InnerClass(String packageName, int uid, List<OpEntry> entries) {
			mPackageName = packageName;
			mUid = uid;
			mEntries = entries;
		}

		public String getPackageName() {
			return mPackageName;
		}

		public int getUid() {
			return mUid;
		}

		public List<OpEntry> getOps() {
			return mEntries;
		}

		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			dest.writeString(mPackageName);
			dest.writeInt(mUid);
			dest.writeInt(mEntries.size());
			for (int i=0; i<mEntries.size(); i++) {
				mEntries.get(i).writeToParcel(dest, flags);
			}
		}

		InnerClass(Parcel source) {
			mPackageName = source.readString();
			mUid = source.readInt();
			mEntries = new ArrayList<OpEntry>();
			final int N = source.readInt();
			for (int i=0; i<N; i++) {
				mEntries.add(OpEntry.CREATOR.createFromParcel(source));
			}
		}

		public static final Creator<InnerClass> CREATOR = new Creator<InnerClass>() {
			@Override public InnerClass createFromParcel(Parcel source) {
				return new InnerClass(source);
			}

			@Override public InnerClass[] newArray(int size) {
				return new InnerClass[size];
			}
		};
	}

	public static class OpEntry implements Parcelable {
		private final int mOp;
		private final long mMode;
		private final boolean mRunning;
		private final String mProxyPackageName;

		public OpEntry(int op, long mode, long time, long rejectTime, int duration, int proxyUid, String proxyPackage) {
			mOp = op; mMode = mode; mRunning = true; mProxyPackageName = proxyPackage;
		}

		public int getOp() {
			return mOp;
		}

		public long getMode() {
			return mMode;
		}

		public boolean isRunning() {
			return mRunning;
		}

		public String getProxyPackageName() {
			return mProxyPackageName;
		}

		@Override public int describeContents() {
			return 0;
		}
		@Override public void writeToParcel(Parcel dest, int flags) {}

		OpEntry(Parcel source) {
			mOp = source.readInt(); mMode = source.readInt(); mRunning = true; mProxyPackageName = source.readString();
		}

		public static final Creator<OpEntry> CREATOR = new Creator<OpEntry>() {
			@Override public OpEntry createFromParcel(Parcel source) { return new OpEntry(source); }
			@Override public OpEntry[] newArray(int size) { return new OpEntry[size]; }
		};
	}
}