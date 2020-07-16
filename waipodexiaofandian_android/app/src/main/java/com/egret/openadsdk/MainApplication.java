package com.egret.openadsdk;

import android.app.Application;

import com.egret.openadsdk.sdk.TTAdManagerHolder;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        //穿山甲SDK初始化
        //强烈建议在应用对应的Application#onCreate()方法中调用，避免出现content为null的异常
        TTAdManagerHolder.init(this);
        //如果明确某个进程不会使用到广告SDK，可以只针对特定进程初始化广告SDK的content
        //if (PROCESS_NAME_XXXX.equals(processName)) {
        //   TTAdManagerHolder.init(this)
        //}
    }
}
