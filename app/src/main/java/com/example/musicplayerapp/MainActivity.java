package com.example.musicplayerapp;

import android.app.Service;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.Context;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

public class MusicService extends Service {

    private static final String CHANNEL_ID = "music_channel";

    private MediaPlayer player;
    private List<String> playlist = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPaused = false;
    private float playbackSpeed = 1.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        player = new MediaPlayer();
        player.setOnCompletionListener(mp -> nextSong());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getStringExtra("action");
            String path = intent.getStringExtra("path");

            if ("play".equals(action) && path != null) {
                playlist.clear();
                playlist.add(path);
                currentIndex = 0;
                playMusic(playlist.get(currentIndex));
            } else if ("pause".equals(action)) {
                pauseMusic();
            } else if ("resume".equals(action)) {
                resumeMusic();
            } else if ("stop".equals(action)) {
                stopMusic();
            } else if ("next".equals(action)) {
                nextSong();
            } else if ("prev".equals(action)) {
                previousSong();
            } else if ("speed".equals(action)) {
                float speed = intent.getFloatExtra("speed", 1.0f);
                setSpeed(speed);
            } else if ("playlist".equals(action) && path != null) {
                addToPlaylist(path);
            }
        }
        return START_NOT_STICKY;
    }

    private void playMusic(String path) {
        try {
            if (player.isPlaying()) {
                player.stop();
            }
            player.reset();
            player.setDataSource(this, Uri.parse(path));
            player.prepare();
            player.start();
            setSpeed(playbackSpeed); // apply current speed
            isPaused = false;
            showNotification("Playing", path);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pauseMusic() {
        if (player.isPlaying()) {
            player.pause();
            isPaused = true;
            showNotification("Paused", playlist.get(currentIndex));
        }
    }

    private void resumeMusic() {
        if (isPaused) {
            player.start();
            isPaused = false;
            showNotification("Playing", playlist.get(currentIndex));
        }
    }

    private void stopMusic() {
        if (player.isPlaying() || isPaused) {
            player.stop();
            player.reset();
            isPaused = false;
            stopForeground(true);
        }
    }

    private void nextSong() {
        if (currentIndex + 1 < playlist.size()) {
            currentIndex++;
            playMusic(playlist.get(currentIndex));
        } else {
            stopForeground(true);
        }
    }

    private void previousSong() {
        if (currentIndex - 1 >= 0) {
            currentIndex--;
            playMusic(playlist.get(currentIndex));
        }
    }

    private void addToPlaylist(String path) {
        playlist.add(path);
    }

    private void setSpeed(float speed) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && player != null) {
            try {
                PlaybackParams params = new PlaybackParams();
                params.setSpeed(speed);
                player.setPlaybackParams(params);
                playbackSpeed = speed;
                showNotification("Playing at " + speed + "x", playlist.get(currentIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showNotification(String status, String song) {
        Intent pauseIntent = new Intent(this, MusicService.class);
        pauseIntent.putExtra("action", isPaused ? "resume" : "pause");
        PendingIntent pausePendingIntent = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.putExtra("action", "stop");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 2, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent nextIntent = new Intent(this, MusicService.class);
        nextIntent.putExtra("action", "next");
        PendingIntent nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent prevIntent = new Intent(this, MusicService.class);
        prevIntent.putExtra("action", "prev");
        PendingIntent prevPendingIntent = PendingIntent.getService(this, 4, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent speedIntent = new Intent(this, MusicService.class);
        speedIntent.putExtra("action", "speed");
        speedIntent.putExtra("speed", nextSpeed(playbackSpeed));
        PendingIntent speedPendingIntent = PendingIntent.getService(this, 5, speedIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = new Notification.Builder(this)
                .setContentTitle(status)
                .setContentText(song)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
                .addAction(android.R.drawable.ic_media_pause, isPaused ? "Resume" : "Pause", pausePendingIntent)
                .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
                .addAction(android.R.drawable.ic_menu_manage, (playbackSpeed + "x"), speedPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .setOngoing(!isPaused);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }

        startForeground(1002, builder.build());
    }

    private float nextSpeed(float current) {
        if (current < 1.25f) return 1.25f;
        if (current < 1.5f) return 1.5f;
        if (current < 1.75f) return 1.75f;
        if (current < 2.0f) return 2.0f;
        return 1.0f; // reset về 1.0x
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Music Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
