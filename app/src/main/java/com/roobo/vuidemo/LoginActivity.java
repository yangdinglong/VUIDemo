package com.roobo.vuidemo;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
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

import com.roobo.vui.RError;
import com.roobo.vui.api.InitListener;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.tts.RTTSPlayer;
import com.roobo.vui.business.auth.entity.UserInfo;
import com.roobo.vuidemo.utils.StatusBarUtil;

import static com.roobo.vuidemo.MyApplication.TAG_HEAD;

public class LoginActivity extends AppCompatActivity implements InitListener {

    private static final String TAG = TAG_HEAD + LoginActivity.class.getSimpleName();

    private Button mLoginBtn;

    private ProgressDialog mProgressDialog;

    private RadioButton mAutoRb;
    private RadioButton mManualRb;
    private RadioGroup mRG;

    private boolean mIsAuto = true;

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
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_OFFLINE, speakMessage);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mIsAuto) {
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
                    mIsAuto = true;
                } else {
                    mIsAuto = false;
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
        UserInfo userInfo = new UserInfo();
        userInfo.setDeviceID(MyApplication.getDeviceID()); //必须设置此字段
        userInfo.setAgentID(MyApplication.APP_ID); //必须设置此字段
        userInfo.setPublicKey(MyApplication.PUBLIC_KEY); //必须设置此字段
        VUIApi.getInstance().setLogLevel(5);//;日志级别0-5,默认是0, 最高级别是5
        VUIApi.InitParam.InitParamBuilder builder = new VUIApi.InitParam.InitParamBuilder();

        builder.setEnv(VUIApi.EnvType.ENV_ONLINE)//请根据分配的Appid配置环境
                .setUserInfo(userInfo)  //设置用户信息，必须设置
                .addOfflineFileName("test_offline") //设置离线词文件，必须设置
                .setAudioGenerator(new CustomAndroidAudioGenerator())//配置音频源
                .setVUIType(mIsAuto ? VUIApi.VUIType.AUTO : VUIApi.VUIType.MANUAL);  //设置交互方式，AUTO（唤醒后采用VAD进行断句） MANUAL(手动断句)
        VUIApi.getInstance().init(this, builder.build(), this);//绑定初始化监听器
        showProgressDialog();
    }


    @Override
    public void onSuccess() {
        Log.d(TAG, "InitListener [onSuccess]");
        reportLocation(); //上报wifi信息，用于识别中用到位置信息
        dismissProgressDialog();
        mHandler.obtainMessage(MyApplication.MSG_INIT_SUCCESS).sendToTarget();
    }

    @Override
    public void onFail(RError message) {
        Log.d(TAG, "InitListener [onFail] message = " + message.getFailDetail());
        dismissProgressDialog();
        mHandler.obtainMessage(MyApplication.MSG_INIT_FAIL).sendToTarget();
    }

    /**
     * reportLocation wifi信息，只有上报了wifi信息，当用户查询跟位置相关的信息时才会返回结果，比如：今天的天气怎么样
     */
    private void reportLocation() {
        WifiManager wifiManager = (WifiManager) getApplication().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        VUIApi.getInstance().reportLocationInfo(wifiManager.getScanResults());

    }

    private void showProgressDialog() {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            mProgressDialog.setTitle("提示");
            mProgressDialog.setMessage("正在初始化，请稍后...");
            mProgressDialog.setCancelable(false);
        }
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    private void dismissProgressDialog() {
        if (!LoginActivity.this.isFinishing() && mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }
}
