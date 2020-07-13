package com.true_u.ifly_elevator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechEvent;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.VoiceWakeuper;
import com.iflytek.cloud.WakeuperListener;
import com.iflytek.cloud.WakeuperResult;
import com.true_u.ifly_elevator.adapter.FloorAdapter;
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.HexUtils;
import com.true_u.ifly_elevator.util.ShowUtils;
import com.true_u.ifly_elevator.util.SiriWaveView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.f1reking.serialportlib.listener.ISerialPortDataListener;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;

public class TakeElevatorActivity extends ValueAct implements EasyPermissions.PermissionCallbacks, ISerialPortDataListener {

    private final static String TAG = TakeElevatorActivity.class.getSimpleName();
    @BindView(R.id.time)
    TextView time;

    @BindView(R.id.isr_text)
    TextView voiceText;

    @BindView(R.id.floor_num_text)
    TextView floorNumText;

    @BindView(R.id.grid_view)
    GridView gridView;

    @BindView(R.id.siri_wave_view)
    SiriWaveView siriWaveView;

//    @BindView(R.id.videoView)
//    UniversalVideoView mVideoView;

    private String videoPath;

    private String[] mPerms = {Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_PHONE_STATE};


    @SuppressLint("ShowToast")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ShowUtils.NavigationBarStatusBar(this, true);//全屏模式
        setContentView(R.layout.activity_take_elevator);
        ButterKnife.bind(this);

        //初始化楼层
        newFloor();

        //权限管理
        if (!EasyPermissions.hasPermissions(this, mPerms))
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, 0, mPerms).build());
        else
            initVoiceModel();

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

    //初始化语音模块
    public void initVoiceModel() {
        createVoice();
        setVoiceParam();//设置语音朗读参数
        startVoiceWake(); //保持唤醒监听
        openPort();//打开串口
    }

    //初始化楼层
    public void newFloor() {
        for (int i = lowFloor; i < 0; i++) {
            list.add(i);
        }
        for (int i = 1; i <= highFloor; i++) {
            list.add(i);
        }
        floorAdapter = new FloorAdapter(TakeElevatorActivity.this);
        gridView.setAdapter(floorAdapter);
        floorAdapter.updateData(list, floorList, 0);
    }

    /**
     * 识别监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            Log.d(TAG, "当前正在说话，音量大小：" + volume);
            siriWaveView.setVolume(volume);
        }

        @Override
        public void onResult(final RecognizerResult result, boolean isLast) {
            if (null != result && !TextUtils.isEmpty(result.getResultString())) {
                Log.d(TAG, "JSON解析-" + result.getResultString());
                floorNum = parseJson(result);//解析json
                if (trust > 30) {//置信度大于30
                    trustFailCount = 0;
                    sendPortMsg(floorNum); //发送串口数据
                    mTts.startSpeaking(floorNum + "楼", mTtsListener);
                    voiceText.setText(floorNum + "楼");
                    voiceText.setVisibility(View.INVISIBLE);

                    floorNumText.setVisibility(View.VISIBLE);
                    floorNumText.setText(floorNum + "楼");
                    floorList.add(floorNum);
                    floorAdapter.updateData(list, floorList, 0);
                    floorAdapter.notifyDataSetChanged();
                } else {
                    floorNumText.setVisibility(View.INVISIBLE);
                    trustFailCount++;
                    if (trustFailCount <= 3) {
                        voiceText.setVisibility(View.VISIBLE);
                        mTts.startSpeaking("没听清。", mTtsListener);
                        voiceText.setText("没听清。");
                    } else {
                        trustFailCount = 0;
                        mTts.startSpeaking("我先离开，稍后回来", null);
                        startVoiceWake();//继续监听唤醒
                    }
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
                recognitionCount = 0;
                mTts.startSpeaking("我先离开，稍后回来", null);
                startVoiceWake();//继续监听唤醒
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
                //开始识别命令
                startRecognize();
            } else if (error != null) {
                ShowUtils.showToast(TakeElevatorActivity.this, error.getPlainDescription(true));
            }
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            if (SpeechEvent.EVENT_SESSION_ID == eventType) {
                String sid = obj.getString(SpeechEvent.KEY_EVENT_AUDIO_URL);
                Log.d(TAG, "打印-session id =" + sid);
            }
        }
    };


    /***
     * 开启唤醒
     */
    public void startVoiceWake() {
        mWake = VoiceWakeuper.getWakeuper();
        Log.d(TAG, "打印-进入唤醒状态！ ");

        floorNumText.setVisibility(View.INVISIBLE);
        voiceText.setVisibility(View.VISIBLE);
        voiceText.setText(R.string.ttg_desc);
        voiceText.setBackgroundColor(Color.parseColor("#ff6510"));
        if (mWake != null) {
            voiceWakeValue();
            mWake.startListening(mWakeuperListener);
        } else {
            ShowUtils.showToast(TakeElevatorActivity.this, "唤醒未初始化");
        }
    }


    /***
     * 开启识别
     */
    public void startRecognize() {
        //关闭唤醒监听
        if (mWake.isListening()) mWake.stopListening();
        // 设置参数
        if (!setParam()) {
            voiceText.setText("请先构建语法。");
            return;
        }

        floorNumText.setVisibility(View.INVISIBLE);
        siriWaveView.startAnim();
        voiceText.setBackgroundColor(Color.parseColor("#000000"));
        voiceText.setText("请继续\n我在听...");
        voiceText.setVisibility(View.VISIBLE);

        ret = mVoiceRecognition.startListening(mRecognizerListener);

        if (ret != ErrorCode.SUCCESS) {
            voiceText.setText("识别失败,错误码: \" + ret + \",请点击网址https://www.xfyun.cn/document/error-code查询解决方案");
        }
    }

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

    @Override
    public void onDataReceived(byte[] bytes) {
        Log.d("打印-串口数据", "接收回调: " + HexUtils.byteArrToHex(bytes));
        for (int i = 0; i < bytes.length; i++) Log.d("打印-bytes", i + "：" + bytes[i]);
        if (bytes[0] == -91) {
            floorList.clear();
            floorList = HexUtils.getDataNum(bytes);
            for (int i = 0; i < floorList.size(); i++) {
                Log.e("打印-接收的串口数据：", "下标-" + floorList.get(i));
            }
            TakeElevatorActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    floorAdapter.updateData(list, floorList, 1);
                    floorAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onDataSend(byte[] bytes) {
        Log.d("打印-串口数据", "发送回调: " + Arrays.toString(bytes));
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

    //权限回调
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        createVoice();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        Toast.makeText(TakeElevatorActivity.this, "权限不足，无法正常使用", Toast.LENGTH_SHORT).show();
        TakeElevatorActivity.this.finish();
    }

    class RemindTask extends TimerTask {
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    time.setText(ShowUtils.getTime() + " " + ShowUtils.dateToWeek(ShowUtils.getDate()));
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
