// Copyright 2026 Picomisu contributors
// SPDX-License-Identifier: Apache-2.0

package com.android.server.audio;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.media.AudioPlaybackConfiguration;
import android.media.IPlaybackConfigDispatcher;
import android.media.IPlayer;
import android.media.PlayerBase;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * PICO background playback control, as in the PICO OS 5.13.7 factory services: when
 * Settings.System "disable_background_playback" is set, players of packages that are not
 * visible (or all players while see-through is shown) are muted through
 * {@link IPlayer#setExtVolume}, unless the app holds com.picovr.permission.BACKGROUND_PLAYBACK.
 */
public class ExtAudioServiceImpl implements IExtAudioService {
    private static final int MSG_UPDATE_ALLOW_BACKGROUND_PLAYBACK = 2;
    private static final int MSG_UPDATE_PLAYERS_VOLUME = 1;
    private static final int SENDMSG_NOOP = 1;
    private static final int SENDMSG_QUEUE = 2;
    private static final int SENDMSG_REPLACE = 0;
    private static final String TAG = "ExtAudioServiceImpl";
    private static final String sBackgroundPlaybackSettingKey = "disable_background_playback";

    private InternalContentObserver mContentObserver;
    private Context mContext;
    private ForegroundStateProvider mForegroundProvider;
    private InternalHandler mHandler;
    private HandlerThread mHandlerThread;
    private PlaybackConfigDispatcher mPlaybackDispatcher;
    private final Object mLock = new Object();
    private boolean mAllowBackgroundPlayback = true;
    private ArrayList<String> mForegroundPkgs = new ArrayList<>();
    private boolean mSeeThroughShowing = false;
    private SparseArray<PlayerInfo> mPlayers = new SparseArray<>();

    public ExtAudioServiceImpl(Context context) {
        mContext = context;
    }

    @Override
    public void initialize() {
        mAllowBackgroundPlayback = Settings.System.getInt(mContext.getContentResolver(),
                sBackgroundPlaybackSettingKey, 0) == 0;
        if (!mAllowBackgroundPlayback) {
            setupHandlerThread();
        }
        mForegroundProvider = new ForegroundStateProvider(new InternalForegroundStateListener());
        mContentObserver = new InternalContentObserver();
        mContext.getContentResolver().registerContentObserver(
                Settings.System.getUriFor(sBackgroundPlaybackSettingKey), false,
                mContentObserver);
    }

    @Override
    public void trackPlayer(int piid, int uid, int pid, PlayerBase.PlayerIdCard pic) {
        boolean hasPermission = mContext.checkPermission(
                "com.picovr.permission.BACKGROUND_PLAYBACK", pid, uid)
                == PackageManager.PERMISSION_GRANTED;
        synchronized (mLock) {
            PlayerInfo info = new PlayerInfo();
            info.piid = piid;
            info.uid = uid;
            info.pid = pid;
            info.player = pic.mIPlayer;
            info.hasBackgroundPlayPermission = hasPermission;
            for (int i = 0; i < mPlayers.size(); i++) {
                PlayerInfo tempInfo = mPlayers.valueAt(i);
                if (tempInfo.pid == pid) {
                    info.packageName = tempInfo.packageName;
                    break;
                }
            }
            if (info.packageName == null) {
                info.packageName = AudioPackageManager.getPackageName(uid, pid);
            }
            mPlayers.put(piid, info);
            mutePlayerIfNeedLock(info);
        }
    }

    @Override
    public void setPlaybackActivityMonitor(PlaybackActivityMonitor monitor) {
        if (monitor == null) {
            return;
        }
        if (mPlaybackDispatcher != null) {
            monitor.unregisterPlaybackCallback(mPlaybackDispatcher);
        }
        mPlaybackDispatcher = new PlaybackConfigDispatcher();
        monitor.registerPlaybackCallback(mPlaybackDispatcher, true);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("\nEx Dump:");
        synchronized (mLock) {
            pw.println("Handler: " + (mHandler != null));
            pw.print("FgPkgs: ");
            for (String pkg : mForegroundPkgs) {
                pw.print(pkg + " ");
            }
            pw.print("\n");
            for (int i = 0; i < mPlayers.size(); i++) {
                PlayerInfo info = mPlayers.valueAt(i);
                pw.println("piid " + mPlayers.keyAt(i) + ", uid/pid " + info.uid + "/" + info.pid
                        + " [" + info.packageName + "], mute " + info.mute + ", fg "
                        + mForegroundPkgs.contains(info.packageName));
            }
        }
    }

    private class InternalHandler extends Handler {
        InternalHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_UPDATE_PLAYERS_VOLUME:
                    onUpdatePlayersVolume();
                    break;
                case MSG_UPDATE_ALLOW_BACKGROUND_PLAYBACK:
                    onAllowBackgroundPlaybackChanged();
                    break;
            }
        }
    }

    private static void sendMsg(Handler handler, int msg, int existingMsgPolicy, int arg1,
            int arg2, Object obj, int delay) {
        if (existingMsgPolicy == SENDMSG_REPLACE) {
            handler.removeMessages(msg);
        } else if (existingMsgPolicy == SENDMSG_NOOP && handler.hasMessages(msg)) {
            return;
        }
        long time = SystemClock.uptimeMillis() + delay;
        handler.sendMessageAtTime(handler.obtainMessage(msg, arg1, arg2, obj), time);
    }

    private static class PlayerInfo {
        boolean hasBackgroundPlayPermission = false;
        boolean mute = false;
        String packageName;
        int pid;
        int piid;
        IPlayer player;
        int uid;
    }

    private class PlaybackConfigDispatcher extends IPlaybackConfigDispatcher.Stub {
        @Override
        public void dispatchPlaybackConfigChange(List<AudioPlaybackConfiguration> configs,
                boolean flush) {
            if (flush) {
                synchronized (mLock) {
                    for (int i = 0; i < mPlayers.size(); i++) {
                        int piid = mPlayers.keyAt(i);
                        boolean alive = false;
                        for (AudioPlaybackConfiguration config : configs) {
                            if (config.getPlayerInterfaceId() == piid) {
                                alive = true;
                                break;
                            }
                        }
                        if (!alive) {
                            // As in the factory, only the first released player is removed.
                            mPlayers.remove(piid);
                            break;
                        }
                    }
                }
            }
        }
    }

    private class InternalForegroundStateListener
            implements ForegroundStateProvider.ForegroundStateListener {
        @Override
        public void onForegroundPkgsChanged(ArrayList<String> pkgs) {
            ExtAudioServiceImpl.this.onForegroundPkgsChanged(pkgs);
        }

        @Override
        public void onSeeThroughStateChanged(boolean show) {
            ExtAudioServiceImpl.this.onSeeThroughStateChanged(show);
        }
    }

    private void onForegroundPkgsChanged(ArrayList<String> foregroundPkgs) {
        synchronized (mLock) {
            if (mForegroundPkgs.size() == foregroundPkgs.size()
                    && mForegroundPkgs.containsAll(foregroundPkgs)) {
                return;
            }
            mForegroundPkgs.clear();
            mForegroundPkgs.addAll(foregroundPkgs);
            if (mAllowBackgroundPlayback) {
                return;
            }
            sendMsg(mHandler, MSG_UPDATE_PLAYERS_VOLUME, SENDMSG_NOOP, 0, 0, null, 0);
        }
    }

    private void onSeeThroughStateChanged(boolean show) {
        synchronized (mLock) {
            if (show == mSeeThroughShowing) {
                return;
            }
            mSeeThroughShowing = show;
            if (mAllowBackgroundPlayback) {
                return;
            }
            sendMsg(mHandler, MSG_UPDATE_PLAYERS_VOLUME, SENDMSG_NOOP, 0, 0, null, 0);
        }
    }

    private void allowBackgroundPlaybackChanged(boolean allow) {
        synchronized (mLock) {
            if (mAllowBackgroundPlayback == allow) {
                return;
            }
            if (allow) {
                sendMsg(mHandler, MSG_UPDATE_ALLOW_BACKGROUND_PLAYBACK, SENDMSG_REPLACE, 0, 0,
                        null, 0);
                releaseHandlerThread();
            }
            mAllowBackgroundPlayback = allow;
            if (!allow) {
                setupHandlerThread();
                sendMsg(mHandler, MSG_UPDATE_ALLOW_BACKGROUND_PLAYBACK, SENDMSG_REPLACE, 0, 0,
                        null, 0);
            }
        }
    }

    private void setupHandlerThread() {
        if (mHandlerThread != null) {
            return;
        }
        mHandlerThread = new HandlerThread("AudioServiceEx");
        mHandlerThread.start();
        mHandler = new InternalHandler(mHandlerThread.getLooper());
    }

    private void releaseHandlerThread() {
        if (mHandlerThread == null) {
            return;
        }
        mHandlerThread.quitSafely();
        mHandlerThread = null;
        mHandler = null;
    }

    private void onUpdatePlayersVolume() {
        ArrayList<PlayerInfo> playersToMute = new ArrayList<>();
        ArrayList<PlayerInfo> playersToUnmute = new ArrayList<>();
        synchronized (mLock) {
            if (mAllowBackgroundPlayback) {
                return;
            }
            updatePlayersVolumeLock(playersToMute, playersToUnmute);
            mutePlayers(playersToMute, true);
            mutePlayers(playersToUnmute, false);
        }
    }

    private void onAllowBackgroundPlaybackChanged() {
        Log.i(TAG, "onAllowBackgroundPlaybackChanged " + mAllowBackgroundPlayback);
        ArrayList<PlayerInfo> playersToMute = new ArrayList<>();
        ArrayList<PlayerInfo> playersToUnmute = new ArrayList<>();
        synchronized (mLock) {
            updatePlayersVolumeLock(playersToMute, playersToUnmute);
        }
        mutePlayers(playersToMute, true);
        mutePlayers(playersToUnmute, false);
    }

    private void updatePlayersVolumeLock(ArrayList<PlayerInfo> playersToMute,
            ArrayList<PlayerInfo> playersToUnmute) {
        if (mAllowBackgroundPlayback) {
            for (int i = 0; i < mPlayers.size(); i++) {
                PlayerInfo info = mPlayers.valueAt(i);
                if (info.player != null && info.mute) {
                    playersToUnmute.add(info);
                }
            }
            return;
        }
        for (int i = 0; i < mPlayers.size(); i++) {
            PlayerInfo info = mPlayers.valueAt(i);
            if (info.player == null) {
                continue;
            }
            if (!mSeeThroughShowing && mForegroundPkgs.contains(info.packageName)) {
                if (info.mute) {
                    playersToUnmute.add(info);
                }
            } else if (!info.mute && !info.hasBackgroundPlayPermission) {
                playersToMute.add(info);
            }
        }
    }

    private void mutePlayerIfNeedLock(PlayerInfo playerInfo) {
        if (mAllowBackgroundPlayback || playerInfo.hasBackgroundPlayPermission) {
            return;
        }
        if (!mSeeThroughShowing && mForegroundPkgs.contains(playerInfo.packageName)) {
            return;
        }
        mutePlayer(playerInfo, true);
    }

    private void mutePlayers(ArrayList<PlayerInfo> players, boolean mute) {
        if (players == null) {
            return;
        }
        for (PlayerInfo info : players) {
            mutePlayer(info, mute);
        }
    }

    private boolean mutePlayer(PlayerInfo info, boolean mute) {
        try {
            info.player.setExtVolume(mute ? 0.0f : 1.0f);
            info.mute = mute;
            return true;
        } catch (RemoteException e) {
            Log.w(TAG, "mute player failed! [" + info.packageName + "], mute " + mute + " " + e);
            return false;
        }
    }

    private class InternalContentObserver extends ContentObserver {
        InternalContentObserver() {
            super(new Handler());
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange);
            if (uri.equals(Settings.System.getUriFor(sBackgroundPlaybackSettingKey))) {
                boolean allow = Settings.System.getInt(mContext.getContentResolver(),
                        sBackgroundPlaybackSettingKey, 0) == 0;
                allowBackgroundPlaybackChanged(allow);
            }
        }
    }
}
