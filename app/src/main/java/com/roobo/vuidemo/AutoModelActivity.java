package com.roobo.vuidemo;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.roobo.vuidemo.manager.RooboVUIManager;
import com.roobo.vuidemo.manager.TTSListener;
import com.roobo.vuidemo.manager.VUIASRListener;
import com.roobo.vuidemo.manager.VUIAiListener;
import com.roobo.vuidemo.utils.StatusBarUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.roobo.vuidemo.MyApplication.TAG_HEAD;

public class AutoModelActivity extends AppCompatActivity {

    private static final String TAG = TAG_HEAD + AutoModelActivity.class.getSimpleName();

    private Button mRecordBtn;

    private RecyclerView mRecyclerView;

    private DataAdapter mMyAdapter = new DataAdapter();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyApplication.MSG_AI_RESULT:
                    mMyAdapter.addData(mQueryData);
                    mMyAdapter.addData(mHintData);
                    moveToLast();
                    break;
                default:
                    break;
            }
        }
    };

    private DataAdapter.MyData mQueryData;
    private DataAdapter.MyData mHintData;

    private LinearLayoutManager mLinearLayoutManager;

    private void createData(String query, String hint) {
        mQueryData = new DataAdapter.MyData();
        mQueryData.text = query;
        mQueryData.isReveived = false;
        mHintData = new DataAdapter.MyData();
        mHintData.text = hint;
        mHintData.isReveived = true;
        ttsSpeak(hint);
    }

    public static void launch(Context context) {
        Intent intent = new Intent(context, AutoModelActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StatusBarUtil.setRootViewFitsSystemWindows(this, false);
        StatusBarUtil.setTranslucentStatus(this);
        initView();
        setVUIListener();
    }

    private void initView() {
        mRecordBtn = findViewById(R.id.record_btn);
        mRecordBtn.setVisibility(View.GONE);
        mRecyclerView = findViewById(R.id.rv);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);
        mLinearLayoutManager.setOrientation(OrientationHelper.VERTICAL);
        mRecyclerView.setAdapter(mMyAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MediaPlayerUtil.stop();
            }
        });

    }

    public void moveToLast() {
        mRecyclerView.scrollToPosition(mMyAdapter.getItemCount() - 1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        RooboVUIManager.getInstance(this).stopRecognize();
        RooboVUIManager.getInstance(this).release();
        MediaPlayerUtil.stop();
        System.exit(0);
    }

    public void setVUIListener() {
        RooboVUIManager.getInstance(this).setAiListener(new VUIAiListener() {
            @Override
            public void onResult(String result) {
                Log.e(TAG, "VUIAiListener [onResult] result: " + result);
                String query = AIResultParser.parserQueryFromAIResultJSON(result);
                String hint = AIResultParser.parserHintFromAIResultJSON(result);
                if (AIResultParser.isStartPlayer(result)) {
                    String url = AIResultParser.parserMP3UrlFromAIResultJSON(result);
                    if (!TextUtils.isEmpty(url)) {
                        createData(query, hint);
                        mHintData.isMedia = true;
                        MediaPlayerUtil.playByUrl(url);
                        handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                    }
                } else if (AIResultParser.isExitPlayer(result)) {
                    MediaPlayerUtil.stop();
                } else {
                    createData(query, hint);
                    handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                }
                RooboVUIManager.getInstance(AutoModelActivity.this).pseudoSleep();//关闭在线识别，可以根据业务实际情况选择关闭的时间点
            }

            @Override
            public void onFail(int code, String message) {
                RooboVUIManager.getInstance(AutoModelActivity.this).pseudoSleep();//关闭在线识别，可以根据业务实际情况选择关闭的时间点
                Toast.makeText(AutoModelActivity.this, message, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "VUIAiListener [onFail] code=" + code + " message=" + message);
            }
        });
        RooboVUIManager.getInstance(this).setASRListener(new VUIASRListener() {
            @Override
            public void onResult(String result) {
                Log.d(TAG, "VUIASRListener [onResult] result:" + result);
            }

            @Override
            public void onWakeUp(String result) {
                Log.d(TAG, "VUIASRListener [onWakeUp] result:" + result);
                try {
                    JSONObject object = new JSONObject(result);
                    createData(object.optString("text"), "我在");
                    handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                RooboVUIManager.getInstance(AutoModelActivity.this).manualWakeup();
            }

            @Override
            public void onEvent(String event) {
                Log.d(TAG, "VUIASRListener [onEvent] event:" + event);
            }

            @Override
            public void onFail(int code, String message) {
                Log.e(TAG, "VUIAiListener [onFail] code=" + code + " message=" + message);
            }
        });
        RooboVUIManager.getInstance(this).startRecognize();//开始录音，唤醒后会一直在检查人声，如果要停止需要调用stopRecognize
    }


    /**
     * ttsSpeak 播放tts
     *
     * @param message tts播放的内容
     */
    private void ttsSpeak(String message) {
        RooboVUIManager.getInstance(this).TTSOnline(message, new TTSListener() {
            @Override
            public void onSpeakBegin() {
                Log.d(TAG, "RTTSListener [onSpeakBegin]");
            }

            @Override
            public void onCompleted() {
                Log.d(TAG, "RTTSListener [onCompleted]");
            }

            @Override
            public void onStop() {
                Log.d(TAG, "RTTSListener [onStop]");
            }

            @Override
            public void onError(int i) {
                Log.d(TAG, "RTTSListener [onError] i;" + i);
            }
        });
    }

    public static class DataAdapter extends RecyclerView.Adapter<DataAdapter.MyViewHolder> {

        private List<MyData> mData = new ArrayList<>();

        static class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView textTv;

            public MyViewHolder(View v) {
                super(v);
                textTv = v.findViewById(R.id.text_tv);
            }
        }

        public DataAdapter() {
        }

        public void addData(MyData data) {
            mData.add(data);
            this.notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {

            if (mData.get(position).isMedia) {
                holder.textTv.setText(mData.get(position).text.concat(("(多媒体资源)")));
                holder.textTv.setTextColor(Color.RED);
            } else {
                holder.textTv.setText(mData.get(position).text);
                holder.textTv.setTextColor(Color.BLACK);
            }

            if (mData.get(position).isReveived) {
                holder.textTv.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
            } else {
                holder.textTv.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
            }
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item, parent, false);
            return new MyViewHolder(v);
        }

        public static class MyData {
            private String text;
            private boolean isMedia;
            private boolean isReveived;
        }
    }

}
