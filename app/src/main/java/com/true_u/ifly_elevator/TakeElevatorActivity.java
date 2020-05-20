package com.true_u.ifly_elevator;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.GridView;
import android.widget.TextView;
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
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.HexUtils;
import com.true_u.ifly_elevator.util.ShowUtils;
import com.true_u.ifly_elevator.util.SiriWaveView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.f1reking.serialportlib.SerialPortHelper;
import me.f1reking.serialportlib.entity.DATAB;
import me.f1reking.serialportlib.entity.FLOWCON;
import me.f1reking.serialportlib.entity.PARITY;
import me.f1reking.serialportlib.entity.STOPB;
import me.f1reking.serialportlib.listener.IOpenSerialPortListener;
import me.f1reking.serialportlib.listener.ISerialPortDataListener;
import me.f1reking.serialportlib.listener.Status;

public class TakeElevatorActivity extends AppCompatActivity {

    private final static String TAG = TakeElevatorActivity.class.getSimpleName();
    // 默认本地发音人
    public static String voicerXtts = "xiaoyan";
    @BindView(R.id.time)
    TextView time;

    @BindView(R.id.isr_text)
    TextView voiceText;

    @BindView(R.id.grid_view)
    GridView gridView;

    @BindView(R.id.siri_wave_view)
    SiriWaveView siriWaveView;

//    @BindView(R.id.videoView)
//    UniversalVideoView mVideoView;

    // 语音识别对象
    private SpeechRecognizer mVoiceRecognition;
    // 本地语法文件
    private String mLocalGrammar = null;
    // 本地语法构建路径
    private String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    // 返回结果格式，支持：xml,json
    private String mResultType = "json";

    private int curThresh = 1450;
    private String keep_alive = "1";
    private String ivwNetMode = "0";

    private int lowFloor = -5;
    private int highFloor = 30;

    // 语音唤醒对象
    private VoiceWakeuper mWake;
    // 语音合成对象
    private SpeechSynthesizer mTts;

    private final String GRAMMAR_TYPE_BNF = "bnf";

    private String mEngineType = "local";

    private String mContent;// 语法、词典临时变量
    private int ret = 0;// 函数调用返回值
    private int trust, floorNum;
    private Timer timer;
    private List<Integer> list = new ArrayList<>();
    private List<Integer> floorList = new LinkedList<>();
    private FloorAdapter floorAdapter;
    private SerialPortHelper mSerialPortHelper;
    //没有匹配结果次数
    private int recognitionCount;

    private String videoPath;

    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShowUtils.NavigationBarStatusBar(this, true);//全屏模式
        setContentView(R.layout.activity_take_elevator);
        ButterKnife.bind(this);
        //初始化楼层
        newFloor();
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
        openPort();//打开串口
        //时间
        timer = new Timer();
        timer.schedule(new RemindTask(), 0, 1000);

       /*
       //视频播放
       videoPath = "android.resource://" + getPackageName() + "/" + R.raw.trump;
        mVideoView.setVideoPath(videoPath);
        mVideoView.start();

        //重播
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.setVideoPath(videoPath);
                mVideoView.start();
            }
        });*/
    }

    //初始化楼层
    public void newFloor() {
        for (int i = lowFloor; i < 0; i++) {
            list.add(i);
        }
        for (int i = 1; i <= highFloor; i++) {
            list.add(i);
        }
        floorAdapter = new FloorAdapter(TakeElevatorActivity.this, list, floorList);
        gridView.setAdapter(floorAdapter);
    }


    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "打印-当前正在说话，音量大小：" + volume);
            siriWaveView.setVolume(volume);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "打印-JSON" + result.getResultString());
                floorNum = parseJson(result);//解析json
                if (trust > 30) {//置信度大于30
                    sendPortMsg(floorNum); //发送串口数据
                    mTts.startSpeaking(floorNum + "楼", mTtsListener);
                    voiceText.setText(floorNum + "楼");
                    floorList.add(floorNum);
                    floorAdapter.notifyDataSetChanged();
                } else {
                    mTts.startSpeaking("没听清，再说一次", mTtsListener);
                    voiceText.setText("没听清，再说一次");
                }
            } else {
                Log.d(TAG, "打印- null");
            }
        }

        @Override
        public void onEndOfSpeech() {
            Log.d("打印-结束说话。。。", "");
            siriWaveView.stopAnim();
//            voiceText.setBackgroundColor(Color.parseColor("#ff6510"));
        }

        @Override
        public void onBeginOfSpeech() {
            Log.d("打印-开始说话！", "");
        }

        @Override
        public void onError(SpeechError error) {
            Log.d("打印-没有匹配结果", error.getErrorCode() + "");
            recognitionCount++;
            if (recognitionCount < 3) {
                startRecognize();
            } else {
                mTts.startSpeaking("我先离开，稍后回来", null);
                voiceWake();//继续监听唤醒
                recognitionCount = 0;
            }
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
            Log.d(TAG, "打印-onResult：" + result.getResultString());

            if (mWake.isListening())
                mWake.stopListening();

//            int code = mTts.startSpeaking("我在呢", mTtsListener);
//            voiceText.setText("我在呢");

            //开始识别命令
            startRecognize();
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
                Log.d(TAG, "打印-播放完成！");
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
                Log.d(TAG, "打印-session id =" + sid);
            }
        }
    };


    /***
     * 发送串口通信
     * 0xa50xfahgjdshgdho
     * String strHex = Integer.toHexString(valueTen);将其转换为十六进制并输出
     */
    public void sendPortMsg(int floorNum) {
        if (floorNum < 0)
            floorNum = FucUtil.getMinusFloor(floorNum);
        int crc = 0x00;
        if (mSerialPortHelper != null) {
            byte bytes[] = new byte[7];
            bytes[0] = (byte) 0XA5;//head1
            bytes[1] = (byte) 0XFA;//head2
            bytes[2] = (byte) 0X01;//长度
            bytes[3] = (byte) 0X01;//命令
            bytes[4] = (byte) floorNum;//楼层
            crc = bytes[0] + bytes[1] + bytes[2] + bytes[3] + bytes[4];
            crc = crc % 256;
            bytes[5] = (byte) crc;//和校验
            bytes[6] = (byte) 0XFB;//结束

            mSerialPortHelper.sendBytes(bytes);
        }
    }


    /***
     * 打开串口
     */
    public void openPort() {
        mSerialPortHelper = new SerialPortHelper();
        mSerialPortHelper.setPort("/dev/ttyS4");
        mSerialPortHelper.setBaudRate(115200);
        mSerialPortHelper.setStopBits(STOPB.getStopBit(STOPB.B1));
        mSerialPortHelper.setDataBits(DATAB.getDataBit(DATAB.CS8));
        mSerialPortHelper.setParity(PARITY.getParity(PARITY.NONE));
        mSerialPortHelper.setFlowCon(FLOWCON.getFlowCon(FLOWCON.NONE));

        mSerialPortHelper.setIOpenSerialPortListener(new IOpenSerialPortListener() {
            @Override
            public void onSuccess(final File device) {
                Log.d(TAG, "打印-串口打开成功：" + device.getPath());
            }

            @Override
            public void onFail(final File device, final Status status) {
                switch (status) {
                    case NO_READ_WRITE_PERMISSION:
                        Log.d(TAG, "打印-串口没有读写权限：" + device.getPath());
                        break;
                    case OPEN_FAIL:
                    default:
                        Log.d(TAG, "打印-串口打开失败：" + device.getPath());
                        break;
                }
            }
        });


        mSerialPortHelper.setISerialPortDataListener(new ISerialPortDataListener() {
            //接收数据回调
            @Override
            public void onDataReceived(byte[] bytes) {
                Log.d("打印-串口数据", "onDataReceived: " + HexUtils.byteArrToHex(bytes));
                for (int i = 0; i < list.size(); i++) {
                    if (list.get(i) == 0) {
                        list.remove(i);
                    }
                }
                floorAdapter.notifyDataSetChanged();
            }

            //发送数据回调
            @Override
            public void onDataSend(byte[] bytes) {
                Log.d("打印-串口数据", "onDataSend: " + Arrays.toString(bytes));
            }
        });
        Log.d("打印-串口数据", "open: " + mSerialPortHelper.open());

    }


    /***
     * 关闭串口
     */
    private void closePort() {
        if (mSerialPortHelper != null) {
            mSerialPortHelper.close();
        }
    }


    /**
     * 构建语法监听器。
     */
    private GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if (error == null) {
                Log.d(TAG, "打印-语法构建成功！" + grammarId);
            } else {
                Log.d(TAG, "打印-语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };


    public void startRecognize() {
        // 设置参数
        if (!setParam()) {
            showTip("请先构建语法。");
            return;
        }

        siriWaveView.startAnim();
        voiceText.setBackgroundColor(Color.parseColor("#000000"));
        voiceText.setText("请继续\n我在听...");

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
        mVoiceRecognition.setParameter(SpeechConstant.PARAMS, null);
        // 设置文本编码格式
        mVoiceRecognition.setParameter(SpeechConstant.TEXT_ENCODING, "utf-8");
        // 设置引擎类型
        mVoiceRecognition.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
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
        mVoiceRecognition.setParameter(SpeechConstant.PARAMS, null);
        // 设置识别引擎
        mVoiceRecognition.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置本地识别资源
        mVoiceRecognition.setParameter(ResourceUtil.ASR_RES_PATH, getResourcePath());
        // 设置语法构建路径
        mVoiceRecognition.setParameter(ResourceUtil.GRM_BUILD_PATH, grmPath);
        // 设置返回结果格式
        mVoiceRecognition.setParameter(SpeechConstant.RESULT_TYPE, mResultType);
        //语音输入超时时间 设置录取音频的最长时间
        mVoiceRecognition.setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, "200000");
        // 设置本地识别使用语法id
        mVoiceRecognition.setParameter(SpeechConstant.LOCAL_GRAMMAR, "take");
        // 设置识别的门限值
        mVoiceRecognition.setParameter(SpeechConstant.MIXED_THRESHOLD, "30");
        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        mVoiceRecognition.setParameter(SpeechConstant.AUDIO_FORMAT, "wav");
        mVoiceRecognition.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory() + "/msc/asr.wav");
        return true;
    }


    /***
     * 唤醒参数
     */
    public void voiceWake() {
//        mWake = VoiceWakeuper.getWakeuper();
        Log.d(TAG, "打印-进入唤醒状态！ ");

        voiceText.setText(R.string.ttg_desc);
        voiceText.setBackgroundColor(Color.parseColor("#ff6510"));
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
                    Log.d(TAG, "打印-楼层" + j1.optString("w") + "楼" + j1.optString("id"));
                    //拿到楼层
                    floor = FucUtil.getLouceng(j1.optString("w"));
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


    class RemindTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    time.setText(ShowUtils.getTime() + "  " + ShowUtils.getDate() + " " + ShowUtils.dateToWeek(ShowUtils.getDate()));
                }
            });

            //判断更新
            if (ShowUtils.getTime().equals("01:01:01")) {
                Log.d(TAG, "打印-现在要更新啦！");
            }
        }
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
        if (timer != null) timer.cancel();//关闭计时器
        closePort();//关闭串口

        startActivity(new Intent(this, TakeElevatorActivity.class));

    }

}
