package com.roobo.vuidemo;

import android.app.AlertDialog;
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

import com.roobo.vui.RError;
import com.roobo.vui.api.AutoTypeController;
import com.roobo.vui.api.IASRController;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.asr.RASRListener;
import com.roobo.vui.api.tts.RTTSPlayer;
import com.roobo.vui.common.recognizer.ASRResult;
import com.roobo.vui.common.recognizer.EventType;
import com.roobo.vui.recognizer.OnAIResponseListener;
import com.roobo.vuidemo.utils.StatusBarUtil;

import java.util.ArrayList;
import java.util.List;

public class ManualModeActivity extends AppCompatActivity {

    private static final String TAG = ManualModeActivity.class.getSimpleName();

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

    //设置AI回调接口。AI返回的结果都在此接口中回调，如果不需要AI结果，可以不设置此回调接口。
    private OnAIResponseListener aiResponseListener = new OnAIResponseListener() {
        @Override
        public void onResult(final String json) {
            dismissProgressDialog();
            Log.e(TAG, "OnAIResponseListener [onResult] json: " + json);
            String query = AIResultParser.parserQueryFromAIResultJSON(json);
            String hint = AIResultParser.parserHintFromAIResultJSON(json);
            if (AIResultParser.isStartPlayer(json)) {
                String url = AIResultParser.parserMP3UrlFromAIResultJSON(json);
                if (!TextUtils.isEmpty(url)) {
                    createData(query, hint);
                    hintData.isMedia = true;
                    MediaPlayerUtil.playByUrl(url);
                    handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
                }
            } else if (AIResultParser.isExitPlayer(json)) {
                MediaPlayerUtil.stop();
            } else {
                createData(query, hint);
                handler.obtainMessage(MyApplication.MSG_AI_RESULT).sendToTarget();
            }
        }

        @Override
        public void onFail(RError message) {
            dismissProgressDialog();
            Toast.makeText(ManualModeActivity.this, message.getFailDetail(), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "OnAIResponseListener [onFail]: " + message.getFailDetail());
        }
    };

    private void createData(String query, String hint) {
        queryData = new MyAdapter.MyData();
        queryData.text = query;
        queryData.isReveived = false;
        hintData = new MyAdapter.MyData();
        hintData.text = hint;
        hintData.isReveived = true;
        ttsSpeak(hint);
    }

    private RASRListener mRASRListener=new RASRListener() {
        @Override
        public void onASRResult(ASRResult asrResult) {
            Log.d(TAG, "RASRListener [onASRResult] asrResult:" + asrResult.getResultText());
        }

        @Override
        public void onFail(RError rError) {
            Log.d(TAG, "RASRListener [onFail] RError:" + rError.getFailDetail());
        }

        @Override
        public void onWakeUp(String s) {
            Log.d(TAG, "RASRListener [onWakeUp] s:" + s);
        }

        @Override
        public void onEvent(EventType eventType) {
            Log.d(TAG, "RASRListener [onEvent] EventType:" + eventType.name());
        }
    };

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
                        IASRController mIASRController = VUIApi.getInstance().startRecognize();
                        if (mIASRController instanceof AutoTypeController) {
                            ((AutoTypeController) mIASRController).manualWakeup();
                        }
                        MediaPlayerUtil.stop();
                        VUIApi.getInstance().stopSpeak();
                        mRecordBtn.setBackgroundResource(R.drawable.record_btn_bg_pressed);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        break;
                    default:
                        mRecordBtn.setBackgroundResource(R.drawable.record_btn_background);
                        VUIApi.getInstance().stopRecognize();
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
        VUIApi.getInstance().stopRecognize();
        VUIApi.getInstance().release();
        MediaPlayerUtil.stop();
        System.exit(0);
    }

    public void setVUIListener() {
        VUIApi.getInstance().setCloudRecognizeMode(VUIApi.VUIRecognizeMode.VUI_MODE_AI);//设置有ai返回
        VUIApi.getInstance().setOnAIResponseListener(aiResponseListener); //绑定AI监听器；如果不需要AI结果，可以不设置
        VUIApi.getInstance().setASRListener(mRASRListener);
    }


    /**
     * ttsSpeak 播放tts
     *
     * @param message tts播放的内容
     */
    private void ttsSpeak(String message) {
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_ONLINE, message);
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
