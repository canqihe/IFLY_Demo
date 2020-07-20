package com.true_u.ifly_elevator;

import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.GrammarListener;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechRecognizer;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.util.ResourceUtil;
import com.true_u.ifly_elevator.adapter.FloorAdapter;
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.ShowUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Timer;

import androidx.appcompat.app.AppCompatActivity;
import me.f1reking.serialportlib.SerialPortHelper;
import me.f1reking.serialportlib.entity.DATAB;
import me.f1reking.serialportlib.entity.FLOWCON;
import me.f1reking.serialportlib.entity.PARITY;
import me.f1reking.serialportlib.entity.STOPB;
import me.f1reking.serialportlib.listener.IOpenSerialPortListener;
import me.f1reking.serialportlib.listener.Status;

import static com.true_u.ifly_elevator.util.Constant.BAUD_RATE;
import static com.true_u.ifly_elevator.util.Constant.PORT_ADDRESS;

/**
 * Created by Colin
 * on 2020/7/13
 * E-mail: hecanqi168@gmail.com
 */
public class ValueAct extends AppCompatActivity {

    // 默认本地发音人
    public static String voicerXtts = "xiaoyan";
    // 语音识别对象
    public SpeechRecognizer mVoiceRecognition;
    // 本地语法文件
    public String mLocalGrammar = null;
    // 本地语法构建路径
    public String grmPath = Environment.getExternalStorageDirectory()
            .getAbsolutePath() + "/msc/test";
    // 返回结果格式，支持：xml,json
    public String mResultType = "json";

    public int curThresh = 1450;
    public String keep_alive = "1";
    public String ivwNetMode = "0";

    public int lowFloor = -5;
    public int highFloor = 30;
    // 语音唤醒对象
    public VoiceWakeuper mWake;
    // 语音合成对象
    public SpeechSynthesizer mTts;

    public final String GRAMMAR_TYPE_BNF = "bnf";

    public String mEngineType = "local";

    public String mContent;// 语法、词典临时变量
    public int ret = 0;// 函数调用返回值
    public int trust, floorNum;
    public Timer timer;
    public List<Integer> list = new ArrayList<>();
    public List<Integer> floorList = new LinkedList<>();
    public FloorAdapter floorAdapter;
    public SerialPortHelper mSerialPortHelper;
    //没有匹配结果次数
    public int recognitionCount;
    public int trustFailCount;

    //初始化语音模块
    public void createVoice() {
        // 初始化识别对象
        mVoiceRecognition = SpeechRecognizer.createRecognizer(this, mInitListener);
        mLocalGrammar = FucUtil.readFile(this, "take.bnf", "utf-8");
        //构筑语法
        buildGramer();
        // 初始化唤醒对象
        mWake = VoiceWakeuper.createWakeuper(this, mInitListener);
        // 初始化合成对象
        mTts = SpeechSynthesizer.createSynthesizer(this, mInitListener);
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
            Toast.makeText(ValueAct.this, "语法构建失败,错误码：" + ret + ",请点击网址https://www.xfyun.cn/document/error-code查询解决方案", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 初始化监听器。
     */
    public InitListener mInitListener = new InitListener() {
        @Override
        public void onInit(int code) {
            if (code != ErrorCode.SUCCESS) {
                Toast.makeText(ValueAct.this, "\"初始化失败,错误码：\" + code + \",请点击网址https://www.xfyun.cn/document/error-code查询解决方案\"", Toast.LENGTH_SHORT).show();
            }
        }

        PriorityQueue qq = new PriorityQueue();
    };

    /**
     * 构建语法监听器。
     */
    public GrammarListener grammarListener = new GrammarListener() {
        @Override
        public void onBuildFinish(String grammarId, SpeechError error) {
            if (error == null) {
                Log.d("TAG", "打印-语法构建成功！" + grammarId);
            } else {
                Log.d("TAG", "打印-语法构建失败,错误码：" + error.getErrorCode());
            }
        }
    };

    /***
     * 唤醒参数
     */
    public void voiceWakeValue() {
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
        } else {
            ShowUtils.showToast(ValueAct.this, "唤醒未初始化");
        }
    }

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

    //获取识别资源路径
    public String getResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        //识别通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "asr/common.jet"));
        return tempBuffer.toString();
    }

    public String getResource() {
        final String resPath = ResourceUtil.generateResourcePath(ValueAct.this, ResourceUtil.RESOURCE_TYPE.assets, "ivw/" + getString(R.string.app_id) + ".jet");
        return resPath;
    }

    //获取发音人资源路径
    public String getMttsResourcePath() {
        StringBuffer tempBuffer = new StringBuffer();
        String type = "xtts";
        //合成通用资源
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/common.jet"));
        tempBuffer.append(";");
        tempBuffer.append(ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, type + "/" + voicerXtts + ".jet"));

        return tempBuffer.toString();
    }

    /***
     * 打开串口
     */
    public void openPort() {
        mSerialPortHelper = new SerialPortHelper();
        mSerialPortHelper.setPort(PORT_ADDRESS);
        mSerialPortHelper.setBaudRate(BAUD_RATE);
        mSerialPortHelper.setStopBits(STOPB.getStopBit(STOPB.B1));
        mSerialPortHelper.setDataBits(DATAB.getDataBit(DATAB.CS8));
        mSerialPortHelper.setParity(PARITY.getParity(PARITY.NONE));
        mSerialPortHelper.setFlowCon(FLOWCON.getFlowCon(FLOWCON.NONE));

        String[] paths = mSerialPortHelper.getAllDeicesPath();
        for (int i = 0; i < mSerialPortHelper.getAllDeicesPath().length; i++) {
            Log.e("打印-串口列表：", paths[i] + "");
        }

        mSerialPortHelper.setIOpenSerialPortListener(new IOpenSerialPortListener() {
            @Override
            public void onSuccess(final File device) {
                Log.d("TAG", "打印-串口打开成功：" + device.getPath());
            }

            @Override
            public void onFail(final File device, final Status status) {
                switch (status) {
                    case NO_READ_WRITE_PERMISSION:
                        Log.d("TAG", "打印-串口没有读写权限：" + device.getPath());
                        break;
                    case OPEN_FAIL:
                    default:
                        Log.d("TAG", "打印-串口打开失败：" + device.getPath());
                        break;
                }
            }
        });

        Log.d("打印-串口数据", "open: " + mSerialPortHelper.open());
    }


    /***
     * 关闭串口
     */
    public void closePort() {
        if (mSerialPortHelper != null) {
            mSerialPortHelper.close();
        }
    }


}
