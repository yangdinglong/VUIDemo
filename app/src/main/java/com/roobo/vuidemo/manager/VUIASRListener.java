package com.roobo.vuidemo.manager;

/**
 * Created by HP on 2019/7/10.
 */

public interface VUIASRListener {
    void onResult(String result);

    void onWakeUp(String result);

    void onEvent(String event);

    void onFail(int code, String message);

}
