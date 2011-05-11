package com.cpdev;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.cpdev.utils.StringUtils;

import java.io.IOException;

public class PlayerService extends NotificationService {

    private static final String TAG = "com.cpdev.PlayerService";
    RadioActivity caller;

    private final IBinder mBinder = new RadioServiceBinder();

    public boolean alreadyPlaying() {
        MediaPlayer mediaPlayer = ((RadioApplication) getApplicationContext()).getMediaPlayer();
        if (mediaPlayer != null) {
            boolean playing = mediaPlayer.isPlaying();
            return playing;
        } else {
            return false;
        }
    }

    @Override
    public void onCreate() {
        RadioApplication radioApplication = (RadioApplication) getApplicationContext();
        MediaPlayer mediaPlayer = radioApplication.getMediaPlayer();
        super.onCreate();
        if (mediaPlayer == null) {
            mediaPlayer = new MediaPlayer();
        }
        radioApplication.setMediaPlayer(mediaPlayer);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public void stopPlaying(RadioActivity view) {
        RadioApplication radioApplication = (RadioApplication) getApplicationContext();
        MediaPlayer mediaPlayer = radioApplication.getMediaPlayer();

        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }

        view.setStatus("Stopped playing");
        cancelNotification(NotificationService.PLAYING_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void startPlaying(RadioActivity view, final RadioDetails radioDetails) {

        caller = view;
        RadioApplication radioApplication = (RadioApplication) getApplicationContext();
        MediaPlayer mediaPlayer = radioApplication.getMediaPlayer();

        try {

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            }

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {

                public void onCompletion(MediaPlayer mediaPlayer) {
                    if (mediaPlayer.isPlaying()) {    //should be false if error occurred
                        mediaPlayer.start();
                    }
                }
            });

            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                    Log.e(TAG, "Damn error occurred");
                    caller.setStatus("Error");
                    return true;
                }
            });

            mediaPlayer.setDataSource(radioDetails.getStreamUrl());
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mediaPlayer.start();
                    StringBuilder status = new StringBuilder("Playing ");
                    if (!StringUtils.IsNullOrEmpty(radioDetails.getStationName())) {
                        status.append(radioDetails.getStationName());
                    }
                    caller.setStatus(status.toString());

                    String operation = "Playing ";
                    CharSequence tickerText = StringUtils.IsNullOrEmpty(radioDetails.getStationName()) ? operation : operation + radioDetails.getStationName();
                    CharSequence contentText = StringUtils.IsNullOrEmpty(radioDetails.getStationName()) ? operation : operation + radioDetails.getStationName();
                    showNotification(NotificationService.PLAYING_ID, radioDetails, tickerText, contentText);
                }
            });


        } catch (IOException ioe) {
            Log.e(TAG, "Error caught in play", ioe);
            ioe.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            mediaPlayer.reset();
        } finally {
            radioApplication.setMediaPlayer(mediaPlayer);
            radioApplication.setPlayingStation(radioDetails);
        }
    }

    public class RadioServiceBinder extends Binder {
        PlayerService getService() {
            return PlayerService.this;
        }
    }
}


