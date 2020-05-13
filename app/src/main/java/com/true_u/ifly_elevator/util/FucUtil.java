package com.true_u.ifly_elevator.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.io.IOException;
import java.io.InputStream;

/**
 * 功能性函数扩展类
 */
public class FucUtil {
    /**
     * 读取asset目录下文件。
     *
     * @return content
     */
    public static String readFile(Context mContext, String file, String code) {
        int len = 0;
        byte[] buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            result = new String(buf, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 读取asset目录下音频文件。
     *
     * @return 二进制文件数据
     */
    public static byte[] readAudioFile(Context context, String filename) {
        try {
            InputStream ins = context.getAssets().open(filename);
            byte[] data = new byte[ins.available()];
            ins.read(data);
            ins.close();
            return data;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    /***
     * 英文楼层
     * @param num
     * @return
     */
    public static int getNum(String num) {
        int floor = 0;
        if (num.equals("First") || num.equals("first"))
            floor = 1;
        else if (num.equals("Second") || num.equals("second"))
            floor = 2;
        else if (num.equals("Third") || num.equals("third") || num.equals("3rd"))
            floor = 3;
        else if (num.equals("Fourth") || num.equals("fourth") || num.equals("4th"))
            floor = 4;
        else if (num.equals("Fifth") || num.equals("fifth") || num.equals("5th"))
            floor = 5;

        return floor;
    }


    /***
     * 拿到楼层
     * @param lou
     * @return
     */
    public static int getLouceng(String lou) {
        int floor = 0;
        //拿到楼层
        if (lou.indexOf("负") != -1) {
            floor = FucUtil.getFloor(lou);
        } else {
            floor = ChineseNumToArabicNumUtil.chineseNumToArabicNum(lou);
        }
        return floor;
    }

    /***
     * 楼层
     * @param lou
     * @return
     */
    public static int getFloor(String lou) {
        int floor = 0;
        if (lou.equals("负一")) floor = -1;
        if (lou.equals("负二")) floor = -2;
        if (lou.equals("负三")) floor = -3;
        if (lou.equals("负四")) floor = -4;
        if (lou.equals("负五")) floor = -5;
        if (lou.equals("负六")) floor = -6;
        return floor;
    }


    /***
     * 负楼层转换
     * @param lou
     * @return
     */
    public static int getMinusFloor(int lou) {
        int floor = 0;
        if (lou == -1) floor = 80;
        if (lou == -2) floor = 81;
        if (lou == -3) floor = 82;
        if (lou == -4) floor = 83;
        if (lou == -5) floor = 84;
        if (lou == -6) floor = 85;
        return floor;
    }

    /**
     * 返回当前程序版本号
     */
    public static int getAppVersionCode(Context context) {
        int versionCode = 0;
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionCode = pi.versionCode;
            if (versionCode == 0) {
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionCode;
    }

    /**
     * 返回当前程序版本名
     */
    public static String getAppVersionName(Context context) {
        String versionName = "";
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), 0);
            versionName = pi.versionName;
            if (versionName == null) {
                return "";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return versionName;
    }


}
