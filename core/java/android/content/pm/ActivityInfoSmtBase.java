// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.content.pm;

import android.os.Parcel;

/**
 * Smartisan/PICO extension state attached to every {@link ActivityInfo} (reachable through
 * {@code ActivityInfo.getSmtEx()}). Reconstructed from the PICO OS 5.13.7 factory framework;
 * it is parcelled together with the owning {@link ActivityInfo}.
 *
 * @hide
 */
public class ActivityInfoSmtBase {
    public static final int ADD_INTENT_TO_TRACK = 1;

    public static final int SCREEN_ORIENTATION_APP_PORTRAIT = 15;
    public static final int SCREEN_ORIENTATION_APP_LANDSCAPE = 16;
    public static final int SCREEN_ORIENTATION_APP_REVERSE_PORTRAIT = 17;
    public static final int SCREEN_ORIENTATION_APP_REVERSE_LANDSCAPE = 18;

    public int chosenPriority;

    /** Smartisan activity flags declared in the manifest (see PackageParserSmtBase). */
    public int smXMLFlags;

    public int userOrientation = -1;

    public int forceDisplayFlags;

    public int autoDisplayFlags = 8;

    public boolean taskInVisible;

    public int smFlag = -1;

    public void readFromParcel(Parcel in) {
        chosenPriority = in.readInt();
        smXMLFlags = in.readInt();
        userOrientation = in.readInt();
        forceDisplayFlags = in.readInt();
        autoDisplayFlags = in.readInt();
        taskInVisible = in.readBoolean();
    }

    public void writeToParcel(Parcel dest, int parcelableFlags) {
        dest.writeInt(chosenPriority);
        dest.writeInt(smXMLFlags);
        dest.writeInt(userOrientation);
        dest.writeInt(forceDisplayFlags);
        dest.writeInt(autoDisplayFlags);
        dest.writeBoolean(taskInVisible);
    }

    public void copyFrom(ActivityInfo orig) {
        chosenPriority = orig.getSmtEx().chosenPriority;
        smXMLFlags = orig.getSmtEx().smXMLFlags;
        userOrientation = orig.getSmtEx().userOrientation;
        forceDisplayFlags = orig.getSmtEx().forceDisplayFlags;
        autoDisplayFlags = orig.getSmtEx().autoDisplayFlags;
    }

    /**
     * Copies the parcelled state of {@code activityInfoSmtEx} into this object.
     */
    public void clone(ActivityInfoSmtBase activityInfoSmtEx) {
        chosenPriority = activityInfoSmtEx.chosenPriority;
        smXMLFlags = activityInfoSmtEx.smXMLFlags;
        userOrientation = activityInfoSmtEx.userOrientation;
        forceDisplayFlags = activityInfoSmtEx.forceDisplayFlags;
        autoDisplayFlags = activityInfoSmtEx.autoDisplayFlags;
    }

    /**
     * Like {@code ActivityInfo.screenOrientationToString}, including the Smartisan
     * app orientations.
     */
    public static String orientationToString(int orientation) {
        switch (orientation) {
            case ActivityInfo.SCREEN_ORIENTATION_BEHIND:
                return "SCREEN_ORIENTATION_BEHIND";
            case ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR:
                return "SCREEN_ORIENTATION_FULL_SENSOR";
            case ActivityInfo.SCREEN_ORIENTATION_FULL_USER:
                return "SCREEN_ORIENTATION_FULL_USER";
            case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                return "SCREEN_ORIENTATION_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_LOCKED:
                return "SCREEN_ORIENTATION_LOCKED";
            case ActivityInfo.SCREEN_ORIENTATION_NOSENSOR:
                return "SCREEN_ORIENTATION_NOSENSOR";
            case ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:
                return "SCREEN_ORIENTATION_PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                return "SCREEN_ORIENTATION_REVERSE_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT:
                return "SCREEN_ORIENTATION_REVERSE_PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR:
                return "SCREEN_ORIENTATION_SENSOR";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:
                return "SCREEN_ORIENTATION_SENSOR_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:
                return "SCREEN_ORIENTATION_SENSOR_PORTRAIT";
            case ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED:
                return "SCREEN_ORIENTATION_UNSPECIFIED";
            case ActivityInfo.SCREEN_ORIENTATION_USER:
                return "SCREEN_ORIENTATION_USER";
            case ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE:
                return "SCREEN_ORIENTATION_USER_LANDSCAPE";
            case ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT:
                return "SCREEN_ORIENTATION_USER_PORTRAIT";
            case SCREEN_ORIENTATION_APP_PORTRAIT:
                return "SCREEN_ORIENTATION_APP_PORTRAIT";
            case SCREEN_ORIENTATION_APP_REVERSE_PORTRAIT:
                return "SCREEN_ORIENTATION_APP_REVERSE_PORTRAIT";
            case SCREEN_ORIENTATION_APP_LANDSCAPE:
                return "SCREEN_ORIENTATION_APP_LANDSCAPE";
            case SCREEN_ORIENTATION_APP_REVERSE_LANDSCAPE:
                return "SCREEN_ORIENTATION_APP_REVERSE_LANDSCAPE";
            default:
                return "SCREEN_ORIENTATION_UNKNOWN(" + Integer.toString(orientation) + ")";
        }
    }
}
