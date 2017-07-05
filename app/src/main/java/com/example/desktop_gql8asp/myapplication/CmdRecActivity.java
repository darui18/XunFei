package com.example.desktop_gql8asp.myapplication;

import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.desktop_gql8asp.myapplication.com.example.utils.FucUtil;
import com.example.desktop_gql8asp.myapplication.com.example.utils.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.util.ResourceUtil;

public class CmdRecActivity extends AppCompatActivity {
    InitListener mInitListener;
    RecognizerListener mRecognizerListener;

    TextView mResultTv;
    TextView mErrorTv;
    Button mStartBt;
    RadioGroup mRadioGroup;
    private static final String TAG = "Speech";
    SpeechRecognizer mSpeechRecognizer;

    //语法文件
    String mCloudGrammar;
    String mLocalGrammar;
    private final String GRAMMAR_TYPE_ABNF = "abnf";
    private final String GRAMMAR_TYPE_BNF = "bnf";
    private String mCloudGrammarID;
    private String mLocalGrammarID;
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    //引擎类型


    private Toast mToast;
    // 返回结果格式，支持：xml,json
    private String mEngineType = SpeechConstant.TYPE_LOCAL;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_local_abnf);
        initView();
        initListener();
        startLocalInit();
    }

    private void initListener() {
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                if (checkedId == R.id.cloud_rb) {
                    mEngineType = SpeechConstant.TYPE_CLOUD;
                    startCloudInit();
                } else {
                    mEngineType = SpeechConstant.TYPE_LOCAL;
                    startLocalInit();
                }
            }
        });
        mStartBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
                    cancelRecoginzer();
                    startCloudRecognizer();
                } else {
                    cancelRecoginzer();
                    startLocalRecognizer();
                }
            }
        });
    }


    private void initView() {
        mResultTv = (TextView) findViewById(R.id.result_tv);
        mResultTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        mErrorTv = (TextView) findViewById(R.id.show_tv);
        mErrorTv.setMovementMethod(ScrollingMovementMethod.getInstance());
        mStartBt = (Button) findViewById(R.id.start_bt);
        mRadioGroup = (RadioGroup) findViewById(R.id.radio);
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(str);
                mToast.show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.cancel();
            mSpeechRecognizer.destroy();
            mSpeechRecognizer = null;
        }
    }

    /**
     * 本地识别流程
     */
    void startLocalInit() {
        mSpeechRecognizer = SpeechRecognizer.createRecognizer(this, new InitialListener("SpeechRecognizer"));
        if (mSpeechRecognizer == null) {
            showTip("初始化失败");
            return;
        }
        //buildGrammar 仅在使用本地语法文件时 需要使用
        mSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        mLocalGrammar = FucUtil.readFile(this, "LocalGrammar.bnf", "UTF-8");
        // 设置语法构建路径
        mSpeechRecognizer.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置语法名称
//        mSpeechRecognizer.setParameter(SpeechConstant.GRAMMAR_LIST, "LocalGrammar");

        //使用8k音频的时候请解开注释
//					mAsr.setParameter(SpeechConstant.SAMPLE_RATE, "8000");
        // 设置资源路径
        mSpeechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
//        mSpeechRecognizer.setParameter(SpeechConstant.SUBJECT, "asr");// 设置为使用命令词模式
        // 设置引擎类型
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //设置其他属性
        mSpeechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mSpeechRecognizer.setParameter(SpeechConstant.ASR_PTT, 0 + "");
        mSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置识别的门限值
        // mSpeechRecognizer.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");

        int res = mSpeechRecognizer.buildGrammar(GRAMMAR_TYPE_BNF, mLocalGrammar, new GrammarListener() {
            @Override
            public void onBuildFinish(String s, SpeechError speechError) {
                if (speechError == null) {
                    mLocalGrammarID = s;
                    showTip("本地语法构建成功 ,Grammar id = " + s);
                } else {
                    showTip("本地语法构建失败 ,SpeechError id = " + speechError.getErrorDescription() + speechError.getErrorCode());
                }
            }
        });
        if (res != ErrorCode.SUCCESS) {
            showTip("本地语法构建失败");
            return;
        }

    }

    /**
     * 取消识别
     */
    void cancelRecoginzer() {
        if (mSpeechRecognizer != null) {
            mSpeechRecognizer.cancel();
        }
    }

    /**
     * 开始本地识别
     */
    void startLocalRecognizer() {
        mSpeechRecognizer.setParameter(SpeechConstant.LOCAL_GRAMMAR, mLocalGrammarID);
        int ret = mSpeechRecognizer.startListening(recognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("识别失败,错误码: " + ret);
        }
    }


    /**
     * 云识别初始化
     */
    private void startCloudInit() {
        // 初始化
        mSpeechRecognizer = SpeechRecognizer.createRecognizer(this, new InitialListener("SpeechRecognizer"));
        if (mSpeechRecognizer == null) {
            return;
        }

        //buildGrammar 仅在使用本地语法文件时 需要使用
        mSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        mCloudGrammar = FucUtil.readFile(this, "CloudGrammar.abnf", "UTF-8");
        int res = mSpeechRecognizer.buildGrammar(GRAMMAR_TYPE_ABNF, mCloudGrammar, new GrammarListener() {
            @Override
            public void onBuildFinish(String s, SpeechError speechError) {
                if (speechError == null) {
                    mCloudGrammarID = s;
                    showTip("云端构建语法成功 ,Grammar id = " + s);
                } else {
                    showTip("云端构架语法失败 ,SpeechError id = " + speechError.getErrorDescription() + speechError.getErrorCode());

                }
            }
        });
        if (res != ErrorCode.SUCCESS) {
            showTip("云端构建语法失败");
            return;
        }
        mSpeechRecognizer.setParameter(SpeechConstant.SUBJECT, "asr");// 设置为使用命令词模式
        //设置其他属性
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        mSpeechRecognizer.setParameter(SpeechConstant.RESULT_TYPE, "json");
        mSpeechRecognizer.setParameter(SpeechConstant.ASR_PTT, 0 + "");
        mSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
    }

    /**
     * 开始云识别
     */
    void startCloudRecognizer() {

        //设置参数
        mSpeechRecognizer.setParameter(SpeechConstant.CLOUD_GRAMMAR, mCloudGrammarID); //设置本地生成的语法id，当使用云端时，不需要传id
        mSpeechRecognizer.startListening(recognizerListener);
    }

    RecognizerListener recognizerListener = new RecognizerListener() {
        @Override
        public void onVolumeChanged(int volume, byte[] bytes) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + bytes.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                String text = "";
                text = JsonParser.parseGrammarResult(result.getResultString(), SpeechConstant.TYPE_CLOUD);
//                text = JsonParser.parseIatResult(result.getResultString());
                // 显示
                Log.e("result", text);
                if (!text.startsWith("【结果】【置信度】") && !text.isEmpty()) {
                    mResultTv.setText(text);
                }
            } else {
                Log.d(TAG, "recognizer result : null");
            }
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            showTip("结束说话");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(final SpeechError error) {
            showTip("onError Code：" + error.getErrorCode());
            final String errorTip = "onError Code：" + error.getErrorCode();
            showTip(errorTip);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

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
                showTip(name + " 初始化失败");
            }
        }
    }
}
