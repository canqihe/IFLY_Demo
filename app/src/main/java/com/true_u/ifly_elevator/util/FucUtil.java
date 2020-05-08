package com.true_u.ifly_elevator.util;

import android.content.Context;

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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

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

}
