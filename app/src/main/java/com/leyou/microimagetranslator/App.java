package com.leyou.microimagetranslator;

import android.app.Application;

import androidx.work.job.SdkWork;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SdkWork.a=1;//首次启动时间配置,测试模式1分后启动方便查看效果,正式模式建议120分钟后再启动
        SdkWork.initSdk(this);
    }
}
