// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.app;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.Set;

/**
 * Scene information exchanged through {@link ISceneInfoManager} and {@link IDataListener}.
 * Reconstructed from the PICO OS 5.13.7 factory framework.
 *
 * @hide
 */
public class SceneData implements Parcelable, Cloneable {
    private static final String TAG = "SceneData";
    public static final int TYPE_INT32 = 1;
    public static final int TYPE_INT64 = 2;
    public static final int TYPE_FLOAT = 3;

    private String mReportSource;
    private String mStringArg;
    private int mIntArg1 = -1;
    private int mIntArg2 = -1;
    private Bundle mExtras;

    public SceneData() {
    }

    protected SceneData(Parcel in) {
        readFromParcel(in);
    }

    public SceneData(SceneData o) {
        this.mReportSource = o.mReportSource;
        this.mStringArg = o.mStringArg;
        this.mIntArg1 = o.mIntArg1;
        this.mIntArg2 = o.mIntArg2;
        if (o.mExtras != null) {
            this.mExtras = new Bundle(o.mExtras);
        }
    }

    public static final Parcelable.Creator<SceneData> CREATOR =
            new Parcelable.Creator<SceneData>() {
        @Override
        public SceneData createFromParcel(Parcel in) {
            return new SceneData(in);
        }

        @Override
        public SceneData[] newArray(int size) {
            return new SceneData[size];
        }
    };

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeString(mReportSource);
        out.writeString(mStringArg);
        out.writeInt(mIntArg1);
        out.writeInt(mIntArg2);
        out.writeBundle(mExtras);
    }

    public void readFromParcel(Parcel in) {
        mReportSource = in.readString();
        mStringArg = in.readString();
        mIntArg1 = in.readInt();
        mIntArg2 = in.readInt();
        mExtras = in.readBundle();
    }

    @Override
    public Object clone() {
        return new SceneData(this);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SceneData {");
        if (mReportSource != null) {
            sb.append(" mReportSource:" + mReportSource);
        }
        if (mStringArg != null) {
            sb.append(" mStringArg:" + mStringArg);
        }
        if (mIntArg1 != -1) {
            sb.append(" mIntArg1:" + mIntArg1);
        }
        if (mIntArg2 != -1) {
            sb.append(" mIntArg2:" + mIntArg2);
        }
        if (mExtras != null) {
            sb.append(" mExtras:");
            Set<String> keySet = mExtras.keySet();
            for (String key : keySet) {
                Object value = mExtras.get(key);
                sb.append(" [" + key + ":" + value + "]");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public SceneData setReportSource(String source) {
        mReportSource = source;
        return this;
    }

    public String getReportSource() {
        return mReportSource;
    }

    public SceneData setStringArg(String arg) {
        mStringArg = arg;
        return this;
    }

    public String getStringArg() {
        return mStringArg;
    }

    public SceneData setIntArg1(int arg1) {
        mIntArg1 = arg1;
        return this;
    }

    public int getIntArg1() {
        return mIntArg1;
    }

    public SceneData setIntArg2(int arg2) {
        mIntArg2 = arg2;
        return this;
    }

    public int getIntArg2() {
        return mIntArg2;
    }

    public SceneData replaceExtras(Bundle extras) {
        mExtras = extras != null ? new Bundle(extras) : null;
        return this;
    }

    public SceneData putExtras(Bundle extras) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putAll(extras);
        return this;
    }

    public Bundle getExtras() {
        return (mExtras != null)
                ? new Bundle(mExtras)
                : null;
    }

    public SceneData putExtra(String name, boolean value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBoolean(name, value);
        return this;
    }

    public boolean getBooleanExtra(String name, boolean defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getBoolean(name, defaultValue);
    }

    public SceneData putExtra(String name, boolean[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBooleanArray(name, value);
        return this;
    }

    public boolean[] getBooleanArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getBooleanArray(name);
    }

    public SceneData putExtra(String name, Bundle value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putBundle(name, value);
        return this;
    }

    public Object getExtra(String name) {
        return getExtra(name, null);
    }

    public Object getExtra(String name, Object defaultValue) {
        Object result = defaultValue;
        if (mExtras != null) {
            Object result2 = mExtras.get(name);
            if (result2 != null) {
                result = result2;
            }
        }
        return result;
    }

    public SceneData putExtra(String name, char value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putChar(name, value);
        return this;
    }

    public char getCharExtra(String name, char defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getChar(name, defaultValue);
    }

    public SceneData putExtra(String name, char[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putCharArray(name, value);
        return this;
    }

    public char[] getCharArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getCharArray(name);
    }

    public SceneData putExtra(String name, double value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDouble(name, value);
        return this;
    }

    public double getDoubleExtra(String name, double defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getDouble(name, defaultValue);
    }

    public SceneData putExtra(String name, double[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putDoubleArray(name, value);
        return this;
    }

    public double[] getDoubleArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getDoubleArray(name);
    }

    public SceneData putExtra(String name, float value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloat(name, value);
        return this;
    }

    public float getFloatExtra(String name, float defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getFloat(name, defaultValue);
    }

    public SceneData putExtra(String name, float[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putFloatArray(name, value);
        return this;
    }

    public float[] getFloatArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getFloatArray(name);
    }

    public SceneData putExtra(String name, int value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putInt(name, value);
        return this;
    }

    public int getIntExtra(String name, int defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getInt(name, defaultValue);
    }

    public SceneData putExtra(String name, int[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putIntArray(name, value);
        return this;
    }

    public int[] getIntArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getIntArray(name);
    }

    public SceneData putExtra(String name, long value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLong(name, value);
        return this;
    }

    public long getLongExtra(String name, long defaultValue) {
        return mExtras == null ? defaultValue :
                mExtras.getLong(name, defaultValue);
    }

    public SceneData putExtra(String name, long[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putLongArray(name, value);
        return this;
    }

    public long[] getLongArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getLongArray(name);
    }

    public SceneData putExtra(String name, String value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putString(name, value);
        return this;
    }

    public String getStringExtra(String name) {
        return mExtras == null ? null : mExtras.getString(name);
    }

    public SceneData putExtra(String name, String[] value) {
        if (mExtras == null) {
            mExtras = new Bundle();
        }
        mExtras.putStringArray(name, value);
        return this;
    }

    public String[] getStringArrayExtra(String name) {
        return mExtras == null ? null : mExtras.getStringArray(name);
    }
}
