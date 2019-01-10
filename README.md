# shortVideo
简单仿微信短视频拍照  
  启动  
  startActivity（new Intent（this，CameraActivity.class））  
  接收  
      @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {  
            if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {  
        Uri videoUri = intent.getData();  
        Log.e(TAG, "videoUri：" + videoUri);  
        }  
