package com.roobo.vuidemo.manager;

/**
 * Created by HP on 2019/7/10.
 */

public interface VUIInitListener {
    void onSuccess();

    void onFail(int code, String message);

}
