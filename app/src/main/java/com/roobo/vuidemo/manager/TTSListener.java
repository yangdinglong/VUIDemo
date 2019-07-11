package com.roobo.vuidemo.manager;

/**
 * Created by HP on 2019/7/10.
 */

public interface TTSListener {


     void onSpeakBegin();


     void onCompleted();


      void onStop();


     void onError(int i);
}
