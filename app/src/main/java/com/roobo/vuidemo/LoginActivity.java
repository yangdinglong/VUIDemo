package com.roobo.vuidemo;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.roobo.vuidemo.manager.RooboVUIManager;
import com.roobo.vuidemo.manager.VUIInitListener;
import com.roobo.vuidemo.utils.StatusBarUtil;

import static com.roobo.vuidemo.MyApplication.TAG_HEAD;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = TAG_HEAD + LoginActivity.class.getSimpleName();

    private Button mLoginBtn;

    private ProgressDialog mProgressDialog;

    private RadioButton mAutoRb;
    private RadioButton mManualRb;
    private RadioGroup mRG;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MyApplication.MSG_INIT_SUCCESS:
                    mLoginBtn.setEnabled(false);
                    mAutoRb.setEnabled(false);
                    mManualRb.setEnabled(false);
                    speak("初始化成功");
                    break;
                case MyApplication.MSG_INIT_FAIL:
                    Toast.makeText(LoginActivity.this, "初始化失败", Toast.LENGTH_SHORT).show();
                    break;
                default:
                    break;
            }
        }
    };

    private void speak(String speakMessage) {
        RooboVUIManager.getInstance(this).TTSOffline(speakMessage);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (RooboVUIManager.getInstance(LoginActivity.this).isAuto()) {
                    AutoModelActivity.launch(LoginActivity.this);
                } else {
                    ManualModeActivity.launch(LoginActivity.this);
                }

                finish();
            }
        }, 1500);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        MyApplication.initPermission(this);
        StatusBarUtil.setRootViewFitsSystemWindows(this, false);
        StatusBarUtil.setTranslucentStatus(this);
        initView();
    }

    private void initView() {
        mLoginBtn = findViewById(R.id.login_btn);
        mAutoRb = findViewById(R.id.auto_rb);
        mManualRb = findViewById(R.id.manual_rb);
        mRG = findViewById(R.id.rg);
        mRG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if (i == mAutoRb.getId()) {
                    RooboVUIManager.getInstance(LoginActivity.this).setIsAuto(true);
                } else {
                    RooboVUIManager.getInstance(LoginActivity.this).setIsAuto(false);
                }
            }
        });
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initVUI();
            }
        });
    }


    /**
     * 初始化VUI SDK
     */
    private void initVUI() {
        RooboVUIManager.getInstance(this).init(new VUIInitListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "VUIInitListener [onSuccess]");
                dismissProgressDialog();
                mHandler.obtainMessage(MyApplication.MSG_INIT_SUCCESS).sendToTarget();
            }

            @Override
            public void onFail(int code, String message) {
                Log.d(TAG, "VUIInitListener [onFail] code = " + code + "message=" + message);
                dismissProgressDialog();
                mHandler.obtainMessage(MyApplication.MSG_INIT_FAIL).sendToTarget();
            }
        });
        showProgressDialog();
    }


    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle("提示");
            mProgressDialog.setMessage("正在初始化，请稍后...");
            mProgressDialog.setCancelable(false);
        }
        if (!LoginActivity.this.isFinishing() && mProgressDialog != null && !mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    private void dismissProgressDialog() {
        if (!LoginActivity.this.isFinishing() && mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
