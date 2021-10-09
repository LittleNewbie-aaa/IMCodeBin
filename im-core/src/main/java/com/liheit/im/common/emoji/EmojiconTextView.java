package com.liheit.im.common.emoji;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import com.liheit.im.core.IMClient;
import com.liheit.im.core.bean.User;
import com.liheit.im.widget.TextDrawable;
import com.liheit.im_core.R;
import com.qmuiteam.qmui.span.QMUIAlignMiddleImageSpan;
import com.tencent.mars.xlog.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiconTextView extends AppCompatTextView {
    Pattern atPattern = Pattern.compile("@[\u4e00-\u9fa5_a-zA-Z0-9]{1,}\\s");

    private int mEmojiconSize;
    private int mEmojiconTextSize;
    private int mTextStart = 0;
    private int mTextLength = -1;
    private boolean mUseSystemDefault = false;

    public EmojiconTextView(Context context) {
        this(context, null);
    }

    public EmojiconTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmojiconTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        mEmojiconTextSize = (int) getTextSize();
        mEmojiconSize = (int) getTextSize();
        setText(getText());
    }

    public void setTextWithWidth(CharSequence text, int limitedWidth) {
        if (TextUtils.isEmpty(text)) {
            super.setText(text);
            return;
        }
        if (limitedWidth < 0) {
            limitedWidth = this.getMeasuredWidth() - getPaddingRight() - getPaddingLeft();
        }

        SpannableStringBuilder builder = new SpannableStringBuilder(text);
        EmojiconHandler.addEmojis(getContext(), builder, mEmojiconSize, mEmojiconTextSize, mTextStart, mTextLength, mUseSystemDefault);
        CharSequence trucatedText = TextUtils.ellipsize(builder, getPaint(), limitedWidth, getEllipsize());
        super.setText(trucatedText, BufferType.SPANNABLE);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text) && text != null) {
            try{
                Matcher matcher = atPattern.matcher(text);
                while (matcher.find()) {
                    final String atName = matcher.group();
                    String idStr = atName.trim().substring(1);
                    Pattern pNumber = Pattern.compile("[0-9]*");
                    String displayName = "";
                    if (pNumber.matcher(idStr).matches()) {
                        if (Long.parseLong(idStr) == 0l) {
                            displayName = "@全体成员";
                            text = text.toString().replace("@" + idStr, displayName);
                        } else {
                            User user = IMClient.INSTANCE.getUserManager().getUserById(Long.parseLong(idStr));
                            displayName = "@" + user.getCname();
                            text = text.toString().replace("@" + idStr, displayName);
                        }
                    }
                }
            }catch (Exception e){
            }

            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            EmojiconHandler.addEmojis(getContext(), builder, mEmojiconSize, mEmojiconTextSize, mTextStart, mTextLength, mUseSystemDefault);
            text = builder;
        }
        super.setText(text, type);
    }


    /**
     * Set the size of emojicon in pixels.
     */
    public void setEmojiconSize(int pixels) {
        mEmojiconSize = pixels;
        super.setText(getText());
    }

    /**
     * Set whether to use system default emojicon
     */
    public void setUseSystemDefault(boolean useSystemDefault) {
        mUseSystemDefault = useSystemDefault;
    }
}
