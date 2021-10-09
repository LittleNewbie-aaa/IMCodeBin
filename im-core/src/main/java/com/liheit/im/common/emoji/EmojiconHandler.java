package com.liheit.im.common.emoji;

import android.content.Context;
import android.support.v4.util.ArrayMap;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.util.SparseIntArray;

import com.qmuiteam.qmui.util.QMUIDisplayHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@SuppressWarnings({"JavaDoc", "UnusedReturnValue", "unused", "WeakerAccess"})
public final class EmojiconHandler {
    public static final List<QQFace> mQQFaceList = new ArrayList<>();
    public static final Map<String, String> emojiDescs = new HashMap<String, String>();
    private static final HashMap<String, Integer> sQQFaceMap = new HashMap<>();
    private static final SparseIntArray sEmojisMap = new SparseIntArray(846);
    private static final SparseIntArray sSoftbanksMap = new SparseIntArray(471);
    private static final ArrayMap<String, String> mQQFaceFileNameList = new ArrayMap<>();//存储QQ表情对应的文件名,方便混淆后可以获取到原文件名
    /**
     * 表情的放大倍数
     */
    private static final float EMOJIICON_SCALE = 1f;
    /**
     * 表情的偏移值
     */
    private static final int EMOJIICON_TRANSLATE_Y = 0;
    private static final int QQFACE_TRANSLATE_Y = QMUIDisplayHelper.dpToPx(1);

    public static String getDesc(String key) {
        return emojiDescs.get(key);
    }

    private EmojiconHandler() {
    }

    private static boolean isSoftBankEmoji(char c) {
        return ((c >> 12) == 0xe);
    }

    private static int getEmojiResource(int codePoint) {
        return sEmojisMap.get(codePoint);
    }

    private static int getSoftbankEmojiResource(char c) {
        return sSoftbanksMap.get(c);
    }

    /**
     * @param context   Convert emoji characters of the given Spannable to the according emojicon.
     * @param text
     * @param emojiSize
     */
    public static void addEmojis(Context context, SpannableStringBuilder text, int emojiSize, int textSize) {
        addEmojis(context, text, emojiSize, textSize, 0, -1, false);
    }

    /**
     * Convert emoji characters of the given Spannable to the according emojicon.
     *
     * @param context
     * @param text
     * @param emojiSize
     * @param index
     * @param length
     */
    public static void addEmojis(Context context, SpannableStringBuilder text, int emojiSize, int textSize, int index, int length) {
        addEmojis(context, text, emojiSize, textSize, index, length, false);
    }

    /**
     * Convert emoji characters of the given Spannable to the according emojicon.
     *
     * @param context
     * @param text
     * @param emojiSize
     * @param useSystemDefault
     */
    public static void addEmojis(Context context, SpannableStringBuilder text, int emojiSize, int textSize, boolean useSystemDefault) {
        addEmojis(context, text, emojiSize, textSize, 0, -1, useSystemDefault);
    }

    /**
     * Convert emoji characters of the given Spannable to the according emojicon.
     *
     * @param context
     * @param text
     * @param emojiSize
     * @param index
     * @param length
     * @param useSystemDefault
     */
    public static SpannableStringBuilder addEmojis(Context context, SpannableStringBuilder text, int emojiSize, int textSize, int index, int length, boolean useSystemDefault) {
        if (useSystemDefault) {
            return text;
        }

        int textLengthToProcess = calculateLegalTextLength(text, index, length);

        // remove spans throughout all text
        EmojiconSpan[] oldSpans = text.getSpans(0, text.length(), EmojiconSpan.class);
        for (EmojiconSpan oldSpan : oldSpans) {
            text.removeSpan(oldSpan);
        }

        int[] results = new int[3];
        String textStr = text.toString();
        int processIdx = index;
        int emojiNum = 0;
        while (processIdx < textLengthToProcess) {
            boolean isEmoji = findEmoji(textStr, processIdx, textLengthToProcess, results);
            int skip = results[1];
            if (isEmoji) {
                if (emojiNum < 150) {
                    int icon = results[0];
                    boolean isQQFace = results[2] > 0;
                    EmojiconSpan span = new EmojiconSpan(context, icon, (int) (emojiSize * EMOJIICON_SCALE),
                            (int) (emojiSize * EMOJIICON_SCALE));
                    span.setTranslateY(isQQFace ? QQFACE_TRANSLATE_Y : EMOJIICON_TRANSLATE_Y);
                    if (span.getCachedDrawable() == null) {
//                        text.replace(processIdx, processIdx + skip, "..");
//                        //重新计算字符串的合法长度
//                        textLengthToProcess = calculateLegalTextLength(text, index, length);
                    } else {
                        emojiNum++;
                        text.setSpan(span, processIdx, processIdx + skip, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
            processIdx += skip;
        }
        return (SpannableStringBuilder) text.subSequence(index, processIdx);
    }

    public static void addEmojis(Context context, Editable text, int emojiSize, int textSize, int start, int lengthBefore, int lengthAfter) {
        // remove spans throughout all text
//        EmojiconSpan[] oldSpans = text.getSpans(0, text.length(), EmojiconSpan.class);
//        for (EmojiconSpan oldSpan : oldSpans) {
//            text.removeSpan(oldSpan);
//        }

        int[] results = new int[3];
        String textStr = text.toString();
//        String textStr = text.toString().substring(lengthBefore,text.length());
//        Log.e("aaa textStr=" + textStr);
        int emojiNum = 0;
        int processIdx = 0;
        int textLengthToProcess = text.length();
//        int textLengthToProcess = lengthAfter-lengthBefore;
        if (start == 0) {
            while (processIdx < textLengthToProcess && emojiNum < 101) {
                boolean isEmoji = findEmoji(textStr, processIdx, textLengthToProcess, results);
                int skip = results[1];
                if (isEmoji) {
//                    Log.e("aaa emojiNum=" + emojiNum);
                    int icon = results[0];
                    boolean isQQFace = results[2] > 0;
                    EmojiconSpan span = new EmojiconSpan(context, icon, (int) (emojiSize * EMOJIICON_SCALE),
                            (int) (emojiSize * EMOJIICON_SCALE));
                    span.setTranslateY(isQQFace ? QQFACE_TRANSLATE_Y : EMOJIICON_TRANSLATE_Y);
                    if (span.getCachedDrawable() == null) {
                    } else {
                        emojiNum++;
                        text.setSpan(span, processIdx, processIdx + skip, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                processIdx += skip;
//                Log.e("aaa processIdx=" + processIdx + "///skip=" + skip);
            }
        }
    }

    /**
     * 判断文本位于start的字节是否为emoji。
     *
     * @param text
     * @param start
     * @param end
     * @param result 长度为3的数据。当第一位表示emoji的资源id，
     *               第二位表示emoji在原文本占位长度，
     *               第三位表示emoji类型是否位qq表情。
     * @return 如果是emoji，返回True。
     */
    public static boolean findEmoji(String text, int start, int end, int[] result) {
        int skip = 0;
        int icon = 0;
        char c = text.charAt(start);
        if (isSoftBankEmoji(c)) {
            icon = getSoftbankEmojiResource(c);
            skip = icon == 0 ? 0 : 1;
        }

        if (icon == 0) {
            int unicode = Character.codePointAt(text, start);
            skip = Character.charCount(unicode);

            if (unicode > 0xff) {
                icon = getEmojiResource(unicode);
            }

            if (icon == 0 && start + skip < end) {
                int followUnicode = Character.codePointAt(text, start + skip);
                if (followUnicode == 0x20e3) {
                    int followSkip = Character.charCount(followUnicode);
                    switch (unicode) {
                        /*case 0x0031:
                            icon = R.drawable.emoji_0031;
                            break;
                        case 0x0032:
                            icon = R.drawable.emoji_0032;
                            break;
                        case 0x0033:
                            icon = R.drawable.emoji_0033;
                            break;
                        case 0x0034:
                            icon = R.drawable.emoji_0034;
                            break;
                        case 0x0035:
                            icon = R.drawable.emoji_0035;
                            break;
                        case 0x0036:
                            icon = R.drawable.emoji_0036;
                            break;
                        case 0x0037:
                            icon = R.drawable.emoji_0037;
                            break;
                        case 0x0038:
                            icon = R.drawable.emoji_0038;
                            break;
                        case 0x0039:
                            icon = R.drawable.emoji_0039;
                            break;
                        case 0x0030:
                            icon = R.drawable.emoji_0030;
                            break;
                        case 0x0023:
                            icon = R.drawable.emoji_0023;
                            break;*/
                        default:
                            followSkip = 0;
                            break;
                    }
                    skip += followSkip;
                } else {
                    int followSkip = Character.charCount(followUnicode);
                    switch (unicode) {
                        /*case 0x1f1ef:
                            icon = (followUnicode == 0x1f1f5) ? R.drawable.emoji_1f1ef_1f1f5 : 0;
                            break;
                        case 0x1f1fa:
                            icon = (followUnicode == 0x1f1f8) ? R.drawable.emoji_1f1fa_1f1f8 : 0;
                            break;
                        case 0x1f1eb:
                            icon = (followUnicode == 0x1f1f7) ? R.drawable.emoji_1f1eb_1f1f7 : 0;
                            break;
                        case 0x1f1e9:
                            icon = (followUnicode == 0x1f1ea) ? R.drawable.emoji_1f1e9_1f1ea : 0;
                            break;
                        case 0x1f1ee:
                            icon = (followUnicode == 0x1f1f9) ? R.drawable.emoji_1f1ee_1f1f9 : 0;
                            break;
                        case 0x1f1ec:
                            icon = (followUnicode == 0x1f1e7) ? R.drawable.emoji_1f1ec_1f1e7 : 0;
                            break;
                        case 0x1f1ea:
                            icon = (followUnicode == 0x1f1f8) ? R.drawable.emoji_1f1ea_1f1f8 : 0;
                            break;
                        case 0x1f1f7:
                            icon = (followUnicode == 0x1f1fa) ? R.drawable.emoji_1f1f7_1f1fa : 0;
                            break;
                        case 0x1f1e8:
                            icon = (followUnicode == 0x1f1f3) ? R.drawable.emoji_1f1e8_1f1f3 : 0;
                            break;
                        case 0x1f1f0:
                            icon = (followUnicode == 0x1f1f7) ? R.drawable.emoji_1f1f0_1f1f7 : 0;
                            break;*/
                        default:
                            followSkip = 0;
                            break;
                    }
                    skip += followSkip;
                }
            }
        }

        boolean isQQFace = false;
        if (icon == 0) {
            if (c == '[') {
                int emojiCloseIndex = text.indexOf(']', start);
                if (emojiCloseIndex > 0 && emojiCloseIndex - start <= 6) {
                    CharSequence charSequence = text.subSequence(start, emojiCloseIndex + 1);
                    Integer value = sQQFaceMap.get(charSequence.toString());

                    if (value != null) {
                        icon = value;
                        skip = emojiCloseIndex + 1 - start;
                        isQQFace = true;
                    }
                }
            }
        }

        result[0] = icon;
        result[1] = skip;
        result[2] = isQQFace ? 1 : 0;

        return icon > 0;
    }

    public static String findQQFaceFileName(String key) {
        return mQQFaceFileNameList.get(key);
    }

    private static int calculateLegalTextLength(SpannableStringBuilder text, int index, int length) {
        int textLength = text.length();
        int textLengthToProcessMax = textLength - index;
        return (length < 0 || length >= textLengthToProcessMax ? textLength : (length + index));
    }

    public static List<QQFace> getQQFaceKeyList() {
        return mQQFaceList;
    }

    public static boolean isQQFaceCodeExist(String qqFaceCode) {
        return sQQFaceMap.get(qqFaceCode) != null;
    }

    public static void setup(List<QQFace> emojis) {
        for (QQFace face : emojis) {
            mQQFaceList.add(face);
            sQQFaceMap.put(face.name, face.res);
            emojiDescs.put(face.name, face.getDesc());
        }
    }

    public static class QQFace {
        private String name;
        private int res;

        public QQFace(String name, int res, String desc) {
            this.name = name;
            this.res = res;
            this.desc = desc;
        }

        private String desc;

        public QQFace(String name, int res) {
            this.name = name;
            this.res = res;
        }

        public String getDesc() {
            return desc;
        }

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getName() {
            return name;
        }

        public int getRes() {
            return res;
        }
    }
}
