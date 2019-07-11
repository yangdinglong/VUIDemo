package com.roobo.vuidemo.manager;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.text.TextUtils;
import android.util.Log;

import com.roobo.vui.RError;
import com.roobo.vui.api.AutoTypeController;
import com.roobo.vui.api.IASRController;
import com.roobo.vui.api.VUIApi;
import com.roobo.vui.api.asr.RASRListener;
import com.roobo.vui.api.tts.RTTSListener;
import com.roobo.vui.api.tts.RTTSPlayer;
import com.roobo.vui.business.auth.entity.UserInfo;
import com.roobo.vui.common.recognizer.ASRResult;
import com.roobo.vui.common.recognizer.EventType;
import com.roobo.vui.recognizer.OnAIResponseListener;
import com.roobo.vuidemo.CustomAndroidAudioGenerator;
import com.roobo.vuidemo.MyApplication;

import java.lang.reflect.Method;

/**
 * Created by HP on 2019/7/10.
 */

public class RooboVUIManager implements com.roobo.vui.api.InitListener {

    public static final String TAG = MyApplication.TAG_HEAD + RooboVUIManager.class.getSimpleName();

    /**
     * roobo分配【保密】，需要更换成对应项目的ID
     */
    private final static String APP_ID = "zMWY0MGZiMDUyYTh";
    /**
     * roobo分配【保密】，需要更换成对应项目的PUBLIC_KEY
     */
    private final static String PUBLIC_KEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQC+tRBGVsDipoExE++l97Gyn1pY89rC6WFDcTYe9gsZCGwgBuHi0dqBdckuWTXJplCGL+PnxjyYpRQOwizAXJ5On5KA6TUDCZufhQ4dcOW7/dBPjRQt2l5WF0U0M9evPi14N4B3kogwvGhfoHsv6YL+shakKZ3lgD9gmWx+KM7qdQIDAQAB";

    private static RooboVUIManager manager;

    private VUIInitListener mInitListener;

    private boolean mIsAuto = true;

    private Context mContext;

    private boolean mIsInit;

    private VUIASRListener mVUIASRListener;

    private VUIAiListener mVUIAiListener;

    private IASRController mIASRController;

    private RooboVUIManager(Context context) {
        this.mContext = context.getApplicationContext();
    }

    public static RooboVUIManager getInstance(Context context) {
        if (manager == null) {
            synchronized (RooboVUIManager.class) {
                if (manager == null) {
                    manager = new RooboVUIManager(context);
                }
            }
        }
        return manager;
    }


    public void init(VUIInitListener initListener) {
        mInitListener = initListener;
        UserInfo userInfo = new UserInfo();
        userInfo.setDeviceID(getDeviceID()); //必须设置此字段
        userInfo.setAgentID(APP_ID); //必须设置此字段
        userInfo.setPublicKey(PUBLIC_KEY); //必须设置此字段
        VUIApi.getInstance().setLogLevel(5);//;日志级别0-5,默认是0, 最高级别是5
        VUIApi.InitParam.InitParamBuilder builder = new VUIApi.InitParam.InitParamBuilder();

        builder.setEnv(VUIApi.EnvType.ENV_ONLINE)//请根据分配的Appid配置环境
                .setUserInfo(userInfo)  //设置用户信息，必须设置
                .addOfflineFileName("test_offline") //设置离线词文件，必须设置
                .setAudioGenerator(new CustomAndroidAudioGenerator())//配置音频源
                .setVUIType(mIsAuto ? VUIApi.VUIType.AUTO : VUIApi.VUIType.MANUAL);  //设置交互方式，AUTO（唤醒后采用VAD进行断句） MANUAL(手动断句)
        VUIApi.getInstance().init(mContext, builder.build(), this);//绑定初始化监听器
    }

    public void setListener() {
        VUIApi.getInstance().setASRListener(new RASRListener() {
            @Override
            public void onASRResult(ASRResult asrResult) {
                Log.d(TAG, "[onASRResult] ASRResult:" + asrResult.getResultText());
                if (mVUIASRListener != null) {
                    mVUIASRListener.onResult(asrResult.getResultText());
                }
            }

            @Override
            public void onFail(RError rError) {
                Log.d(TAG, "[onFail] RError code=" + rError.getFailCode() + " message:" + rError.getFailDetail());
                if (mVUIASRListener != null) {
                    mVUIASRListener.onFail(rError.getFailCode(), rError.getFailDetail());
                }
            }

            @Override
            public void onWakeUp(String s) {
                Log.d(TAG, "[onWakeUp] s=" + s);
                if (mVUIASRListener != null) {
                    mVUIASRListener.onWakeUp(s);
                }
            }

            @Override
            public void onEvent(EventType eventType) {
                Log.d(TAG, "[onEvent] EventType=" + eventType.name());
                if (mVUIASRListener != null) {
                    mVUIASRListener.onEvent(eventType.name());
                }
            }
        });

        VUIApi.getInstance().setOnAIResponseListener(new OnAIResponseListener() {
            @Override
            public void onResult(String s) {
                Log.d(TAG, "[onResult] s=" + s);
                if (mVUIAiListener != null) {
                    mVUIAiListener.onResult(s);
                }
            }

            @Override
            public void onFail(RError rError) {
                Log.d(TAG, "[onFail] RError code=" + rError.getFailCode() + " message:" + rError.getFailDetail());
                if (mVUIAiListener != null) {
                    mVUIAiListener.onFail(rError.getFailCode(), rError.getFailDetail());
                }
            }
        });
    }

    public void setASRListener(VUIASRListener listener) {
        mVUIASRListener = listener;
    }

    public void setAiListener(VUIAiListener listener) {
        mVUIAiListener = listener;
        if (mVUIAiListener != null) {
            VUIApi.getInstance().setCloudRecognizeMode(VUIApi.VUIRecognizeMode.VUI_MODE_AI);//设置有ai返回
        } else {
            VUIApi.getInstance().setCloudRecognizeMode(VUIApi.VUIRecognizeMode.VUI_MODE_NO_AI);//设置有ai返回
        }
    }

    public void release() {
        if (!mIsInit) {
            Log.d(TAG, "[TTSOnline] plz login first");
            return;
        }
        VUIApi.getInstance().release();
    }

    /**
     * 获取设备唯一标识符，分为两种方式：
     * 1 预分配并生产线烧录方式，直接将分配的SN号赋值给deviceID；
     * 2 通过唯一标识符在线注册方式，获取设备的唯一标识符赋值给deviceID；唯一标识可以采用序列号、MAC地址、IME号
     *
     * @return
     */
    public static String getDeviceID() {
        return getDeviceSerialNO();
    }

    /**
     * 获取设备的唯一标识符，此函数做为参考，不保证返回标识符的唯一性，开发者需要根据自身设备的特性自己实现
     *
     * @return
     */
    public static String getDeviceSerialNO() {
        String serial = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String) get.invoke(c, "ro.serialno");
        } catch (Exception ignored) {
        }
        if (TextUtils.isEmpty(serial)) {
            serial = android.os.Build.SERIAL;
        }
        return serial;
    }

    public void setIsAuto(boolean isAuto) {
        mIsAuto = isAuto;
    }

    public boolean isAuto() {
        return mIsAuto;
    }


    //开始录音，唤醒后会一直在检查人声，如果要停止需要调用stopRecognize
    public IASRController startRecognize() {
        if (!mIsInit) {
            Log.d(TAG, "[startRecognize] plz login first");
            return null;
        }
        mIASRController = VUIApi.getInstance().startRecognize();
        return mIASRController;
    }

    public void stopRecognize() {
        if (!mIsInit) {
            Log.d(TAG, "[stopRecognize] plz login first");
            return;
        }
        VUIApi.getInstance().stopRecognize();
    }

    public void manualWakeup() {
        if (mIASRController instanceof AutoTypeController) {
            ((AutoTypeController) mIASRController).manualWakeup();
        }
    }

    public void pseudoSleep() {
        if (mIASRController instanceof AutoTypeController) {
            ((AutoTypeController) mIASRController).pseudoSleep();
        }
    }


    public void TTSOnline(String message) {
        if (!mIsInit) {
            Log.d(TAG, "[TTSOnline] plz login first");
            return;
        }
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_ONLINE, message);
    }

    public void TTSOnline(String message, final TTSListener listener) {
        if (!mIsInit) {
            Log.d(TAG, "[TTSOnline] plz login first");
            return;
        }
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_ONLINE, message, new RTTSListener() {
            @Override
            public void onSpeakBegin() {
                if (listener != null) {
                    listener.onSpeakBegin();
                }
            }

            @Override
            public void onCompleted() {
                if (listener != null) {
                    listener.onCompleted();
                }
            }

            @Override
            public void onStop() {
                if (listener != null) {
                    listener.onStop();
                }
            }

            @Override
            public void onError(int i) {
                if (listener != null) {
                    listener.onError(i);
                }
            }
        });
    }

    public void TTSOffline(String message) {
        if (!mIsInit) {
            Log.d(TAG, "[TTSOffline] plz login first");
            return;
        }
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_OFFLINE, message);
    }

    public void TTSOffline(String message, final TTSListener listener) {
        if (!mIsInit) {
            Log.d(TAG, "[TTSOnline] plz login first");
            return;
        }
        VUIApi.getInstance().speak(RTTSPlayer.TTSType.TYPE_OFFLINE, message, new RTTSListener() {
            @Override
            public void onSpeakBegin() {
                if (listener != null) {
                    listener.onSpeakBegin();
                }
            }

            @Override
            public void onCompleted() {
                if (listener != null) {
                    listener.onCompleted();
                }
            }

            @Override
            public void onStop() {
                if (listener != null) {
                    listener.onStop();
                }
            }

            @Override
            public void onError(int i) {
                if (listener != null) {
                    listener.onError(i);
                }
            }
        });
    }

    public void stopTTS() {
        if (!mIsInit) {
            Log.d(TAG, "[stopTTS] plz login first");
            return;
        }
        VUIApi.getInstance().stopSpeak();
    }

    @Override
    public void onSuccess() {
        mIsInit = true;
        if (mInitListener != null) {
            mInitListener.onSuccess();
        }
        reportLocation();
        setListener();
    }

    /**
     * reportLocation wifi信息，只有上报了wifi信息，当用户查询跟位置相关的信息时才会返回结果，比如：今天的天气怎么样
     */
    private void reportLocation() {
        if (!mIsInit) {
            Log.d(TAG, "[reportLocation] plz login first");
            return;
        }
        WifiManager wifiManager = (WifiManager) mContext.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        VUIApi.getInstance().reportLocationInfo(wifiManager.getScanResults());
    }

    @Override
    public void onFail(RError rError) {
        if (mInitListener != null) {
            mInitListener.onFail(rError.getFailCode(), rError.getFailDetail());
        }
    }
}
