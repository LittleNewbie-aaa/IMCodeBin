package com.liheit.im.common.emoji;

import android.content.Context;
import android.graphics.Canvas;
import android.support.v7.widget.AppCompatTextView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class EmojiconTextView2 extends AppCompatTextView {
    private int mEmojiconSize;
    private int mEmojiconTextSize;
    private int mTextStart = 0;
    private int mTextLength = -1;
    private boolean mUseSystemDefault = false;

    public EmojiconTextView2(Context context) {
        this(context, null);
    }

    public EmojiconTextView2(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmojiconTextView2(Context context, AttributeSet attrs, int defStyle) {
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

    long time;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            time = System.currentTimeMillis();
        } else if (event.getAction() == MotionEvent.ACTION_UP)
            if (System.currentTimeMillis() - time > ViewConfiguration.getLongPressTimeout())
                return true;
        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        CharSequence charSequence = getText();
        try{
            int lastCharDown = getLayout().getLineVisibleEnd(getMaxLines() - 1);
//        Log.e("aaa lastCharDown=" + lastCharDown);
            if (charSequence.length() > lastCharDown && lastCharDown!=0) {
                SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
                spannableStringBuilder.append(charSequence.subSequence(0, lastCharDown)).append("...");
                setText(spannableStringBuilder);
            }
        }catch (Exception e){
        }
        super.onDraw(canvas);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            SpannableStringBuilder builder = new SpannableStringBuilder(text);
            EmojiconHandler.addEmojis(getContext(), builder, mEmojiconSize, mEmojiconTextSize, mTextStart, mTextLength, mUseSystemDefault);
            text = builder;
//            Log.e("aaa text.length()=" + text.length());
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
