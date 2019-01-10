package com.android.camerelibrary;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity {
    private Camera camera;
    private CameraPreview preview;
    private MediaRecorder mMediaRecorder;

    private TextView cameraButton;
    private TextView countDownText;
    private FrameLayout previewLayout;
    private String resultUrl;
    private int resultCode = -1;


    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        cameraButton = findViewById(R.id.camera_button);
        countDownText = findViewById(R.id.count_down);
        previewLayout = findViewById(R.id.camera_preview);
        cameraButton.setOnTouchListener(new TouchListener());
        initCamera();
    }


    /**
     * 初始化摄像头
     */
    private void initCamera() {
        // Create an instance of Camera
        camera = getCameraInstance();
        //旋转90°
        camera.setDisplayOrientation(90);
        Camera.Parameters parameters = camera.getParameters();
        //对焦模式
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

        List<Camera.Size> SupportedPreviewSizes = parameters.getSupportedPreviewSizes();// 获取支持预览照片的尺寸
        Camera.Size cameraSize = SupportedPreviewSizes.get(0).width > SupportedPreviewSizes.get(SupportedPreviewSizes.size() - 1).width ? SupportedPreviewSizes.get(0) : SupportedPreviewSizes.get(SupportedPreviewSizes.size() - 1);
        Camera.Size previewSize = cameraSize;// 从List取出Size
        parameters.setPreviewSize(previewSize.width, previewSize.height);// 设置预览高宽
        parameters.setPictureSize(previewSize.width, previewSize.height);// 设置图片高宽
        camera.setParameters(parameters);
        // Create our Preview view and set it as the content of our activity.
        preview = new CameraPreview(this, camera);
        previewLayout.addView(preview);
    }

    /**
     * 相机拍照监听
     */
    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try {
                File pictureFile = CameraTool.getOutputMediaFile(CameraTool.MEDIA_TYPE_IMAGE);
                if (pictureFile == null) {
                    return;
                }
                resultUrl = pictureFile.getPath();
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                CameraTool.compressImageByQuality(pictureFile.getPath());
                closeCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * A safe way to get an instance of the Camera object.
     */
    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private boolean prepareVideoRecorder() {
        try {
            mMediaRecorder = new MediaRecorder();

            // Step 1: Unlock and set camera to MediaRecorder
            camera.unlock();
            mMediaRecorder.setCamera(camera);

            // Step 2: Set sources  需要开启音频摄像头权限
            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
            mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_QCIF));

            resultUrl = CameraTool.getOutputMediaFile(CameraTool.MEDIA_TYPE_VIDEO).toString();
            // Step 4: Set output file
            mMediaRecorder.setOutputFile(resultUrl);

            // Step 5: Set the preview output
            mMediaRecorder.setPreviewDisplay(preview.getHolder().getSurface());

            // Step 6: Prepare configured MediaRecorder
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

    private void closeCamera() {
        Intent intent = new Intent();
        intent.putExtra(CameraTool.RESULT_URL, resultUrl);
        setResult(resultCode, intent);
        finish();
    }

    /**
     * 按钮的监听以及处理
     */

    class TouchListener implements View.OnTouchListener {
        private int countDown = 12;
        private boolean isRecording = false;

        Timer timer = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        countDown--;
                        if (countDown == 10) {
                            if (!isRecording) {
                                if (prepareVideoRecorder()) {
                                    // Camera is available and unlocked, MediaRecorder is prepared,
                                    // now you can start recording
                                    mMediaRecorder.start();
                                    countDownText.setVisibility(View.VISIBLE);
                                    isRecording = true;
                                } else {
                                    // prepare didn't work, release the camera
                                    releaseMediaRecorder();
                                }
                            }
                        } else if (countDown < 10 && countDown > 0) {
                            countDownText.setText("录像中..." + countDown);
                        } else if (countDown <= 0) {
                            stopCamera();
                        }

                    }
                });
            }
        };
        private final String TAG = "---TouchListener";

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    timer.schedule(task, 0, 1000);
                    cameraButton.setBackground(getResources().getDrawable(R.drawable.bg_camare_button_down));
                    break;
                case MotionEvent.ACTION_UP:
                    timer.cancel();
                    Log.e(TAG, "onTouch" + isRecording);
                    cameraButton.setBackground(getResources().getDrawable(R.drawable.bg_camare_button_up));
                    if (isRecording) {
                        stopCamera();
                        resultCode = CameraTool.MEDIA_TYPE_VIDEO;
                    } else {
                        camera.takePicture(null, null, mPicture);
                        resultCode = CameraTool.MEDIA_TYPE_IMAGE;
                    }
                    isRecording = false;
                    break;
            }
            return true;
        }

        private void stopCamera() {
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder
            timer.cancel();
            countDownText.setVisibility(View.INVISIBLE);
            closeCamera();
        }
    }
}
