/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tensorflow;

import android.Manifest;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Camera;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
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
import com.true_u.ifly_elevator.R;
import com.true_u.ifly_elevator.ValueAct;
import com.true_u.ifly_elevator.adapter.FloorAdapter;
import com.true_u.ifly_elevator.bean.FloorBean;
import com.true_u.ifly_elevator.util.FucUtil;
import com.true_u.ifly_elevator.util.HexUtils;
import com.true_u.ifly_elevator.util.ShowUtils;
import com.true_u.ifly_elevator.util.SiriWaveView;
import com.zhouyou.http.EasyHttp;
import com.zhouyou.http.callback.SimpleCallBack;
import com.zhouyou.http.exception.ApiException;

import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import butterknife.BindView;
import butterknife.ButterKnife;
import me.f1reking.serialportlib.listener.ISerialPortDataListener;
import pub.devrel.easypermissions.EasyPermissions;
import pub.devrel.easypermissions.PermissionRequest;
import tensorflow.env.ImageUtils;
import tensorflow.env.Logger;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public abstract class CameraActivity extends ValueAct implements OnImageAvailableListener,
        Camera.PreviewCallback,
        ISerialPortDataListener,
        EasyPermissions.PermissionCallbacks {
    private String TAG = "TiTiGo_Print";
    private static final Logger LOGGER = new Logger();
    protected int previewWidth = 0;
    protected int previewHeight = 0;
    private boolean debug = false;
    private Handler handler;
    private HandlerThread handlerThread;
    private boolean isProcessingFrame = false;
    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private Runnable postInferenceCallback;
    private Runnable imageConverter;
    private List<FloorBean.DataBean> dataBeans;

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

    @BindView(R.id.container)
    FrameLayout frameLayout;

    @BindView(R.id.elevator_layout)
    LinearLayout linearLayout;

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

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(null);
        ShowUtils.NavigationBarStatusBar(this, true);//全屏模式
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);//屏幕常亮
        setContentView(R.layout.tfe_od_activity_camera);
        ButterKnife.bind(this);

//        setFragment(); //设置物体检测

        //权限管理
        if (!EasyPermissions.hasPermissions(this, mPerms))
            EasyPermissions.requestPermissions(new PermissionRequest.Builder(this, 0, mPerms).build());
        else
            initVoiceModel();

        //时间
        timer = new Timer();
        timer.schedule(new RemindTask(), 0, 1000);

    }


    //初始化语音模块
    public void initVoiceModel() {
        newFloor();//初始化楼层
        createVoice();
        setVoiceParam();//设置语音朗读参数
        startVoiceWake(); //保持唤醒监听
        openPort();//打开串口
    }

    //初始化楼层
    public void newFloor() {
        floorAdapter = new FloorAdapter(CameraActivity.this);
        EasyHttp.get("/elevator-web/elevator/showtable/getFloor")
                .baseUrl("http://192.168.0.110:8080")
                .params("plotDetailId", "7")
                .timeStamp(false)
                .execute(new SimpleCallBack<String>() {
                    @Override
                    public void onError(ApiException e) {
                        Log.e("楼层Error", e.toString());
                    }

                    @Override
                    public void onSuccess(String jsonStirng) {
                        FloorBean floorData = com.alibaba.fastjson.JSONObject.parseObject(jsonStirng, FloorBean.class);
                        dataBeans = floorData.getData();
                        if (dataBeans.size() == 0) return;
                        gridView.setAdapter(floorAdapter);
                        floorAdapter.updateData(dataBeans, floorList, 0);
                    }
                });

    }

    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
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
                    floorAdapter.updateData(dataBeans, floorList, 0);
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
            ShowUtils.showToast(CameraActivity.this, error.getPlainDescription(true));
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
            ShowUtils.showToast(CameraActivity.this, "暂停播放");
        }

        @Override
        public void onSpeakResumed() {
            ShowUtils.showToast(CameraActivity.this, "继续播放");
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
                ShowUtils.showToast(CameraActivity.this, error.getPlainDescription(true));
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
            ShowUtils.showToast(CameraActivity.this, "唤醒未初始化");
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
            CameraActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    floorAdapter.updateData(dataBeans, floorList, 1);
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
        Toast.makeText(CameraActivity.this, "权限不足，无法正常使用", Toast.LENGTH_SHORT).show();
        CameraActivity.this.finish();
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

        startActivity(new Intent(this, DetectorActivity.class));

    }

    /**
     * Callback for android.hardware.Camera API
     */
    @Override
    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            LOGGER.w("Dropping frame!");
            return;
        }

        try {
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter = new Runnable() {
            @Override
            public void run() {
                ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
            }
        };

        postInferenceCallback = new Runnable() {
            @Override
            public void run() {
                camera.addCallbackBuffer(bytes);
                isProcessingFrame = false;
            }
        };
        processImage();
    }

    /**
     * Callback for Camera2 API
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) return;

        if (rgbBytes == null) rgbBytes = new int[previewWidth * previewHeight];

        try {
            final Image image = reader.acquireLatestImage();
            if (image == null) return;
            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter = new Runnable() {
                @Override
                public void run() {
                    ImageUtils.convertYUV420ToARGB8888(yuvBytes[0], yuvBytes[1], yuvBytes[2], previewWidth, previewHeight,
                            yRowStride, uvRowStride, uvPixelStride, rgbBytes);
                }
            };

            postInferenceCallback = new Runnable() {
                @Override
                public void run() {
                    image.close();
                    isProcessingFrame = false;
                }
            };
            processImage();
        } catch (final Exception e) {
            LOGGER.e(e, "Exception!");
            Trace.endSection();
            return;
        }
        Trace.endSection();
    }


    @Override
    public synchronized void onResume() {
        LOGGER.d("onResume " + this);
        super.onResume();
        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    @Override
    public synchronized void onPause() {
        LOGGER.d("onPause " + this);
        handlerThread.quitSafely();
        try {
            handlerThread.join();
            handlerThread = null;
            handler = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
        super.onPause();
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    Fragment fragment;

    protected void setFragment() {
        fragment = new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
        getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {
                LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
                yuvBytes[i] = new byte[buffer.capacity()];
            }
            buffer.get(yuvBytes[i]);
        }
    }

    public boolean isDebug() {
        return debug;
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected abstract void processImage();

    protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

    protected abstract int getLayoutId();

    protected abstract Size getDesiredPreviewFrameSize();


}
