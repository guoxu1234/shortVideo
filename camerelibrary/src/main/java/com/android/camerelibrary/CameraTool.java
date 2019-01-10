package com.android.camerelibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Gx on 2018/5/4.
 * Explain
 */
public class CameraTool {

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final String RESULT_URL = "resultUrl";

    private static final int QUALITY = 200 * 1024;
    private static final int PIXEL = 1200;

    /**
     * Create a File for saving an image or video
     */
    public static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStorageDirectory() + File.separator + "bossien");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }


    /**
     * 按比例缩小图片的像素以达到压缩的目的
     *
     * @param imgPath
     */
    public static void compressImageByPixel(String imgPath) throws FileNotFoundException {
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;//只读边,不读内容
        BitmapFactory.decodeFile(imgPath, newOpts);
        newOpts.inJustDecodeBounds = false;
        int width = newOpts.outWidth;
        int height = newOpts.outHeight;
        int be = 1;
        if (width >= height && width > PIXEL) {//缩放比,用高或者宽其中较大的一个数据进行计算
            be = (int) (newOpts.outWidth / PIXEL);
            be++;
        } else if (width < height && height > PIXEL) {
            be = (int) (newOpts.outHeight / PIXEL);
            be++;
        }
        newOpts.inSampleSize = be;//设置采样率
        newOpts.inPreferredConfig = Bitmap.Config.ARGB_8888;//该模式是默认的,可不设
        newOpts.inPurgeable = true;// 同时设置才会有效
        newOpts.inInputShareable = true;//。当系统内存不够时候图片自动被回收
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath, newOpts);
        compressImageByQuality(bitmap, imgPath);//压缩好比例大小后再进行质量压缩
/*        if (isEnableQualityCompress()) {
            compressImageByQuality(bitmap, imgPath, listener);//压缩好比例大小后再进行质量压缩
        } else {
            File thumbnailFile = getThumbnailFile(new File(imgPath));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(thumbnailFile));

            listener.onCompressSuccess(thumbnailFile.getPath());
        }*/
    }

    /**
     * 多线程压缩图片的质量
     *
     * @param bitmap  内存中的图片
     * @param imgPath 图片的保存路径
     * @author JPH
     * @date 2014-12-5下午11:30:43
     */
    public static void compressImageByQuality(final Bitmap bitmap, final String imgPath) {
        new Thread(new Runnable() {//开启多线程进行压缩处理
            @Override
            public void run() {
                // TODO Auto-generated method stub
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int options = 100;
                bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);//质量压缩方法，把压缩后的数据存放到baos中 (100表示不压缩，0表示压缩到最小)
                while (baos.toByteArray().length > QUALITY) {//循环判断如果压缩后图片是否大于指定大小,大于继续压缩
                    baos.reset();//重置baos即让下一次的写入覆盖之前的内容
                    options -= 5;//图片质量每次减少5
                    if (options <= 5) {
                        options = 5;//如果图片质量小于5，为保证压缩后的图片质量，图片最底压缩质量为5
                    }
                    bitmap.compress(Bitmap.CompressFormat.JPEG, options, baos);//将压缩后的图片保存到baos中
                    if (options == 5) {
                        break;//如果图片的质量已降到最低则，不再进行压缩
                    }
                }
                try {
                    File thumbnailFile = new File(imgPath);
                    FileOutputStream fos = new FileOutputStream(thumbnailFile);//将压缩后的图片保存的本地上指定路径中
                    fos.write(baos.toByteArray());
                    fos.flush();
                    fos.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void compressImageByQuality(final String imgPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(imgPath);
        compressImageByQuality(bitmap, imgPath);
    }


    public static void start(){
        
    }
}
