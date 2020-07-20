package com.true_u.ifly_elevator;

import android.app.Application;

import com.iflytek.cloud.SpeechUtility;
import com.true_u.ifly_elevator.util.Constant;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.cache.converter.SerializableDiskConverter;

/**
 * Created by Colin
 * on 2020/4/26
 * E-mail: hecanqi168@gmail.com
 */
public class MyApplication extends Application {

    public static volatile MyApplication instance;

    public synchronized static MyApplication getInstance() {
        if (instance == null) {
            synchronized (MyApplication.class) {
                if (instance == null)
                    instance = new MyApplication();
            }
        }
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        StringBuffer param = new StringBuffer();
        param.append("appid=" + Constant.APPID);
        param.append(",");
        // 设置使用v5+
        param.append(com.iflytek.cloud.SpeechConstant.ENGINE_MODE + "=" + com.iflytek.cloud.SpeechConstant.MODE_MSC);
        SpeechUtility.createUtility(this, param.toString());

        EasyHttp.init(this);
        EasyHttp.getInstance()
                .debug("RxEasyHttp", true)
                .setReadTimeOut(60 * 1000)
                .setWriteTimeOut(60 * 1000)
                .setConnectTimeout(60 * 1000)
                .setRetryCount(3)//默认网络不好自动重试3次
                .setRetryDelay(500)//每次延时500ms重试
                .setRetryIncreaseDelay(500)//每次延时叠加500ms
                .setCacheDiskConverter(new SerializableDiskConverter())//默认缓存使用序列化转化
                .setCacheMaxSize(50 * 1024 * 1024)//设置缓存大小为50M
                .setCacheVersion(1)//缓存版本为1
                .setCertificates();//信任所有证书
    }
}
