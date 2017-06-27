package com.example.desktop_gql8asp.myapplication;

import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.desktop_gql8asp.myapplication.com.example.utils.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;

import org.json.JSONException;
import org.json.JSONObject;

import static com.example.desktop_gql8asp.myapplication.com.example.utils.FucUtil.readFile;

public class WakeActivity extends AppCompatActivity {
    private static final String TAG = "Speech";
    TextView mWakeReusltTv, mResultTv;
    RadioGroup mRadioGroup;
    VoiceWakeuper mVoiceWakeuper;
    SpeechRecognizer mSpeechRecognizer;
    Switch mSwitch;

    //声音
    private SoundPool soundPool;
    int anlaosunID;
    int chianlaosunID;
    int baohushiliID;
    int bajieID;
    int yeyeID;
    float volume = 0.2f;

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
    String mEngineType = SpeechConstant.TYPE_LOCAL;
    private Toast mToast;

    //门限值
    private final static int MIN = 5;
    private int curThresh = MIN;

    // 唤醒结果内容
    private String resultString;
    String recoString;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        soundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 5);
        anlaosunID = soundPool.load(this, R.raw.qitaindasheng, 1);
        chianlaosunID = soundPool.load(this, R.raw.chianlaosun, 1);
        baohushiliID = soundPool.load(this, R.raw.baohushili, 1);
        bajieID = soundPool.load(this, R.raw.bajie, 1);
        yeyeID = soundPool.load(this, R.raw.yeye, 1);


        setContentView(R.layout.activity_wake);
        initView();
        // 初始化
        mVoiceWakeuper = VoiceWakeuper.createWakeuper(this, new InitialListener("VoiceWakeuper"));
        mSpeechRecognizer = SpeechRecognizer.createRecognizer(this, new InitialListener("SpeechRecognizer"));
        // 初始化语法文件
        mCloudGrammar = readFile(this, "WakeCloudGrammar.abnf", "utf-8");
        mLocalGrammar = readFile(this, "WakeLocalGrammar.bnf", "utf-8");
        buildLocalGrammar();
    }

    private void initView() {
        mToast = Toast.makeText(WakeActivity.this, "", Toast.LENGTH_SHORT);
        mWakeReusltTv = (TextView) findViewById(R.id.wake_err_tv);
        mResultTv = (TextView) findViewById(R.id.recog_result_tv);
        mResultTv.setMovementMethod(new ScrollingMovementMethod());
        mWakeReusltTv.setMovementMethod(new ScrollingMovementMethod());
        mRadioGroup = (RadioGroup) findViewById(R.id.wake_radio);
        mSwitch =  ((Switch) findViewById(R.id.wake_start_sw));
        mSwitch.setClickable(false);
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startOnShot();
                } else {
                    cancelOnshot();
                }
            }
        });
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                mSwitch.setClickable(false);
                mSwitch.setChecked(false);
                if (checkedId == R.id.wake_cloud_rb) {
                    mEngineType = SpeechConstant.TYPE_CLOUD;
                    buildCloudGrammar();
                } else {
                    mEngineType = SpeechConstant.TYPE_LOCAL;
                    buildLocalGrammar();
                }
            }
        });
    }

    private void cancelOnshot() {
        showTip("监听取消");
        if (mVoiceWakeuper != null) {
            mVoiceWakeuper.cancel();
        }
    }

    private void startOnShot() {
        mVoiceWakeuper = VoiceWakeuper.getWakeuper();
        if (mVoiceWakeuper != null) {
            resultString = "";
            recoString = "";
            mWakeReusltTv.setText(resultString);

            final String resPath = ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
            // 清空参数
            mVoiceWakeuper.setParameter(SpeechConstant.PARAMS, null);
            // 设置识别引擎
            mVoiceWakeuper.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
            // 设置唤醒资源路径
            mVoiceWakeuper.setParameter(ResourceUtil.IVW_RES_PATH, resPath);
            //持续唤醒
            mVoiceWakeuper.setParameter(SpeechConstant.KEEP_ALIVE, 1 + "");
            /**
             * 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
             * 示例demo默认设置第一个唤醒词，建议开发者根据定制资源中唤醒词个数进行设置
             */
//            String threshold = "0:5;1:5;2:5;3:5";
//            mVoiceWakeuper.setParameter(SpeechConstant.IVW_THRESHOLD, threshold);
            // 设置唤醒+识别模式
            mVoiceWakeuper.setParameter(SpeechConstant.IVW_SST, "oneshot");
            // 设置返回结果格式
            mVoiceWakeuper.setParameter(SpeechConstant.RESULT_TYPE, "json");
//
//				mIvw.setParameter(SpeechConstant.IVW_SHOT_WORD, "0");

            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mVoiceWakeuper.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            mVoiceWakeuper.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");

            if (mEngineType.equals(SpeechConstant.TYPE_CLOUD)) {
                if (!TextUtils.isEmpty(mCloudGrammarID)) {
                    // 设置云端识别使用的语法id
                    mVoiceWakeuper.setParameter(SpeechConstant.CLOUD_GRAMMAR,
                            mCloudGrammarID);
                    mVoiceWakeuper.startListening(mWakeuperListener);
                } else {
                    showTip("请先构建语法");
                }
            } else {
                if (!TextUtils.isEmpty(mLocalGrammarID)) {
                    // 设置本地识别资源
                    mVoiceWakeuper.setParameter(ResourceUtil.ASR_RES_PATH,
                            getResourcePath());
                    // 设置语法构建路径
                    mVoiceWakeuper.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
                    // 设置本地识别使用语法id
                    mVoiceWakeuper.setParameter(SpeechConstant.LOCAL_GRAMMAR,
                            mLocalGrammarID);
                    mVoiceWakeuper.startListening(mWakeuperListener);
                } else {
                    showTip("请先构建语法");
                }
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

    private void showTip(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mToast.setText(s);
                mToast.show();
            }
        });
    }


    /**
     * 云识别初始化
     */
    private void buildCloudGrammar() {
        if (mVoiceWakeuper == null || mSpeechRecognizer == null) {
            return;
        }

        //buildGrammar 仅在使用本地语法文件时 需要使用
        mSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        int res = mSpeechRecognizer.buildGrammar(GRAMMAR_TYPE_ABNF, mCloudGrammar, new GrammarListener() {
            @Override
            public void onBuildFinish(String s, SpeechError speechError) {
                if (speechError == null) {
                    mCloudGrammarID = s;
                    showTip("buildGrammar success ,grammar id = " + s);
                }
            }
        });
        if (res != ErrorCode.SUCCESS) {
            showTip("云端语法构建失败");
            return;
        }
        //设置其他属性
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        mSpeechRecognizer.setParameter(SpeechConstant.ASR_PTT, 0 + "");
        mSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_CLOUD);
        mSwitch.setClickable(true);
    }


    /**
     * 本地识别流程
     */
    void buildLocalGrammar() {
        //buildGrammar 仅在使用本地语法文件时 需要使用
        mSpeechRecognizer.setParameter(SpeechConstant.PARAMS, null);
        // 设置语法构建路径
        mSpeechRecognizer.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置资源路径
        mSpeechRecognizer.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置引擎类型
        mSpeechRecognizer.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //设置其他属性
        mSpeechRecognizer.setParameter(SpeechConstant.ASR_PTT, 0 + "");
        mSpeechRecognizer.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");

        int res = mSpeechRecognizer.buildGrammar(GRAMMAR_TYPE_BNF, mLocalGrammar, new GrammarListener() {
            @Override
            public void onBuildFinish(String s, SpeechError speechError) {
                if (speechError == null) {
                    mLocalGrammarID = s;
                    showTip("本地语法构建成功 ,grammar id = " + s);
                } else {
                    showTip("本地语法构建失败 ,SpeechError id = " + speechError.getErrorDescription() + speechError.getErrorCode());
                }
            }
        });
        if (res != ErrorCode.SUCCESS) {
            showTip("本地语法构建失败");
        }
        mSwitch.setClickable(true);
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

    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
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
//                int type = Integer.valueOf(object.optString("id"));
//                switch (type) {
//                    case 0:
//                        soundPool.play(bajieID, volume, volume, 0, 0, 1);
//                        break;
//                    case 1:
//                        soundPool.play(baohushiliID, volume, volume, 0, 0, 1);
//                        break;
//                    case 2:
//                        soundPool.play(anlaosunID, volume, volume, 0, 0, 1);
//                        break;
//                    case 3:
//                        soundPool.play(yeyeID, volume, volume, 0, 0, 1);
//                        break;
//                    default:
//                }
            } catch (JSONException e) {
                resultString = "结果解析出错";
                e.printStackTrace();
            }
            mWakeReusltTv.setText(resultString);
            recoString = "";
            mResultTv.setText("");
        }

        @Override
        public void onError(SpeechError error) {
            showTip(error.getPlainDescription(true));
            if (error.getErrorCode() == ErrorCode.ERROR_NO_MATCH) {
                if (mVoiceWakeuper != null) {
                    mVoiceWakeuper.startListening(mWakeuperListener);
                }
            }
        }

        @Override
        public void onBeginOfSpeech() {
            showTip("开始监听");
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            Log.d(TAG, "eventType:" + eventType + "arg1:" + isLast + "arg2:" + arg2);
            // 识别结果
            if (SpeechEvent.EVENT_IVW_RESULT == eventType) {
                RecognizerResult reslut = ((RecognizerResult) obj.get(SpeechEvent.KEY_EVENT_IVW_RESULT));
                recoString += JsonParser.parseGrammarResult(reslut.getResultString());
                mResultTv.setText(recoString);
            }
            //继续监听
            if (isLast == 1) {
                if (mVoiceWakeuper != null) {
                    mVoiceWakeuper.startListening(mWakeuperListener);
                }
            }
        }

        @Override
        public void onVolumeChanged(int volume) {
            // TODO Auto-generated method stub
        }
    };
}
