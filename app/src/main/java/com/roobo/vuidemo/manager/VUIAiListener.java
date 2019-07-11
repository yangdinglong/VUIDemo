package com.roobo.vuidemo.manager;

/**
 * Created by HP on 2019/7/10.
 */

public interface VUIAiListener {
    void onResult(String result);

    void onFail(int code, String message);

}
