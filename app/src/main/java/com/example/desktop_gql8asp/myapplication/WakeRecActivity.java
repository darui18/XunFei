package com.example.desktop_gql8asp.myapplication;

import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.desktop_gql8asp.myapplication.com.example.utils.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.Timer;
import java.util.TimerTask;

import static com.example.desktop_gql8asp.myapplication.com.example.utils.FucUtil.readFile;

public class WakeRecActivity extends AppCompatActivity {

    private static final String TAG = "WakeRec1002";
    TextView mRecogResultTv;
    TextView mRecogStatusTv;
    ProgressBar mProgressBar;
    private VoiceWakeuper mVoiceWakeuper;
    private SpeechRecognizer mSpeechRecognizer;
    private String mLocalGrammar;
    private Toast mToast;

    // 本地语法构建路径
    private String mGrmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/speech";

    private final String GRAMMAR_TYPE_BNF = "bnf";
    private String mLocalGrammarID;
    private String resultString;
    private String recoString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wake_rec);
        initView();
        initWakeRecog();

    }

    @Override
    protected void onDestroy() {
        stopListening();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.destroy();
        }
        if (mVoiceWakeuper != null) {
            mVoiceWakeuper.cancel();
            mVoiceWakeuper.destroy();
        }
        super.onDestroy();
    }

    private void initView() {
        mRecogResultTv = (TextView) findViewById(R.id.wake_recog_result_tv);
        mRecogStatusTv = (TextView) findViewById(R.id.wake_recog_status_tv);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mToast = Toast.makeText(WakeRecActivity.this, "", Toast.LENGTH_SHORT);
    }

    private void initWakeRecog() {
        // 初始化
        mVoiceWakeuper = VoiceWakeuper.createWakeuper(this, new WakeRecActivity.InitialListener("VoiceWakeuper"));
        mSpeechRecognizer = SpeechRecognizer.createRecognizer(this, new WakeRecActivity.InitialListener("SpeechRecognizer"));
        // 初始化语法文件
        mLocalGrammar = readFile(this, "WakeLocalGrammar.bnf", "utf-8");
        buildLocalGrammar();
    }


    /**
     * 本地识别流程
     */
    void buildLocalGrammar() {
        //buildGrammar 仅在使用本地语法文件时 需要使用
        mSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        // 设置语法构建路径
        mSpeechRecognizer.setParameter(ResourceUtil.GRM_BUILD_PATH, mGrmPath);
        // 设置资源路径
        mSpeechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置引擎类型
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //不带标点
        mSpeechRecognizer.setParameter(SpeechConstant.ASR_PTT, 0 + "");

        mSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        //设置为命令词模式
        mSpeechRecognizer.setParameter(SpeechConstant.SUBJECT, "asr");

        int res = mSpeechRecognizer.buildGrammar(GRAMMAR_TYPE_BNF, mLocalGrammar, new GrammarListener() {
            @Override
            public void onBuildFinish(String s, SpeechError speechError) {
                if (speechError == null) {
                    mLocalGrammarID = s;
                    mSpeechRecognizer.setParameter(SpeechConstant.LOCAL_GRAMMAR, mLocalGrammarID);
                    startOnShot();
                    Log.e(TAG, "build local grammar success");
                } else {
                    showTip("本地语法构建失败 ,SpeechError id = " + speechError.getErrorDescription() + speechError.getErrorCode());
                }
            }
        });
        if (res != ErrorCode.SUCCESS) {
            showTip("本地语法构建失败");
        }
    }


    private void startOnShot() {
        Log.e(TAG, "start onshot");
        mVoiceWakeuper = VoiceWakeuper.getWakeuper();
        if (mVoiceWakeuper != null) {
            resultString = "";
            recoString = "";
            mRecogResultTv.setText(resultString);

            final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
            // 清空参数
            mVoiceWakeuper.setParameter(SpeechConstant.PARAMS, null);
            // 设置唤醒资源路径
            mVoiceWakeuper.setParameter(ResourceUtil.IVW_RES_PATH, resPath);
            // 设置唤醒+识别模式
            mVoiceWakeuper.setParameter(SpeechConstant.IVW_SST, "oneshot");
            // 设置返回结果格式
            mVoiceWakeuper.setParameter(SpeechConstant.RESULT_TYPE, "json");
            // 设置识别引擎
            mVoiceWakeuper.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
            //不带标点
            mVoiceWakeuper.setParameter(SpeechConstant.ASR_PTT, "0");
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mVoiceWakeuper.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            mVoiceWakeuper.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            if (!TextUtils.isEmpty(mLocalGrammarID)) {
                // 设置本地识别资源
                mVoiceWakeuper.setParameter(ResourceUtil.ASR_RES_PATH,
                        getResourcePath());
                // 设置语法构建路径
                mVoiceWakeuper.setParameter(ResourceUtil.GRM_BUILD_PATH, mGrmPath);
                // 设置本地识别使用语法id
                mVoiceWakeuper.setParameter(SpeechConstant.LOCAL_GRAMMAR,
                        mLocalGrammarID);
                mVoiceWakeuper.startListening(mWakeuperListener);
                Log.e(TAG, "start  onshot listener");
            } else {
                showTip("请先构建语法");
            }
        }
    }


    /**
     * 初始化监听
     */
    class InitialListener implements InitListener {
        String name;

        InitialListener(String name) {
            this.name = name;
        }

        @Override
        public void onInit(int i) {
            if (i != 0) {
                showTip(name + " Init error");
            }
        }

    }

    /**
     * 唤醒监听
     */
    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            mProgressBar.setVisibility(View.VISIBLE);
            Log.e(TAG, "wake recognizer");
            try {
                String text = result.getResultString();
                JSONObject object;
                object = new JSONObject(text);
                StringBuffer buffer = new StringBuffer();
                buffer.append("【RAW】 " + text);
                buffer.append("\n");
                buffer.append("【操作类型】" + object.optString("sst"));
                buffer.append("\n");
                buffer.append("【唤醒词id】" + object.optString("id"));
                buffer.append("\n");
                buffer.append("【得分】" + object.optString("score"));
                buffer.append("\n");
                buffer.append("【前端点】" + object.optString("bos"));
                buffer.append("\n");
                buffer.append("【尾端点】" + object.optString("eos"));
                resultString = buffer.toString();
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            mRecogStatusTv.setText(resultString);
            recoString = "";
            mRecogResultTv.setText("");
        }

        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            if (error.getErrorCode() == ErrorCode.ERROR_NO_MATCH && mVoiceWakeuper != null) {
                mVoiceWakeuper.startListening(mWakeuperListener);
            } else {
                startListening();
            }
        }

        @Override
        public void onBeginOfSpeech() {
//            showTip("开始监听");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.e(TAG, "wakeuplistener onEvent eventType:" + eventType + "arg1:" + isLast + "arg2:" + arg2);
            // 识别结果
            if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult reslut = ((RecognizerResult) obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
                recoString += JsonParser.parseGrammarResult(reslut.getResultString());
                mRecogResultTv.setText(recoString);
            }
            //开启语音识别监听
            if (isLast == 1) {
                if (mSpeechRecognizer != null) {
                    mVoiceWakeuper.cancel();
                    mTimeOut = false;
                    startListening();
                    startTimer();
                    Log.e(TAG, "begin speech recognizer");
                } else {
                    showTip("开启语音继续识别出错");
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub
        }
    };

    /**
     * 计时器相关操作
     */

    private void startTimer() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "start timer");
                if (mTimer == null) {
                    mTimer = new Timer();
                    mStopTimerTask = new StopTimerTask();
                    mTimer.schedule(mStopTimerTask, 0, 1 * 1000);
                }
                mBeginTime = System.currentTimeMillis();
            }
        });
    }

    Timer mTimer;
    StopTimerTask mStopTimerTask;
    volatile long mBeginTime;
    boolean mTimeOut = false;

    /**
     * 语音识别停止定时器
     */
    class StopTimerTask extends TimerTask {

        @Override
        public void run() {
            long time = System.currentTimeMillis() - mBeginTime;
            Log.e(TAG, "stop timerTask time = " + time + ";beginTime= " + mBeginTime + " ;currentTime = " + System.currentTimeMillis());
            if (time > 8 * 1000) {
                stopListening();
            }
        }
    }

    /**
     * ui线程下停止监听
     */
    private void stopListening() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "stop listening");
                mTimeOut = true;
                mProgressBar.setVisibility(View.INVISIBLE);
                if (mStopTimerTask != null) {
                    mStopTimerTask.cancel();
                    mStopTimerTask = null;
                }
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                if (mSpeechRecognizer != null) {
                    mSpeechRecognizer.stopListening();
                    mSpeechRecognizer.cancel();
                }
                if (mVoiceWakeuper != null && !mVoiceWakeuper.isListening()) {
                    mVoiceWakeuper.startListening(mWakeuperListener);
                }
            }
        });
    }

    /**
     * ui线程下开始监听
     */
    void startListening() {
//        showTip("开始监听，请说命令");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.e(TAG, "start listening");
                if (mSpeechRecognizer != null && !mSpeechRecognizer.isListening() && !mTimeOut) {
                    mSpeechRecognizer.startListening(mRecognizerListener);
                }
            }
        });
    }

    /**
     * 识别监听
     */
    RecognizerListener mRecognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int volume, byte[] bytes) {
//            showTip("当前正在说话，音量大小：" + volume);
            Log.e(TAG, "返回音频数据：" + bytes.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            Log.e(TAG, "RecognizerListener onresult");
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.e(TAG, "recognizer result：" + result.getResultString());
                String text = "";
//                text = JsonParser.parseGrammarResult(result.getResultString(), SpeechConstant.TYPE_CLOUD);
                text = JsonParser.parseGrammarResult(result.getResultString());
                // 显示
                Log.e("result", text);
                if (!text.startsWith("【结果】【置信度】") && !text.isEmpty()) {
                    mRecogResultTv.setText(text);
                    if (getCredit(result.getResultString()) > 10) {
                        Log.e(TAG, "scrod > 10; timer reset");
                        startTimer();
                    }
                }
            } else {
                Log.e(TAG, "recognizer result : null");
            }
            startListening();
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
//            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
//            showTip("开始说话");
        }

        @Override
        public void onError(final SpeechError error) {
            Log.e(TAG, "RecognizerListening onError" + "onError Code：" + error.getErrorCode());
            if (error.getErrorCode() == ErrorCode.ERROR_NO_MATCH) {
                mRecogResultTv.setText("无匹配结果");
                startListening();
            } else {
                mSpeechRecognizer.cancel();
                stopListening();
                final String errorTip = "onError Code：" + error.getErrorCode();
                showTip(errorTip);
                startListening();
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.e(TAG, "session id =" + sid);
            //	}
        }
    };

    private void showTip(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(s);
                mToast.show();
            }
        });
    }

    //获取识别资源路径 离线使用
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        //识别8k资源-使用8k的时候请解开注释
//		tempBuffer.append(";");
//		tempBuffer.append(ResourceUtil.generateResourcePath(this, RESOURCE_TYPE.assets, "asr/common_8k.jet"));
        return tempBuffer.toString();
    }

    private int getCredit(String json) {
        int scord = -1;
        Log.d("Speech", json);
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                if (words.getJSONObject(i).getString("slot").equals("<operate>")) {
                    JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                    for (int j = 0; j < items.length(); j++) {
                        JSONObject obj = items.getJSONObject(j);
                        scord = obj.getInt("sc");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret.append("没有匹配结果.");
        }
        return scord;
    }
}
