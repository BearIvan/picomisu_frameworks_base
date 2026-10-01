// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package android.view;

import android.content.ClipData;
import android.os.Handler;
import android.util.Slog;

import java.util.ArrayList;

/**
 * Detects a "shake" gesture while dragging in Smartisan PC mode: four alternating velocity
 * quadrants above a density scaled threshold (factory PICO OS 5.13.7
 * {@code android.view.DragGestureDetector}; nothing in the factory jars creates it).
 *
 * @hide
 */
public class DragGestureDetector {
    private static String TAG = "DragGestureDetector";
    private static boolean DEBUG = true;

    private static final int INVALIDATE_REGION_DIRECTION = -1;
    private static final int ONE_REGION_DIRECTION = 0x00;
    private static final int TWO_REGION_DIRECTION = 0x01;
    private static final int THREE_REGION_DIRECTION = 0x11;
    private static final int FOUR_REGION_DIRECTION = 0x10;
    private static final int RECORD_COUNT = 4;
    private static final int CYCLE = 2;

    private static ArrayList<VelocityDirection> mDirectionList = new ArrayList<>();

    private VelocityTracker mVelocityTracker;
    private final DisplayInfo mDisplayInfo = new DisplayInfo();
    private final int mTimeSlot = 50;
    private float mVelocityThreadHold_Mounse = 750f;
    private float mVelocityThreadHold_Touch = 300f;
    private float mVelocityThreadHold = 0f;
    private boolean mEnabled = false;
    private boolean mStop = false;
    private Handler mHandler = new Handler();
    private Runnable mPendingRunnable = new Runnable() {
        @Override
        public void run() {
            mStop = false;
        }
    };

    private static class VelocityDirection {
        int direction;
        long time;
        float mVelocityX;
        float mVelocityY;
        int mCount;

        public VelocityDirection(int direction, long time, float x, float y) {
            this.direction = direction;
            this.time = time;
            mVelocityX = x;
            mVelocityY = y;
            mCount = 1;
        }
    }

    public boolean detectDragMotion(MotionEvent event) {
        if (!mEnabled) {
            return false;
        }
        if (mStop) {
            return false;
        }
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
            mVelocityThreadHold = event.isFromSource(InputDevice.SOURCE_MOUSE)
                    ? mVelocityThreadHold_Mounse : mVelocityThreadHold_Touch;
            mVelocityThreadHold *= mDisplayInfo.logicalDensityDpi / 160;
            Slog.d(TAG, "detectDragMotion mVelocityThreadHold:" + mVelocityThreadHold);
        }
        long time = System.currentTimeMillis();
        mVelocityTracker.addMovement(event);
        mVelocityTracker.computeCurrentVelocity(1000);
        long consume = System.currentTimeMillis() - time;
        if (consume > 2) {
            Slog.d(TAG, "detectDragMotion consume time:" + consume);
        }
        float velocityX = mVelocityTracker.getXVelocity();
        float velocityY = mVelocityTracker.getYVelocity();
        int direction = computeVelocityDirection(velocityX, velocityY);
        Slog.d(TAG, "detectDragMotion velocityX:" + velocityX + " velocityY:" + velocityY
                + " region:" + Integer.toHexString(direction));
        if (direction == INVALIDATE_REGION_DIRECTION) {
            return false;
        }
        if (mDirectionList.size() == 0) {
            VelocityDirection vDirection = new VelocityDirection(direction,
                    System.currentTimeMillis(), velocityX, velocityY);
            mDirectionList.add(vDirection);
        } else {
            int size = mDirectionList.size();
            VelocityDirection tail = mDirectionList.get(size - 1);
            if (tail.direction != direction) {
                VelocityDirection vDirection = new VelocityDirection(direction,
                        System.currentTimeMillis(), velocityX, velocityY);
                size = mDirectionList.size();
                if (vDirection.time - tail.time < mTimeSlot) {
                    mDirectionList.remove(size - 1);
                    Slog.d(TAG, "detectDragMotion remove direction:"
                            + Integer.toHexString(tail.direction) + " at:" + size
                            + " smarler than timeslot");
                }
                size = mDirectionList.size();
                if (size == 0) {
                    return false;
                }
                tail = mDirectionList.get(size - 1);
                if (Math.abs(tail.mVelocityX / tail.mCount) < mVelocityThreadHold
                        && Math.abs(tail.mVelocityY / tail.mCount) < mVelocityThreadHold) {
                    mDirectionList.clear();
                    Slog.d(TAG, "detectDragMotion clear x:"
                            + Math.abs(tail.mVelocityX / tail.mCount)
                            + " y:" + Math.abs(tail.mVelocityY / tail.mCount)
                            + " direction:" + Integer.toHexString(tail.direction));
                    return false;
                }
                if (mDirectionList.size() == RECORD_COUNT) {
                    VelocityDirection front = mDirectionList.remove(0);
                    if (DEBUG) {
                        Slog.d(TAG, "detectDragMotion remove direction:"
                                + Integer.toHexString(front.direction) + " queue front");
                    }
                }
                mDirectionList.add(vDirection);
                if (DEBUG) {
                    Slog.d(TAG, "detectDragMotion add direction:"
                            + Integer.toHexString(vDirection.direction)
                            + " size:" + mDirectionList.size());
                }
                boolean trigger = false;
                if (size == RECORD_COUNT) {
                    trigger = true;
                    if (DEBUG) {
                        Slog.d(TAG, "detectDragMotion  direction list:{"
                                + Integer.toHexString(mDirectionList.get(0).direction) + ","
                                + Integer.toHexString(mDirectionList.get(1).direction) + ","
                                + Integer.toHexString(mDirectionList.get(2).direction) + ","
                                + Integer.toHexString(mDirectionList.get(3).direction));
                    }
                    for (int i = 0; i < size; i++) {
                        if ((mDirectionList.get(i).direction
                                ^ mDirectionList.get(++i).direction) != THREE_REGION_DIRECTION) {
                            trigger = false;
                        }
                    }
                }
                if (trigger) {
                    mDirectionList.clear();
                    mVelocityTracker.clear();
                    pendDetect();
                    if (DEBUG) {
                        Slog.d(TAG, "detectDragMotion trigger:" + trigger);
                    }
                }
                return trigger;
            } else {
                tail.mVelocityX += velocityX;
                tail.mVelocityY += velocityY;
                tail.mCount += 1;
            }
        }
        return false;
    }

    private void pendDetect() {
        mStop = true;
        mHandler.postDelayed(mPendingRunnable, 1000);
    }

    public void reset() {
        if (DEBUG) {
            Slog.d(TAG, "detectDragMotion reset");
        }
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
        mHandler.removeCallbacks(mPendingRunnable);
        mEnabled = false;
        mStop = false;
        mDirectionList.clear();
    }

    public int computeVelocityDirection(float velocityX, float velocityY) {
        if (velocityX > 1f && velocityY > 1f) {
            return ONE_REGION_DIRECTION;
        } else if (velocityX < -1f && velocityY > 1f) {
            return TWO_REGION_DIRECTION;
        } else if (velocityX < -1f && velocityY < -1f) {
            return THREE_REGION_DIRECTION;
        } else if (velocityX > 1f && velocityY < -1f) {
            return FOUR_REGION_DIRECTION;
        }
        return INVALIDATE_REGION_DIRECTION;
    }

    public void probeState(DisplayInfo info, boolean isPcMode, ClipData data) {
        final boolean shortcut = data != null && data.getDescription() != null
                && data.getDescription().getMimeTypeCount() == 1
                && data.getDescription().hasMimeType("shortcut/*");
        mEnabled = isPcMode && data != null && !shortcut;
        mDisplayInfo.copyFrom(info);
    }
}
