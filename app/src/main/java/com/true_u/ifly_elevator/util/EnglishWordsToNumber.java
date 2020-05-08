package com.true_u.ifly_elevator.util;

import org.apache.commons.lang3.StringUtils;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Colin
 * on 2020/4/27
 * E-mail: hecanqi168@gmail.com
 * Copyright (C) 2018 SSZB, Inc.
 */
public class EnglishWordsToNumber {
    /*
     * 小数点
     */
    private static final String POINT = "point";
    /*
     * 数量级--百。用得比较多，所以单拿出来
     */
    private static final String HUNDRED_MANITUDE = "hundred";
    /*
     * 数量级
     */
    private static final String[] MAGNITUDES = {"billion", "million", "thousand", "hundred"};
    /*
     * 个位数
     */
    private static final String[] DIGITS = {"one", "two", "three", "four", "five", "six", "seven", "eight", "nine"};
    /*
     * 十位数
     */
    private static final String[] TENS = {"twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"};
    /*
     * 只能单独标识的数字[10, 19]
     */
    private static final String[] TEENS = {"ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen"};

    private static final Map<String, Integer> map = new HashMap<>();
    /*
     * 十个阿拉伯数字
     */
    private static final Map<String, String> arabicNumerals = new HashMap<>();

    static {
        map.put("zero", 0);
        // 添加1~9
        for (int i = 0; i < DIGITS.length; i++) {
            map.put(DIGITS[i], i + 1);
        }
        // 添加10~19
        for (int i = 0; i < TEENS.length; i++) {
            map.put(TEENS[i], i + 10);
        }
        // 添加TENS
        for (int i = 0; i < TENS.length; i++) {
            map.put(TENS[i], (i + 2) * 10);
        }
        for (int i = 0; i < TENS.length; i++) {
            for (int j = 0; j < DIGITS.length; j++) {
                map.put(TENS[i] + " " + DIGITS[j], (i + 2) * 10 + (j + 1));
                map.put(TENS[i] + "-" + DIGITS[j], (i + 2) * 10 + (j + 1));
            }
        }
        // "hundred", "thousand", "million", "billion"
        map.put("hundred", 100);
        map.put("thousand", 1000);
        map.put("million", 1000000);
        map.put("billion", 1000000000);

        arabicNumerals.put("zero", "0");
        for (int i = 0; i < DIGITS.length; i++) {
            arabicNumerals.put(DIGITS[i], String.valueOf(i + 1));
        }
    }

    /**
     * 将英文单词解析为一个数字
     *
     * @param input
     * @return 如果无法进行合理化解析，就返回原值；否则返回解析后的值
     */
    public static String parse(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }

        // 拆分为整数部分和小数部分
        String[] parts = input.toLowerCase().replaceAll(" and ", " ").trim().split(POINT);
        // 如果整数部分只有zero
        if (parts[0].trim().equals("zero")) {
            // 判断小数部分的值
            if (parts.length == 1) {// 没有小数部分
                return "0";
            } else if (parts.length == 2) {
                String decimal = parseAfterPoint(parts[1]);
                return decimal == null ? input : "0." + decimal;
            } else {// point出现了超过一次
                return input;
            }
        }

        String decimal = null;
        if (parts.length > 2) {// point出现了超过一次
            return input;
        }
        // 小数部分
        if (parts.length == 2) {
            decimal = parseAfterPoint(parts[1]);
            if (decimal == null) {// 如果有小数部分，但是对小数部分解析后返回null，说明小数部分解析失败
                return input;
            }
        }

        // 整数部分
        String part0 = parts[0].trim();
        BigInteger bi = BigInteger.ZERO;
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < MAGNITUDES.length && part0.length() > 0; i++) {
            Map<String, Object> ret = parseEveryMagnitude(part0, MAGNITUDES[i]);
            if (ret == null) {
                return input;
            }
            bi = bi.add((BigInteger) ret.get("number"));
            part0 = ret.get("part0").toString();
        }

        // 加上整数部分
        result.append(bi.toString());
        // 加上小数部分
        if (decimal != null) {
            result.append('.').append(decimal);
        }
        return result.toString();
    }

    /**
     * 解析小数部分
     *
     * @param str
     * @return 返回null表示小数部分解析失败，返回非null值表示解析成功
     */
    private static String parseAfterPoint(String str) {
        if (StringUtils.isBlank(str)) {
            return "0";
        }

        StringBuilder builder = new StringBuilder();
        String[] arr = StringUtils.split(str);
        for (String s : arr) {
            String num = arabicNumerals.get(s);
            if (num == null) {// 也就是说，小数点后面只能出现0~9的英文单词
                return null;
            }
            builder.append(num);
        }
        return builder.toString();
    }

    /**
     * 解析得到每个数量级前面的数字，然后和数量级相乘，得到该数量级的值
     *
     * @param part0
     * @param magnitude 数量级字符串
     * @return 如果解析不成功，返回null，否则表示解析成功
     */
    private static Map<String, Object> parseEveryMagnitude(String part0, String magnitude) {
        Map<String, Object> ret = new HashMap<>(2);
        if (magnitude.equals(HUNDRED_MANITUDE)) {// 到了hundred这个数量级
            int num = parseHundred(part0);
            if (num == -1) {
                return null;
            }

            ret.put("part0", "");
            ret.put("number", BigInteger.valueOf(num));
            return ret;
        }

        String[] arr = part0.split(magnitude);
        if (arr.length > 2) {// 字符串中包含多个数量级字符串
            return null;
        }

        if (arr.length == 1) {// arr的长度是1，有两种情况：不包含数量级字符串，或者比如one billion
            if (part0.contains(magnitude)) {
                int num = parseHundred(arr[0].trim());
                if (num == -1) {
                    return null;
                }

                int magnitudeNum = map.get(magnitude).intValue();
                ret.put("part0", "");
                ret.put("number", BigInteger.valueOf(num).multiply(BigInteger.valueOf(magnitudeNum)));
                return ret;
            } else {// 不包含数量级字符串，就将part0原样返回
                ret.put("part0", part0);
                ret.put("number", BigInteger.ZERO);
                return ret;
            }
        }

        if (arr.length == 2) {// 字符串中包含数量级字符串，那么只解析数量级字符串前面的数字内容
            int num = parseHundred(arr[0].trim());
            if (num == -1) {
                return null;
            }

            int magnitudeNum = map.get(magnitude).intValue();
            ret.put("part0", arr[1].trim());
            ret.put("number", BigInteger.valueOf(num).multiply(BigInteger.valueOf(magnitudeNum)));
            return ret;
        }

        return null;
    }

    /**
     * 解析每个数量级前面的数字及hundred数量级的数字。根据英语数字表示，该部分的值的范围是[1, 999]。<br>
     *
     * @param hundred
     * @return 如果解析失败，返回-1，否则返回解析后的值
     */
    private static int parseHundred(String hundred) {
        String[] arr = hundred.split(HUNDRED_MANITUDE);
        if (arr.length > 2) {
            return -1;
        }
        if (arr.length == 1) {// arr长度为1，有如下两种情况：one 或者 one hundred
            Integer num = map.get(arr[0].trim());
            if (hundred.contains(HUNDRED_MANITUDE)) {// one hundred
                return (num != null && num.intValue() > 0 && num.intValue() < 10) ? num.intValue() * map.get(HUNDRED_MANITUDE) : -1;
            } else {// one
                return (num != null && num.intValue() > 0 && num.intValue() < 100) ? num.intValue() : -1;
            }
        }
        if (arr.length == 2) {// 包含hundred，那么hundred前面的值的范围就是[1, 9]，hundred后面的值的范围就是[1, 99]
            Integer beforeHundred = map.get(arr[0].trim());
            Integer afterHundred = map.get(arr[1].trim());
            return (beforeHundred != null && beforeHundred.intValue() > 0 && beforeHundred.intValue() < 10 &&
                    afterHundred != null && afterHundred.intValue() > 0 && afterHundred.intValue() < 100)
                    ? beforeHundred.intValue() * map.get(HUNDRED_MANITUDE).intValue() + afterHundred.intValue()
                    : -1;
        }

        return -1;
    }

}
