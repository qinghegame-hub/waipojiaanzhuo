package com.egret.openadsdk;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.content.Context;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTBannerAd;
import com.bytedance.sdk.openadsdk.TTInteractionAd;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.FilterWord;
import com.egret.openadsdk.sdk.AdCode;
import com.egret.openadsdk.sdk.FullScreenVideoActivity;
import com.egret.openadsdk.sdk.RewardVideoActivity;
import com.egret.openadsdk.sdk.SplashActivity;
import com.egret.openadsdk.sdk.TTAdManagerHolder;
import com.egret.openadsdk.sdk.TToast;
import com.google.gson.JsonObject;


import org.egret.runtime.launcherInterface.INativePlayer;
import org.egret.egretnativeandroid.EgretNativeAndroid;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

//Android项目发布设置详见doc目录下的README_ANDROID.md

public class MainActivity extends Activity {
    private final String TAG = "MainActivity";
    private EgretNativeAndroid nativeAndroid;
    private TTAdNative mTTAdNative;
    private FrameLayout mExpressContainer;
    public static MainActivity instance = null;
   // private NframeLayout mExpressContainer;
   private Context mContext;
    private TTAdDislike mTTAdDislike;
    private TTNativeExpressAd mTTAd;
    private long startTime = 0;
    private boolean mHasShowDownloadActive = false;
    View bannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = this;
        nativeAndroid = new EgretNativeAndroid(this);
        if (!nativeAndroid.checkGlEsVersion()) {
            Toast.makeText(this, "This device does not support OpenGL ES 2.0.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        nativeAndroid.config.showFPS = false;
        nativeAndroid.config.fpsLogTime = 30;
        nativeAndroid.config.disableNativeRender = false;
        nativeAndroid.config.clearCache = false;
        nativeAndroid.config.loadingTimeout = 0;

        setExternalInterfaces();

        if (!nativeAndroid.initialize("http://tool.egret-labs.org/Weiduan/game/index.html")) {
            Toast.makeText(this, "Initialize native failed.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        setContentView(nativeAndroid.getRootFrameLayout());

        this.initJSEvent();

        //step2:创建TTAdNative对象，createAdNative(Context context) banner广告context需要传入Activity对象
        mTTAdNative = TTAdManagerHolder.get().createAdNative(this);
        // 申请部分权限,建议在sdk初始化前申请,如：READ_PHONE_STATE、ACCESS_COARSE_LOCATION及ACCESS_FINE_LOCATION权限，
        // 以获取更好的广告推荐效果，如read_phone_state,防止获取不了imei时候，下载类广告没有填充的问题。
        TTAdManagerHolder.get().requestPermissionIfNecessary(this);
    }

    public void initJSEvent() {
        //监听来自JS的开屏视频消息
        nativeAndroid.setExternalInterface("TTSplashAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, SplashActivity.class);
                intent.putExtra("splash_code", AdCode.splash_code);
                intent.putExtra("is_express", false);
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的激励视频消息
        nativeAndroid.setExternalInterface("TTRewardVideoAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, RewardVideoActivity.class);
                intent.putExtra("horizontal_rit", AdCode.reward_horizontal_code);
                intent.putExtra("vertical_rit", AdCode.reward_vertical_code);

                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean is_horizontal = object.getBoolean("is_horizontal");
                    intent.putExtra("is_horizontal", is_horizontal);
                    String userID = object.getString("userID");
                    intent.putExtra("userID", userID);
                    int rewardAmount = object.getInt("rewardAmount");
                    intent.putExtra("rewardAmount", rewardAmount);
                    String rewardName = object.getString("rewardName");
                    intent.putExtra("rewardName", rewardName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的全屏视频消息
        nativeAndroid.setExternalInterface("TTFullScreenVideoAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                Intent intent = new Intent(MainActivity.this, FullScreenVideoActivity.class);
                intent.putExtra("horizontal_rit", AdCode.full_horizontal_code);
                intent.putExtra("vertical_rit", AdCode.full_vertical_code);

                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean is_horizontal = object.getBoolean("is_horizontal");
                    intent.putExtra("is_horizontal", is_horizontal);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                startActivityForResult(intent, AdCode.OPENADSDK);
            }
        });
        //监听来自JS的Banner广告消息
        nativeAndroid.setExternalInterface("TTBannerExpressAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                String code = AdCode.banner_code;
                if (mExpressContainer == null) {
                    mExpressContainer = nativeAndroid.getRootFrameLayout();
                }
                JSONObject object = null;
                try {
                    object = new JSONObject(dataFromJs);
                    Boolean iskaiguan = object.getBoolean("iskaiguan");
                    if(iskaiguan == false){
                        if (bannerView != null) {
                            mExpressContainer.removeView(bannerView);
                            bannerView = null;
                        }
                    }else{
                            Boolean is_top = object.getBoolean("is_top");
                            int width;
                            int height;
                            width = object.getInt("width");
                            height = object.getInt("height");
                            MainActivity.instance.loadNewBannerAd(code, width, height);
                            //TToast.show(MainActivity.this, "banner开");
                        }
                 } 
                 catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        //监听来自JS的插屏广告消息
        nativeAndroid.setExternalInterface("TTInteractionAd", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String dataFromJs) {
                String code = AdCode.interaction_code;
                JSONObject object = null;
                try {
                    int width;
                    int height;
                    object = new JSONObject(dataFromJs);
                    Boolean iskaiguan = object.getBoolean("iskaiguan");
                    if(iskaiguan == false){
                        if (bannerView != null) {
                            mExpressContainer.removeView(bannerView);
                            bannerView = null;
                        }
                    }else {
                        width = object.getInt("width");
                        height = object.getInt("height");
                        MainActivity.instance.loadInteractionAd(code, width, height);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /*private void loadBannerAd(String codeId, final boolean isTop, final int width, final int height) {
        //step4:创建广告请求参数AdSlot,具体参数含义参考文档

        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId) //广告位id
                .setSupportDeepLink(true)
                //.setImageAcceptedSize(600, 257)
                .setImageAcceptedSize(width, height)
                .build();
        //step5:请求广告，对请求回调的广告作渲染处理
        mTTAdNative.loadBannerAd(adSlot, new TTAdNative.BannerAdListener() {
            @Override
            public void onError(int code, String message) {
                TToast.show(MainActivity.this, "load error : " + code + ", " + message);
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                }
                JsonObject json = new JsonObject();
                json.addProperty("event", "onError");
                MainActivity.jsEvent(AdCode.BannerExpressAd, json.toString());
            }
            @Override
            public void onBannerAdLoad(final TTBannerAd ad) {
                if (ad == null) {
                    return;
                }
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                    bannerView = null;
                }
                bannerView = ad.getBannerView();
                if (bannerView == null) {
                    return;
                }
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rect.width(), (int) ((((double) height) / ((double) width)) * rect.width()));
                if (!isTop) {
                    params.gravity = Gravity.BOTTOM;
                } else {
                    params.gravity = Gravity.TOP;
                }
                bannerView.setLayoutParams(params);
                //设置轮播的时间间隔  间隔在30s到120秒之间的值，不设置默认不轮播
                ad.setSlideIntervalTime(30 * 1000);
                mExpressContainer.addView(bannerView);
                //设置广告互动监听回调
                ad.setBannerInteractionListener(new TTBannerAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked(View view, int type) {
                        TToast.show(MainActivity.this, "广告被点击");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdClicked");
                        MainActivity.jsEvent(AdCode.BannerExpressAd, json.toString());
                    }
                    @Override
                    public void onAdShow(View view, int type) {
                        TToast.show(MainActivity.this, "广告展示");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdShow");
                        MainActivity.jsEvent(AdCode.BannerExpressAd, json.toString());
                    }
                });
                //在banner中显示网盟提供的dislike icon，有助于广告投放精准度提升
                ad.setShowDislikeIcon(new TTAdDislike.DislikeInteractionCallback() {
                    @Override
                    public void onSelected(int position, String value) {
                        TToast.show(MainActivity.this, "点击 " + value);
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onSelected");
                        json.addProperty("value", value);
                        MainActivity.jsEvent(AdCode.BannerExpressAd, json.toString());
                        //用户选择不喜欢原因后，移除广告展示
                        //mExpressContainer.removeAllViews();
                        if (bannerView != null) {
                            mExpressContainer.removeView(bannerView);
                            bannerView = null;
                        }
                    }

                    @Override
                    public void onCancel() {
                        TToast.show(MainActivity.this, "点击取消 ");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onCancel");
                        MainActivity.jsEvent(AdCode.BannerExpressAd, json.toString());
                    }
                });
            }
        });
    }*/


    @Override
    protected void onPause() {
        super.onPause();
        nativeAndroid.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        nativeAndroid.resume();
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            nativeAndroid.exitGame();
        }

        return super.onKeyDown(keyCode, keyEvent);
    }

    private void setExternalInterfaces() {
        nativeAndroid.setExternalInterface("sendToNative", new INativePlayer.INativeInterface() {
            @Override
            public void callback(String message) {
                String str = "Native get message: ";
                str += message;
                Log.d(TAG, str);
                nativeAndroid.callExternalInterface("sendToJS", str);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == AdCode.OPENADSDK&&resultCode == AdCode.SplashAd) {
//        }else if(requestCode == AdCode.OPENADSDK&&resultCode == AdCode.RewardVideoAd) {
//        }
    }

    public static void jsEvent(int adcode, String json) {
        String tag = "";
        if (adcode == AdCode.RewardVideoAd) {
            tag = "TTRewardVideoAd-js";
        } else if (adcode == AdCode.SplashAd) {
            tag = "TTSplashAd-js";
        } else if (adcode == AdCode.FullScreenVideoAd) {
            tag = "TTFullScreenVideoAd-js";
        } else if (adcode == AdCode.BannerExpressAd) {
            tag = "TTBannerExpressAd-js";
        } else if (adcode == AdCode.InteractionAd) {
            tag = "TTInteractionAd-js";
        }
        MainActivity.instance.send2JS(tag, json);
    }

    public void send2JS(String tag, String json) {
        nativeAndroid.callExternalInterface(tag, json);
    }

    /*private void loadInteractionAd(String codeId, final int width, final int height) {
        //step4:创建插屏广告请求参数AdSlot,具体参数含义参考文档
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId)
                .setSupportDeepLink(true)
                .setImageAcceptedSize(width, height) //根据广告平台选择的尺寸，传入同比例尺寸
                .build();
        //step5:请求广告，调用插屏广告异步请求接口
        mTTAdNative.loadInteractionAd(adSlot, new TTAdNative.InteractionAdListener() {
            @Override
            public void onError(int code, String message) {
                //TToast.show(getApplicationContext(), "code: " + code + "  message: " + message);
                JsonObject json = new JsonObject();
                json.addProperty("event", "onError");
                MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
            }

            @Override
            public void onInteractionAdLoad(TTInteractionAd ttInteractionAd) {
                //TToast.show(getApplicationContext(), "type:  " + ttInteractionAd.getInteractionType());
                ttInteractionAd.setAdInteractionListener(new TTInteractionAd.AdInteractionListener() {
                    @Override
                    public void onAdClicked() {
                        Log.d(TAG, "被点击");
                        //TToast.show(MainActivity.this, "广告被点击");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdClicked");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
                    }

                    @Override
                    public void onAdShow() {
                        Log.d(TAG, "被展示");
                        //TToast.show(MainActivity.this, "广告被展示");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdShow");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
                    }

                    @Override
                    public void onAdDismiss() {
                        Log.d(TAG, "插屏广告消失");
                        //TToast.show(MainActivity.this, "广告消失");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdDismiss");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
                    }
                });
                //弹出插屏广告
                ttInteractionAd.showInteractionAd(MainActivity.this);
            }
        });
    }*/

    private void loadNewBannerAd(String codeId, int expressViewWidth, int expressViewHeight) {
        System.out.println("load1");
        //mExpressContainer.removeAllViews();
        //step4:创建广告请求参数AdSlot,具体参数含义参考文档
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId) //广告位id
                .setSupportDeepLink(true)
                .setAdCount(3) //请求广告数量为1到3条
                .setExpressViewAcceptedSize(640, 0) //期望模板广告view的size,单位dp
                .build();
        //step5:请求广告，对请求回调的广告作渲染处理
        mTTAdNative.loadBannerExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                //TToast.show(MainActivity.this, "load error : " + code + ", " + message);
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                }
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0) {
                    return;
                }

                mTTAd = ads.get(0);
                bindAdListener(mTTAd);
                startTime = System.currentTimeMillis();
                mTTAd.render();
            }
        });
    }


    private void bindAdListener(TTNativeExpressAd ad) {
        ad.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
            @Override
            public void onAdClicked(View view, int type) {
                //TToast.show(MainActivity.this, "广告被点击");
            }

            @Override
            public void onAdShow(View view, int type) {
                //TToast.show(MainActivity.this, "广告展示");
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                // Log.e("ExpressView","render fail:"+(System.currentTimeMillis() - startTime));
                //TToast.show(MainActivity.this, msg + " code:" + code);
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                //Log.e("ExpressView","render suc:"+(System.currentTimeMillis() - startTime));
                //返回view的宽高 单位 dp
                /*if (ad == null) {
                    return;
                }*/
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                    bannerView = null;
                }
                //bannerView = ad.getBannerView();
                /*if (bannerView == null) {
                    return;
                }*/
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rect.width(), (int) ((((double) height) / ((double) width)) * rect.width()));
                params.gravity = Gravity.BOTTOM;
                bannerView = view;
                bannerView.setLayoutParams(params);
                //设置轮播的时间间隔  间隔在30s到120秒之间的值，不设置默认不轮播
                ad.setSlideIntervalTime(30 * 1000);
                mExpressContainer.addView(bannerView);
               // Log.e("ExpressView", "render suc:" + (System.currentTimeMillis() - startTime));
                //返回view的宽高 单位 dp
               // TToast.show(mContext, "渲染成功");
                /*
                view.setLayoutParams(params);
                mExpressContainer.addView(bannerView);*/
            }
        });
        //dislike设置
        bindDislike(ad, false);
        if (ad.getInteractionType() != TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
            return;
        }
        ad.setDownloadListener(new TTAppDownloadListener() {
            @Override
            public void onIdle() {
                //TToast.show(MainActivity.this, "点击开始下载", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                if (!mHasShowDownloadActive) {
                    mHasShowDownloadActive = true;
                    //TToast.show(MainActivity.this, "下载中，点击暂停", Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                //TToast.show(MainActivity.this, "下载暂停，点击继续", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                //TToast.show(MainActivity.this, "下载失败，点击重新下载", Toast.LENGTH_LONG);
            }

            @Override
            public void onInstalled(String fileName, String appName) {
                //TToast.show(MainActivity.this, "安装完成，点击图片打开", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                //TToast.show(MainActivity.this, "点击安装", Toast.LENGTH_LONG);
            }
        });
    }

    /**
     * 设置广告的不喜欢, 注意：强烈建议设置该逻辑，如果不设置dislike处理逻辑，则模板广告中的 dislike区域不响应dislike事件。
     *
     * @param ad
     * @param customStyle 是否自定义样式，true:样式自定义
     */
    private void bindDislike(TTNativeExpressAd ad, boolean customStyle) {
        //使用默认模板中默认dislike弹出样式
        // ad.getExpressAdView().setPadding(0,0,0,0);
        ad.setDislikeCallback(MainActivity.this, new TTAdDislike.DislikeInteractionCallback() {
            @Override
            public void onSelected(int position, String value) {
                //TToast.show(MainActivity.this, "点击 " + value);
                //用户选择不喜欢原因后，移除广告展示
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                }
            }

            @Override
            public void onCancel() {
                //TToast.show(MainActivity.this, "点击取消 ");
            }
        });
    }


    //插屏广告
    //设置广告参数
    private void loadInteractionAd(String codeId, final int width, final int height) {
        //step4:创建插屏广告请求参数AdSlot,具体参数含义参考文档
        AdSlot adSlot = new AdSlot.Builder()
                .setCodeId(codeId) //广告位id
                .setSupportDeepLink(true)
                .setAdCount(1) //请求广告数量为1到3条
                .setExpressViewAcceptedSize(300,300) //期望个性化模板广告view的size,单位dp
                .setImageAcceptedSize(600,600 )//这个参数设置即可，不影响个性化模板广告的size
                .build();
       //加载广告
       //step5:请求广告，调用插屏广告异步请求接口
        mTTAdNative.loadInteractionExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
            @Override
            public void onError(int code, String message) {
                //TToast.show(MainActivity.this, "load error : " + code + ", " + message);
                mExpressContainer.removeAllViews();
                //TToast.show(getApplicationContext(), "code: " + code + "  message: " + message);
                JsonObject json = new JsonObject();
                json.addProperty("event", "onError");
                MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
            }

            @Override
            public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
                if (ads == null || ads.size() == 0){
                    return;
                }
                mTTAd = ads.get(0);
                chapinhuitiao(mTTAd);
                mTTAd.render();//调用render开始渲染广告
            }
        });
    }
    //绑定广告行为
    private void chapinhuitiao(TTNativeExpressAd ad) {
        ad.setExpressInteractionListener(new TTNativeExpressAd.AdInteractionListener() {

            @Override
            public void onAdDismiss() {
                //TToast.show(mContext, "广告关闭");
                Log.d(TAG, "插屏广告消失");
                        //TToast.show(MainActivity.this, "广告消失");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdDismiss");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
            }

            @Override
            public void onAdClicked(View view, int type) {
                //TToast.show(mContext, "广告被点击");
                Log.d(TAG, "被点击");
                        //TToast.show(MainActivity.this, "广告被点击");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdClicked");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
            }

            @Override
            public void onAdShow(View view, int type) {
                //TToast.show(mContext, "广告展示");
                Log.d(TAG, "被展示");
                        //TToast.show(MainActivity.this, "广告被展示");
                        JsonObject json = new JsonObject();
                        json.addProperty("event", "onAdShow");
                        MainActivity.jsEvent(AdCode.InteractionAd, json.toString());
            }

            @Override
            public void onRenderFail(View view, String msg, int code) {
                Log.e("ExpressView","render fail:"+(System.currentTimeMillis() - startTime));
                //TToast.show(mContext, msg+" code:"+code);
            }

            @Override
            public void onRenderSuccess(View view, float width, float height) {
                //返回view的宽高 单位 dp
                //TToast.show(mContext, "渲染成功");
                //在渲染成功回调时展示广告，提升体验
                if (bannerView != null) {
                    mExpressContainer.removeView(bannerView);
                    bannerView = null;
                }
                Rect rect = new Rect();
                getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(rect.width(), (int) ((((double) height) / ((double) width)) * rect.width()));
                params.gravity = Gravity.CENTER;
                int kuang = dip2px(MainActivity.this,width);
                Log.d(TAG, "---->" + kuang);
                params.leftMargin = (rect.width() / 2 - kuang / 2);
                bannerView = view;
                bannerView.setLayoutParams(params);
                mExpressContainer.addView(bannerView); 
                //mExpressContainer.addView(view);
                //ttInteractionAd.showInteractionAd(MainActivity.this);   
            }
        });

        if (ad.getInteractionType() != TTAdConstant.INTERACTION_TYPE_DOWNLOAD){
            return;
        }
        //可选，下载监听设置
        ad.setDownloadListener(new TTAppDownloadListener() {
            @Override
            public void onIdle() {
                TToast.show(MainActivity.this, "点击开始下载", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
                if (!mHasShowDownloadActive) {
                    mHasShowDownloadActive = true;
                    TToast.show(MainActivity.this, "下载中，点击暂停", Toast.LENGTH_LONG);
                }
            }

            @Override
            public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
                TToast.show(MainActivity.this, "下载暂停，点击继续", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
                TToast.show(MainActivity.this, "下载失败，点击重新下载", Toast.LENGTH_LONG);
            }

            @Override
            public void onInstalled(String fileName, String appName) {
                TToast.show(MainActivity.this, "安装完成，点击图片打开", Toast.LENGTH_LONG);
            }

            @Override
            public void onDownloadFinished(long totalBytes, String fileName, String appName) {
                TToast.show(MainActivity.this, "点击安装", Toast.LENGTH_LONG);
            }
        });
    }

    //在合适的时机，释放广告的资源
    protected void chapinshifang () {
        super.onDestroy();
        if (mTTAd != null) {
            //调用destroy()方法释放
            mTTAd.destroy();
        }
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }





}




