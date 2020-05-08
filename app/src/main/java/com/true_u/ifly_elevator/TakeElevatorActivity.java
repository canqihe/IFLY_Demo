package com.true_u.ifly_elevator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.iflytek.cloud.util.ResourceUtil;
import com.true_u.ifly_elevator.adapter.FloorAdapter;
import com.true_u.ifly_elevator.util.ChineseNumToArabicNumUtil;
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.JsonParser;
import com.true_u.ifly_elevator.util.ShowUtils;
import com.true_u.ifly_elevator.util.XmlParser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;

public class TakeElevatorActivity extends AppCompatActivity {

    private final static String TAG = TakeElevatorActivity.class.getSimpleName();
    // 默认本地发音人
    public static String voicerXtts = "xiaoyan";
    // 语音识别对象
    private SpeechRecognizer mVoiceRecognition;
    // 本地语法文件
    private String mLocalGrammar = null;
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    // 返回结果格式，支持：xml,json
    private String mResultType = "json";

    private GridView gridView;
    private EditText conTx;

    private int curThresh = 1450;
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    // 语音唤醒对象
    private VoiceWakeuper mWake;
    // 语音合成对象
    private SpeechSynthesizer mTts;

    private final String GRAMMAR_TYPE_BNF = "bnf";

    private String mEngineType = "local";

    private String mContent;// 语法、词典临时变量
    private int ret = 0;// 函数调用返回值
    private int trust, floorNum;

    private List<Integer> list = new ArrayList<>();

    private FloorAdapter floorAdapter;

    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_take_elevator);
        initLayout();

        // 初始化识别对象
        mVoiceRecognition = SpeechRecognizer.createRecognizer(this, mInitListener);
        mLocalGrammar = FucUtil.readFile(this, "take.bnf", "utf-8");
        //构筑语法
        buildGramer();
        // 初始化唤醒对象
        mWake = VoiceWakeuper.createWakeuper(this, mInitListener);
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);

        setVoiceParam();//设置语音朗读参数
        voiceWake(); //保持唤醒监听

    }

    /**
     * 初始化Layout
     */
    private void initLayout() {
        conTx = findViewById(R.id.isr_text);
        gridView = findViewById(R.id.grid_view);

        floorAdapter = new FloorAdapter(TakeElevatorActivity.this, list, 30);
        gridView.setAdapter(floorAdapter);

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                list.add(position + 1);
                floorAdapter.notifyDataSetChanged();
            }
        });
    }


    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "recognizer result：" + result.getResultString());
                String text = "";
                if (mResultType.equals("json")) {
                    text = JsonParser.parseGrammarResult(result.getResultString(), mEngineType);
                } else if (mResultType.equals("xml")) {
                    text = XmlParser.parseNluResult(result.getResultString());
                } else {
                    text = result.getResultString();
                }

                conTx.setText(text); // 显示

                floorNum = parseJson(result);
                list.add(floorNum);

                floorAdapter.notifyDataSetChanged();

            } else {
                Log.d(TAG, "recognizer result : null");
            }
            startRecognize();//继续识别
        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            Log.d("recognizer result结束说话:", "");
        }

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            Log.d("recognizer result开始说话:", "");
        }

        @Override
        public void onError(SpeechError error) {
            Log.d("recognizer result onError Code：", error.getErrorCode() + "");
            //此时唤醒器已经撤退
            voiceWake();//继续监听唤醒
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
        }
    };


    /***
     *
     * 语音唤醒回调监听
     */
    private WakeuperListener mWakeuperListener = new WakeuperListener() {
        @Override
        public void onResult(WakeuperResult result) {
            Log.d(TAG, "onResult：" + result.getResultString());

            if (mWake.isListening())
                mWake.stopListening();
            int code = mTts.startSpeaking("我在呢", mTtsListener);
            conTx.setText("我在呢");
            if (code != ErrorCode.SUCCESS) {
                ShowUtils.showToast(TakeElevatorActivity.this, "语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }

        @Override
        public void onError(SpeechError error) {
            ShowUtils.showToast(TakeElevatorActivity.this, error.getPlainDescription(true));
        }

        @Override
        public void onBeginOfSpeech() {
        }

        @Override
        public void onEvent(int eventType, int isLast, int arg2, Bundle obj) {
            switch (eventType) {
                // EVENT_RECORD_DATA 事件仅在 NOTIFY_RECORD_DATA 参数值为 真 时返回
                case SpeechEvent.EVENT_RECORD_DATA:
                    final byte[] audio = obj.getByteArray(SpeechEvent.KEY_EVENT_RECORD_DATA);
                    Log.i(TAG, "ivw audio length: " + audio.length);
                    break;
            }
        }

        @Override
        public void onVolumeChanged(int volume) {

        }
    };


    /**
     * 合成回调监听。
     */
    private SynthesizerListener mTtsListener = new SynthesizerListener() {

        @Override
        public void onSpeakBegin() {
            Log.d("开始播放", "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
            ShowUtils.showToast(TakeElevatorActivity.this, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            ShowUtils.showToast(TakeElevatorActivity.this, "继续播放");
        }

        @Override
        public void onBufferProgress(int percent, int beginPos, int endPos, String info) {
            // 合成进度
        }

        @Override
        public void onSpeakProgress(int percent, int beginPos, int endPos) {
            // 播放进度
        }

        @Override
        public void onCompleted(SpeechError error) {
            if (error == null) {
                ShowUtils.showToast(TakeElevatorActivity.this, "播放完成");
                //关闭唤醒监听
                if (mWake.isListening())
                    mWake.stopListening();
                //开始识别命令
                startRecognize();

            } else if (error != null) {
                ShowUtils.showToast(TakeElevatorActivity.this, error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "session id =" + sid);
            }
        }
    };


    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if (error == null) {
                showTip("语法构建成功：" + grammarId);
            } else {
                showTip("语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };


    public void startRecognize() {
        // 设置参数
        if (!setParam()) {
            showTip("请先构建语法。");
            return;
        }
        ret = mVoiceRecognition.startListening(mRecognizerListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("识别失败,错误码: " + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }


    /***
     * 构建语法
     */
    public void buildGramer() {
        mContent = new String(mLocalGrammar);
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置语法构建路径
        mVoiceRecognition.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置资源路径
        mVoiceRecognition.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        ret = mVoiceRecognition.buildGrammar(GRAMMAR_TYPE_BNF, mContent, grammarListener);
        if (ret != ErrorCode.SUCCESS) {
            showTip("语法构建失败,错误码：" + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

    /**
     * 初始化监听器。
     */
    private InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }
    };


    /**
     * 识别参数设置
     *
     * @return
     */
    public boolean setParam() {
        // 清空参数
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.PARAMS, null);
        // 设置识别引擎
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置本地识别资源
        mVoiceRecognition.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置语法构建路径
        mVoiceRecognition.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置返回结果格式
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.RESULT_TYPE, mResultType);
        // 设置本地识别使用语法id
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.LOCAL_GRAMMAR, "take");
        // 设置识别的门限值
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.MIXED_THRESHOLD, "30");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mVoiceRecognition.setParameter(com.iflytek.cloud.SpeechConstant.AUDIO_FORMAT, "wav");
        mVoiceRecognition.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/asr.wav");
        return true;
    }


    /***
     * 唤醒参数
     */
    public void voiceWake() {
        mWake = VoiceWakeuper.getWakeuper();
        if (mWake != null) {
            // 清空参数
            mWake.setParameter(SpeechConstant.PARAMS, null);
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            mWake.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
            // 设置唤醒模式
            mWake.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            mWake.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
            // 设置闭环优化网络模式
            mWake.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
            // 设置唤醒资源路径
            mWake.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mWake.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            mWake.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            mWake.startListening(mWakeuperListener);
        } else {
            ShowUtils.showToast(TakeElevatorActivity.this, "唤醒未初始化");
        }
    }

    /***
     * 语音合成参数
     */
    public void setVoiceParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置使用增强版合成
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_XTTS);
        //设置发音人资源路径
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getMttsResourcePath());
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, voicerXtts);
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }

    //解析json
    public int parseJson(RecognizerResult result) {
        int floor = 0;
        try {
            JSONObject jsonObject = new JSONObject(result.getResultString());
            trust = jsonObject.optInt("sc");//置信度
            JSONArray ws = jsonObject.optJSONArray("ws");
            for (int i = 0; i < ws.length(); i++) {
                JSONObject j = ws.getJSONObject(i);
                if (j.optString("slot").equals("<num>")) {
                    JSONArray jsonArray = j.optJSONArray("cw");
                    JSONObject j1 = (JSONObject) jsonArray.get(0);
                    Log.d(TAG, "recognizer result：楼层" + j1.optString("w") + "楼" + j1.optString("id"));
                    floor = ChineseNumToArabicNumUtil.chineseNumToArabicNum(j1.optString("w"));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("异常recognizer result", e.toString());
        }
        return floor;
    }


    //获取识别资源路径
    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        return tempBuffer.toString();
    }

    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(TakeElevatorActivity.this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
        Log.d(TAG, "resPath: " + resPath);
        return resPath;
    }


    //获取发音人资源路径
    private String getMttsResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "xtts";
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + voicerXtts + ".jet"));

        return tempBuffer.toString();
    }

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(TakeElevatorActivity.this, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.take_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.online:
                startActivity(new Intent(TakeElevatorActivity.this, MainActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mTts != null) mTts.destroy();

        if (mWake != null) mWake.destroy();

        if (mVoiceRecognition != null) {
            mVoiceRecognition.destroy();
            mVoiceRecognition.cancel();
        }

    }

}
