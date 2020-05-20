package com.true_u.ifly_elevator.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Colin
 * on 2020/5/11
 * E-mail: hecanqi168@gmail.com
 */
public class HexUtils {
    private static final char HexCharArr[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static final String HexStr = "0123456789abcdef";


    /***
     * 字节转16进制
     * @param btArr
     * @return
     */
    public static String byteArrToHex(byte[] btArr) {
        char strArr[] = new char[btArr.length * 2];
        int i = 0;
        for (byte bt : btArr) {
            strArr[i++] = HexCharArr[bt >>> 4 & 0xf];
            strArr[i++] = HexCharArr[bt & 0xf];
        }
        return new String(strArr);
    }


    /***
     * 16进制转字节
     * @param hexStr
     * @return
     */
    public static byte[] hexToByteArr(String hexStr) {
        char[] charArr = hexStr.toCharArray();
        byte btArr[] = new byte[charArr.length / 2];
        int index = 0;
        for (int i = 0; i < charArr.length; i++) {
            int highBit = HexStr.indexOf(charArr[i]);
            int lowBit = HexStr.indexOf(charArr[++i]);
            btArr[index] = (byte) (highBit << 4 | lowBit);
            index++;
        }
        return btArr;
    }


    /**
     * 字符串转化成为16进制字符串
     *
     * @param s
     * @return
     */
    public static String strTo16(String s) {
        String str = "";
        for (int i = 0; i < s.length(); i++) {
            int ch = (int) s.charAt(i);
            String s4 = Integer.toHexString(ch);
            str = str + s4;
        }
        return str;
    }

    public static String makeChecksum(String data) {
        if (data == null || data.equals("")) {
            return "";
        }
        int total = 0;
        int len = data.length();
        int num = 0;
        while (num < len) {
            String s = data.substring(num, num + 2);
            System.out.println(s);
            total += Integer.parseInt(s, 16);
            num = num + 2;
        }
        /**
         * 用256求余最大是255，即16进制的FF
         */
        int mod = total % 256;
        String hex = Integer.toHexString(mod);
        len = hex.length();
        // 如果不够校验位的长度，补0,这里用的是两位校验
        if (len < 2) {
            hex = "0" + hex;
        }
        return hex;
    }

    public static List<Integer> getDataNum( byte[] bytes) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < bytes[2] - 1; j++) {
                if (bytes[3 + bytes[2] - j] % 2 == 1) list.add(i + 1 + j * 8);
                bytes[3 + bytes[2] - j] = (byte) (bytes[3 + bytes[2] - j] >> 1);
            }
        }
        return list;
    }


   /* public static List<Integer> getDataNum(int highFloor, byte[] bytes) {
        List<Integer> list = new ArrayList<>();

        highFloor = (bytes[2] - 1) * 8;

        for (int i = 0; i < 8; i++) {
            if (highFloor > 0) {
                if (bytes[3 + bytes[2]] % 2 == 1) list.add(i + 1);
                bytes[10] = (byte) (bytes[10] >> 1);
//                bytes[10] = (byte) (bytes[10] / 2);
            }
            if (highFloor > 8) {
                if (bytes[9] % 2 == 1) list.add(i + 1);
                bytes[9] = (byte) (bytes[9] >> 1);
            }
            if (highFloor > 16) {
                if (bytes[8] % 2 == 1) list.add(i + 1);
                bytes[8] = (byte) (bytes[8] >> 1);
            }
            if (highFloor > 24) {
                if (bytes[7] % 2 == 1) list.add(i + 1);
                bytes[7] = (byte) (bytes[7] >> 1);
            }
            if (highFloor > 32) {
                if (bytes[6] % 2 == 1) list.add(i + 1);
                bytes[6] = (byte) (bytes[6] >> 1);
            }
            if (highFloor > 40) {
                if (bytes[5] % 2 == 1) list.add(i + 1);
                bytes[5] = (byte) (bytes[5] >> 1);
            }

        }
        return list;

    }*/

}
