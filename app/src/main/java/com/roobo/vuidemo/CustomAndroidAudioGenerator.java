package com.roobo.vuidemo;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import com.roobo.vui.api.audiosource.BaseAudioGenerator;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

/***
 * 自定义录音
 */
public class CustomAndroidAudioGenerator implements BaseAudioGenerator {

    public static final String TAG = "CustomAndroidAudio";

    private AudioRecord mAudioRecord;
    private int mRecBufSize;
    private byte[] mDataBuffer;

    private static final int AUDIO_SAMPLE_RATE = 16000;
    private static final int FRAME_SIZE = 2560;
    private byte[] mWriteDataBuf = new byte[FRAME_SIZE];

    // 20*80 = 1600ms
    private final BlockingQueue<byte[]> mRecorderData = new LinkedBlockingDeque<>(20);

    private volatile boolean mFinished;
    private HandlerThread mWorkerThread;
    private Handler mWorkerHandler;

    public CustomAndroidAudioGenerator() {
        mRecBufSize = AudioRecord.getMinBufferSize(AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mDataBuffer = new byte[mRecBufSize];
    }

    @Override
    public int getChannelCount() {
        return 1;
    }

    @Override
    public int getRefCount() {
        return 0;
    }

    class Timer {
        long lastTime;

        void check(String msg) {
            long now = System.currentTimeMillis();
            Log.d(CustomAndroidAudioGenerator.TAG, msg + ": " + (now - lastTime));
            lastTime = now;
        }
    }

    private Timer timer = new Timer();

    @Override
    public short[][] getNextFrame() {
//        timer.check("[getNextFrame]");
        short[][] data = new short[1][];
        synchronized (mRecorderData) {
            byte[] bytes = null;
            try {
                bytes = mRecorderData.poll(3000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (bytes != null) {
                data[0] = convertByteArrayToShortArray(bytes, FRAME_SIZE, true);
            }
        }
        return data;
    }

    private Runnable mReadTask = new Runnable() {

        private void pushDataQueueLocked() {
            if (mFinished) {
                return;
            }
            byte[] data = new byte[FRAME_SIZE];
            System.arraycopy(mWriteDataBuf, 0, data, 0, FRAME_SIZE);
            if (!mRecorderData.offer(data)) {
                Log.d(CustomAndroidAudioGenerator.TAG, "[pushDataQueueLocked]: too slow to take data. clear all.");
                mRecorderData.clear();
                mRecorderData.add(data); // 这里不再验证能否成功，仅靠其抛异常
            }
        }

        @Override
        public void run() {
            Log.d(CustomAndroidAudioGenerator.TAG, "RooboRecorder begin recording... + mMinBufferSize=" + mRecBufSize);
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    16000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, mRecBufSize * 2);
            mAudioRecord.startRecording();
            int lastWriteRemain = 0;

            while (!mFinished) {
                int len = mAudioRecord.read(mDataBuffer, 0, mRecBufSize);
                if (len <= 0) {
                    Log.e(CustomAndroidAudioGenerator.TAG, "read error,lastWriteRemain:" + lastWriteRemain + " len:" + len);
                    continue;
                }
                int pos = 0;

                if (lastWriteRemain > 0) {
                    int new_frame_need = FRAME_SIZE - lastWriteRemain;
                    if (new_frame_need > len) {
                        Log.d(TAG, "lastWriteRemain:" + lastWriteRemain + " len:" + len);
                        System.arraycopy(mDataBuffer, 0, mWriteDataBuf, lastWriteRemain, len);
                        lastWriteRemain += len;
                        continue;
                    }
                    System.arraycopy(mDataBuffer, 0, mWriteDataBuf, lastWriteRemain, new_frame_need);
                    pos += new_frame_need;
//                    mAudioSource.process(mWriteDataBuf, FRAME_SIZE);
                    pushDataQueueLocked();
                }

                while ((pos + FRAME_SIZE) <= len) {
                    System.arraycopy(mDataBuffer, pos, mWriteDataBuf, 0, FRAME_SIZE);
                    pos += FRAME_SIZE;
//                    mAudioSource.process(mWriteDataBuf, FRAME_SIZE);
                    pushDataQueueLocked();
                }

                lastWriteRemain = len - pos;
                if (lastWriteRemain > 0) {
                    System.arraycopy(mDataBuffer, pos, mWriteDataBuf, 0, len - pos);
                }
            }
            synchronized (CustomAndroidAudioGenerator.this) {
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
                CustomAndroidAudioGenerator.this.notifyAll();
            }

            Log.d(TAG, "RooboRecorder finish recording...");
        }
    };

    @Override
    synchronized public void onStart() {
        if (mWorkerThread != null || mWorkerHandler != null) {
            Log.e(TAG, "[onStart]: recorder has been started!!!");
        }
        mFinished = false;
        mWorkerThread = new HandlerThread("RooboRecorder", Process.THREAD_PRIORITY_URGENT_AUDIO);
        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());
        mWorkerHandler.post(mReadTask);
    }

    @Override
    synchronized public void onStop() {
        mFinished = true;
        try {
            wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mWorkerHandler.removeMessages(0);
        mWorkerHandler = null;
        mWorkerThread.quitSafely();
        mWorkerThread = null;
    }

    private short[] convertByteArrayToShortArray(byte[] data, int length, boolean littleEndian) {
        short[] shorts = new short[length / 2];
        for (int i = 0; i < length - 1; i += 2) {
            if (littleEndian) {
                shorts[i / 2] = (short) ((((data[i + 1] << 8) & 0xFF00) | (data[i] & 0xFF)));
            } else {
                shorts[i / 2] = (short) ((((data[i] << 8) & 0xFF00) | (data[i + 1] & 0xFF)));
            }
        }

        return shorts;
    }
}
