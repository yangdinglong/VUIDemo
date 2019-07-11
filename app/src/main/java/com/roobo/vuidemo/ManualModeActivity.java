package com.roobo.vuidemo;


import android.app.ProgressDialog;
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
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.roobo.vuidemo.manager.RooboVUIManager;
import com.roobo.vuidemo.manager.VUIASRListener;
import com.roobo.vuidemo.manager.VUIAiListener;
import com.roobo.vuidemo.utils.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

public class ManualModeActivity extends AppCompatActivity {

    private static final String TAG = MyApplication.TAG_HEAD + ManualModeActivity.class.getSimpleName();

    private Button mRecordBtn;

    private RecyclerView mRecyclerView;

    private MyAdapter mMyAdapter = new MyAdapter();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyApplication.MSG_AI_RESULT:
                    mMyAdapter.addData(queryData);
                    mMyAdapter.addData(hintData);
                    moveToLast();
                    break;
                default:
                    break;
            }
        }
    };

    private MyAdapter.MyData queryData;
    private MyAdapter.MyData hintData;

    private ProgressDialog mProgressDialog;

    private LinearLayoutManager mLinearLayoutManager;

    private void createData(String query, String hint) {
        queryData = new MyAdapter.MyData();
        queryData.text = query;
        queryData.isReveived = false;
        hintData = new MyAdapter.MyData();
        hintData.text = hint;
        hintData.isReveived = true;
        ttsSpeak(hint);
    }

    public static void launch(Context context) {
        Intent intent = new Intent(context, ManualModeActivity.class);
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

        mRecordBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        RooboVUIManager.getInstance(ManualModeActivity.this).startRecognize();
                        RooboVUIManager.getInstance(ManualModeActivity.this).manualWakeup();
                        MediaPlayerUtil.stop();
                        RooboVUIManager.getInstance(ManualModeActivity.this).stopTTS();
                        mRecordBtn.setBackgroundResource(R.drawable.record_btn_bg_pressed);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        mRecordBtn.setBackgroundResource(R.drawable.record_btn_background);
                        RooboVUIManager.getInstance(ManualModeActivity.this).stopRecognize();
                        showProgressDialog();
                        break;
                }
                return true;
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
                dismissProgressDialog();
                Log.e(TAG, "VUIAiListener [onResult] result: " + result);
                String query = AIResultParser.parserQueryFromAIResultJSON(result);
                String hint = AIResultParser.parserHintFromAIResultJSON(result);
                if (AIResultParser.isStartPlayer(result)) {
                    String url = AIResultParser.parserMP3UrlFromAIResultJSON(result);
                    if (!TextUtils.isEmpty(url)) {
                        createData(query, hint);
                        hintData.isMedia = true;
                        MediaPlayerUtil.playByUrl(url);
                        handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                    }
                } else if (AIResultParser.isExitPlayer(result)) {
                    MediaPlayerUtil.stop();
                } else {
                    createData(query, hint);
                    handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                }
            }

            @Override
            public void onFail(int code, String message) {
                dismissProgressDialog();
                Toast.makeText(ManualModeActivity.this, message, Toast.LENGTH_SHORT).show();
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
    }


    /**
     * ttsSpeak 播放tts
     *
     * @param message tts播放的内容
     */
    private void ttsSpeak(String message) {
        RooboVUIManager.getInstance(this).TTSOffline(message);
    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle("提示");
            mProgressDialog.setMessage("识别中，请稍后...");
        }
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (!ManualModeActivity.this.isFinishing() && mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    public static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

        private List<MyData> mData = new ArrayList<>();

        static class MyViewHolder extends RecyclerView.ViewHolder {
            public TextView textTv;

            public MyViewHolder(View v) {
                super(v);
                textTv = v.findViewById(R.id.text_tv);
            }
        }

        public MyAdapter() {
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
