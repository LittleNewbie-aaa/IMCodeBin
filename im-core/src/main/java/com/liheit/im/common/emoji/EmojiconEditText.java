package com.liheit.im.common.emoji;

/**
 * Created by daixun on 2018/7/4.
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.support.v7.widget.AppCompatEditText;
import android.text.Editable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import com.liheit.im.core.IMClient;
import com.liheit.im.core.bean.User;
import com.liheit.im.widget.TextDrawable;
import com.liheit.im_core.R;
import com.qmuiteam.qmui.span.QMUIAlignMiddleImageSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Hieu Rocker (rockerhieu@gmail.com).
 */
public class EmojiconEditText extends AppCompatEditText {
    private OnKeyListener mKeyListener;

    Pattern atPattern = Pattern.compile("@[\u4e00-\u9fa5_a-zA-Z0-9]{1,}\\s");
    Pattern refPattern = Pattern.compile("^\\[refs:(.*)\\[(.*)\\]\\]");
    private int mEmojiconSize;
    private int mEmojiconAlignment;
    private int mEmojiconTextSize;
    private boolean mUseSystemDefault = false;
    private String editable;

    public EmojiconEditText(Context context) {
        super(context);
        mEmojiconSize = (int) getTextSize();
        mEmojiconTextSize = (int) getTextSize();
    }

    public EmojiconEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs);
    }

    public EmojiconEditText(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs);
    }

    private void init(AttributeSet attrs) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.Emojicon);
        mEmojiconSize = (int) a.getDimension(R.styleable.Emojicon_emojiconSize, getTextSize()) + 10;
        mEmojiconAlignment = a.getInt(R.styleable.Emojicon_emojiconAlignment, DynamicDrawableSpan.ALIGN_BASELINE);
        mUseSystemDefault = a.getBoolean(R.styleable.Emojicon_emojiconUseSystemDefault, false);
        a.recycle();
        mEmojiconTextSize = (int) getTextSize();
        setText(getText());
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
//        Log.e("aaa text="+text.toString()+"//start="+start
//                +"//lengthBefore="+lengthBefore
//                +"//lengthAfter="+lengthAfter);
        updateText(text, start, lengthBefore, lengthAfter);
    }

    /*@Override
    public void setText(CharSequence text, BufferType type) {
        if (!TextUtils.isEmpty(text)) {
            EmojiconHandler.addEmojis(getContext(), getText(), mEmojiconSize, mEmojiconTextSize);

            String textStr = getText;
            Matcher matcher = atPattern.matcher(textStr);
            while (matcher.find()) {
                final String atName = matcher.group();
                String idStr = atName.trim().substring(1);
                User user = IMClient.INSTANCE.getUserManager().getUserById(Long.parseLong(idStr));
                String displayName = "@" + user.getCname();
                final float width = this.getPaint().measureText(displayName);

                TextDrawable drawable = TextDrawable.builder().beginConfig()
                        .fontSize(mEmojiconSize)
                        .height(mEmojiconSize)
                        .width((int) width)
                        .textColor(getContext().getResources().getColor(R.color.colorLink))
                        .endConfig()
                        .buildRect(displayName, Color.TRANSPARENT);
                drawable.setBounds(0, 0, ((int) width), mEmojiconSize);

                builder.setSpan(new QMUIAlignMiddleImageSpan(drawable, QMUIAlignMiddleImageSpan.ALIGN_MIDDLE), matcher.start(), matcher.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            super.setText(builder, BufferType.SPANNABLE);
        } else {
            super.setText(text, type);
        }
    }*/

    /**
     * Set the size of emojicon in pixels.
     */
    public void setEmojiconSize(int pixels) {
        mEmojiconSize = pixels;
//        updateText();
    }

    public boolean haveRefs = false;
    public int refsLength = 0;

    private void updateText(CharSequence content, int start, int lengthBefore, int lengthAfter) {
        Editable text = getText();
        if (text != null && !TextUtils.isEmpty(text)) {
            Editable editable = getText();
            EmojiconHandler.addEmojis(getContext(), editable, mEmojiconSize, mEmojiconTextSize, start, lengthBefore, start + lengthAfter);

            String textStr = editable.toString();
            Matcher matcher = atPattern.matcher(textStr);
            while (matcher.find()) {
                final String atName = matcher.group();
                String idStr = atName.trim().substring(1);
                Pattern pNumber = Pattern.compile("[0-9]*");
                String displayName = "";
                if (pNumber.matcher(idStr).matches()) {
                    if (Long.parseLong(idStr) == 0l) {
                        displayName = "@全体成员";
                    } else {
                        User user = IMClient.INSTANCE.getUserManager().getUserById(Long.parseLong(idStr));
                        displayName = "@" + user.getCname();
                    }
                }

                final float width = this.getPaint().measureText(displayName);

                TextDrawable drawable = TextDrawable.builder().beginConfig()
                        .fontSize(mEmojiconSize - 10)
                        .height(mEmojiconSize)
                        .width((int) width)
                        .textColor(getContext().getResources().getColor(R.color.colorLink))
                        .endConfig()
                        .buildRect(displayName, Color.TRANSPARENT);
                drawable.setBounds(0, 0, ((int) width), mEmojiconSize);

                editable.setSpan(new QMUIAlignMiddleImageSpan(drawable, QMUIAlignMiddleImageSpan.ALIGN_MIDDLE),
                        matcher.start(), matcher.end(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
    }

    @Override
    protected void onSelectionChanged(int selStart, int selEnd) {
        super.onSelectionChanged(selStart, selEnd);
        if (listener != null) {
            listener.onSelectionChanged(selStart, selEnd);
        }
    }

    private OnSelectionChangedListener listener;

    public void setOnSelectionChangedListener(OnSelectionChangedListener listener) {
        this.listener = listener;
    }

    public interface OnSelectionChangedListener {
        public void onSelectionChanged(int selStart, int selEnd);
    }

    /**
     * Set whether to use system default emojicon
     */
    public void setUseSystemDefault(boolean useSystemDefault) {
        mUseSystemDefault = useSystemDefault;
    }

    @Override
    public void setOnKeyListener(OnKeyListener l) {
        mKeyListener = l;
        super.setOnKeyListener(l);
    }

    /**
     * 处理Google自带键盘和华为部分手机键盘的del删除键无响应
     * @param outAttrs
     * @return
     */
    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        return new InnerInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    private class InnerInputConnection extends InputConnectionWrapper {
        public InnerInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean deleteSurroundingText(int beforeLength, int afterLength) {
            boolean ret = false;
            if (beforeLength == 1 && afterLength == 0 && mKeyListener != null) {
                ret = mKeyListener.onKey(EmojiconEditText.this, KeyEvent.KEYCODE_DEL, new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
            }
            return ret || super.deleteSurroundingText(beforeLength, afterLength);
        }
    }

}