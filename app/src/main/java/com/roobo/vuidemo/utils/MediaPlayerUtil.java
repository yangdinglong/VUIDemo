package com.roobo.vuidemo.utils;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;

/**
 * Created by chengyijun on 2018/9/15.
 */

public class MediaPlayerUtil {
    private static MediaPlayer mediaPlayer;

    static {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public static void playByUrl(String url) {
        if (!TextUtils.isEmpty(url)) {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(url);
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    @Override
                    public void onPrepared(MediaPlayer mp) {
                        mediaPlayer.start();
                    }
                });
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
                        Log.d("MediaPlayerUtil","onError");
                        return false;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void stop() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }
}
