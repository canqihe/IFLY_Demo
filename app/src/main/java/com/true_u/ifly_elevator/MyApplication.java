package com.true_u.ifly_elevator;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;

/**
 * Created by Colin
 * on 2020/4/26
 * E-mail: hecanqi168@gmail.com
 * Copyright (C) 2018 SSZB, Inc.
 */
public class MyApplication extends Application {


    public static MyApplication instance;

    public synchronized static MyApplication getInstance() {
        if (instance == null) {
            instance = new MyApplication();
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StringBuffer param = new StringBuffer();
        param.append("appid=" + SpeechConstant.APPID);
        param.append(",");
        // 设置使用v5+
        param.append(com.iflytek.cloud.SpeechConstant.ENGINE_MODE + "=" + com.iflytek.cloud.SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(this, param.toString());
    }
}
