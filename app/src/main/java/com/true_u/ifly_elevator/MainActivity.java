package com.true_u.ifly_elevator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
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
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ResourceUtil;
import com.true_u.ifly_elevator.adapter.FloorAdapter;
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.JsonParser;
import com.true_u.ifly_elevator.util.ShowUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.listen_write)
    Button listenWrite;
    @BindView(R.id.voice_wake)
    Button voiceWake;
    @BindView(R.id.grid_view)
    GridView gridView;
    @BindView(R.id.result_edit)
    EditText mResultText; // 听写结果内容
    @BindView(R.id.upstairs)
    TextView upstairs;
    @BindView(R.id.downstairs)
    TextView downstairs;

    public static String PREFER_NAME = "com.iflytek.setting";
    private static String TAG = "IatDemo";
    // 语音听写对象
    private SpeechRecognizer mVoiceWrite;
    // 语音唤醒对象
    private VoiceWakeuper mIvw;
    // 语音合成对象
    private SpeechSynthesizer mTts;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();
    private SharedPreferences mSharedPreferences;
    private int curThresh = 1450;
    private String threshStr = "门限值：";
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    String[] floors = new String[]{"first", "second", "thrid", "fourth", "fifth", "First", "Second", "Thrid", "Fourth", "Fifth", "3rd", "4th", "5th"};
    String[] floorsAction = new String[]{"upstairs", "downstairs"};

    FloorAdapter floorAdapter;
    private List<Integer> list = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initSDK();
    }

    public void initSDK() {
        // 初始化识别无UI识别对象
        // 使用SpeechRecognizer对象，可根据回调消息自定义界面；
        mVoiceWrite = SpeechRecognizer.createRecognizer(this, mTtsInitListener);
        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(this, mTtsInitListener);
        mSharedPreferences = getSharedPreferences(PREFER_NAME, Activity.MODE_PRIVATE);

        // 初始化唤醒对象
        mIvw = VoiceWakeuper.createWakeuper(this, null);

        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mTtsInitListener);

        if (null == mTts) {
            ShowUtils.showToast(MainActivity.this, "创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
            return;
        }

        setVoiceParam();//设置语音朗读参数
        voiceWake(); //保持唤醒监听

        floorAdapter = new FloorAdapter(MainActivity.this, list, 5);
        gridView.setAdapter(floorAdapter);

    }


    //设置语音朗读参数
    public void setVoiceParam() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        //设置使用云端引擎
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        //设置发音人
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaoyan");
        //设置合成语速
        mTts.setParameter(SpeechConstant.SPEED, mSharedPreferences.getString("speed_preference", "50"));
        //设置合成音调
        mTts.setParameter(SpeechConstant.PITCH, mSharedPreferences.getString("pitch_preference", "50"));
        //设置合成音量
        mTts.setParameter(SpeechConstant.VOLUME, mSharedPreferences.getString("volume_preference", "50"));
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, mSharedPreferences.getString("stream_preference", "3"));
        //	mTts.setParameter(SpeechConstant.STREAM_TYPE, AudioManager.STREAM_MUSIC+"");
        // 设置播放合成音频打断音乐播放，默认为true
        mTts.setParameter(SpeechConstant.KEY_REQUEST_FOCUS, "true");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mTts.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mTts.setParameter(SpeechConstant.TTS_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/tts.wav");
    }


    /**
     * 初始化监听。
     */
    private InitListener mTtsInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            Log.d(TAG, "InitListener init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                ShowUtils.showToast(MainActivity.this, "初始化失败,错误码：" + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");

            }
        }
    };

    @OnClick({R.id.listen_write, R.id.voice_wake})
    public void onViewClicked(View view) {
        switch (view.getId()) {
            //语音听写
            case R.id.listen_write:
                if (mIvw.isListening())
                    mIvw.stopListening();
                listenWirte();
                break;
            //语音唤醒
            case R.id.voice_wake:
                voiceWake();
                voiceWake.setText("开始唤醒");
                break;
        }
    }


    /**
     * 听写监听器。
     */
   /* private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            ShowUtils.showToast(MainActivity.this, "开始说话");
        }

        @Override
        public void onError(SpeechError error) {
            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            ShowUtils.showToast(MainActivity.this, error.getPlainDescription(true));

        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            ShowUtils.showToast(MainActivity.this, "结束说话");
        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d("命令结果NONE", results.getResultString());
            String text = "";
            if (gramerTest == 0) {
                text = JsonParser.parseIatResult(results.getResultString());
            } else if (gramerTest == 1) {
                text = parseGrammarResult(results.getResultString(), "local");
            } else if (mResultType.equals("xml")) {
                text = XmlParser.parseNluResult(results.getResultString());
            } else {
                text = results.getResultString();
            }

            mResultText.append(text);
            mResultText.setSelection(mResultText.length());
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            ShowUtils.showToast(MainActivity.this, "当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据：" + data.length);
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
    };*/


    /**
     * 听写UI监听器
     */
    String floorNum = "0";
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void onResult(RecognizerResult results, boolean isLast) {

            Log.d("命令结果UI", results.getResultString());
            String text = JsonParser.parseIatResult(results.getResultString());

            mResultText.append(text);
            mResultText.setSelection(mResultText.length());

            if (text.indexOf("floor") != -1) {
                for (int i = 0; i < floors.length; i++) {
                    if (text.indexOf(floors[i]) != -1) {
                        floorNum = floors[i];
                    }
                }
            }
            list.add(FucUtil.getNum(floorNum));
            gridView.setAdapter(new FloorAdapter(MainActivity.this, list, 5));
            Log.d("floors:", floorNum);

            if (text.indexOf("upstairs") != -1 || text.indexOf("Upstairs") != -1) {
                upstairs.setBackground(MainActivity.this.getDrawable(R.drawable.radius_floor));
            } else if (text.indexOf("downstairs") != -1 || text.indexOf("Downstairs") != -1) {
                downstairs.setBackground(MainActivity.this.getDrawable(R.drawable.radius_floor));
            }
        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            ShowUtils.showToast(MainActivity.this, error.getPlainDescription(true));
        }
    };

    /***
     * 点击唤醒
     */
    public void voiceWake() {
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            // 清空参数
            mIvw.setParameter(SpeechConstant.PARAMS, null);
            // 唤醒门限值，根据资源携带的唤醒词个数按照“id:门限;id:门限”的格式传入
            mIvw.setParameter(SpeechConstant.IVW_THRESHOLD, "0:" + curThresh);
            // 设置唤醒模式
            mIvw.setParameter(SpeechConstant.IVW_SST, "wakeup");
            // 设置持续进行唤醒
            mIvw.setParameter(SpeechConstant.KEEP_ALIVE, keep_alive);
            // 设置闭环优化网络模式
            mIvw.setParameter(SpeechConstant.IVW_NET_MODE, ivwNetMode);
            // 设置唤醒资源路径
            mIvw.setParameter(SpeechConstant.IVW_RES_PATH, getResource());
            // 设置唤醒录音保存路径，保存最近一分钟的音频
            mIvw.setParameter(SpeechConstant.IVW_AUDIO_PATH, Environment.getExternalStorageDirectory().getPath() + "/msc/ivw.wav");
            mIvw.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
            // 如有需要，设置 NOTIFY_RECORD_DATA 以实时通过 onEvent 返回录音音频流字节
            //mIvw.setParameter( SpeechConstant.NOTIFY_RECORD_DATA, "1" );
            // 启动唤醒
            /*	mIvw.setParameter(SpeechConstant.AUDIO_SOURCE, "-1");*/

            mIvw.startListening(mWakeuperListener);
				/*File file = new File(Environment.getExternalStorageDirectory().getPath() + "/msc/ivw1.wav");
				byte[] byetsFromFile = getByetsFromFile(file);
				mIvw.writeAudio(byetsFromFile,0,byetsFromFile.length);*/
            //	mIvw.stopListening();
        } else {
            ShowUtils.showToast(MainActivity.this, "唤醒未初始化");
        }
    }


    /***
     * 点击听写
     */
    public void listenWirte() {

        if (null == mVoiceWrite) {
            ShowUtils.showToast(MainActivity.this, "创建对象失败，请确认 libmsc.so 放置正确，\n 且有调用 createUtility 进行初始化");
            return;
        }
        mResultText.setText(null);// 清空显示内容
        mIatResults.clear();
        // 设置参数
        setParam();
        boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
        if (isShowDialog) {
            // 显示听写对话框
            mIatDialog.setListener(mRecognizerDialogListener);
            mIatDialog.show();
        }
    }


    /***
     *
     * 语音唤醒回调监听
     */
    private WakeuperListener mWakeuperListener = new WakeuperListener() {

        @Override
        public void onResult(WakeuperResult result) {
            Log.d(TAG, "onResult");
            voiceWake.setText("语音唤醒");
            if (mIvw.isListening())
                mIvw.stopListening();
            int code = mTts.startSpeaking("我是小白，有什么吩咐..", mTtsListener);
            mResultText.setText("我是小白，有什么吩咐..");
            if (code != ErrorCode.SUCCESS) {
                ShowUtils.showToast(MainActivity.this, "语音合成失败,错误码: " + code + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
            }
        }

        @Override
        public void onError(SpeechError error) {
            ShowUtils.showToast(MainActivity.this, error.getPlainDescription(true));
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
            //ShowUtils.showToast(MainActivity.this,"开始播放");
            Log.d(MainActivity.TAG, "开始播放：" + System.currentTimeMillis());
        }

        @Override
        public void onSpeakPaused() {
            ShowUtils.showToast(MainActivity.this, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            ShowUtils.showToast(MainActivity.this, "继续播放");
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
                ShowUtils.showToast(MainActivity.this, "播放完成");
            } else if (error != null) {
                ShowUtils.showToast(MainActivity.this, error.getPlainDescription(true));
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

            //实时音频流输出参考
			/*if (SpeechEvent.EVENT_TTS_BUFFER == eventType) {
				byte[] buf = obj.getByteArray(SpeechEvent.KEY_EVENT_TTS_BUFFER);
				Log.e("MscSpeechLog", "buf is =" + buf);
			}*/
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy WakeDemo");
        // 销毁合成对象
        mIvw = VoiceWakeuper.getWakeuper();
        if (mIvw != null) {
            mIvw.destroy();
        }
    }

    /**
     * 听写参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.PARAMS, null);
        String lag = mSharedPreferences.getString("iat_language_preference", "mandarin");
        // 设置引擎
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.ENGINE_TYPE, "cloud");
        // 设置返回结果格式
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.RESULT_TYPE, "json");

        //mVoiceWrite.setParameter(MscKeys.REQUEST_AUDIO_URL,"true");

        //	this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
        // 设置本地识别资源
        mVoiceWrite.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 在线听写支持多种小语种，若想了解请下载在线听写能力，参看其speechDemo
        // 设置方言
//          mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.ACCENT, null);

        //设置语言
        mVoiceWrite.setParameter(SpeechConstant.LANGUAGE, "en_us");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mVoiceWrite.setParameter(com.iflytek.cloud.SpeechConstant.AUDIO_FORMAT, "wav");
        mVoiceWrite.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/iat.wav");
    }


    private String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(MainActivity.this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
        Log.d(TAG, "resPath: " + resPath);
        return resPath;
    }

    private String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/common.jet"));
        tempBuffer.append(";");
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "iat/sms_16k.jet"));
        //识别8k资源-使用8k的时候请解开注释
        return tempBuffer.toString();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.off_line:
                if (mIvw.isListening())
                    mIvw.stopListening();
                startActivity(new Intent(MainActivity.this, TakeElevatorActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
