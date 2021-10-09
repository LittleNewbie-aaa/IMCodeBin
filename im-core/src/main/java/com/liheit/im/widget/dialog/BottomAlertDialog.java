package com.liheit.im.widget.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.liheit.im_core.R;

/**
 * 在底部弹出的Dialog
 */
public class BottomAlertDialog {
    private Context context;
    private Dialog dialog;
    private Display display;
    private LinearLayout lLayout_bg;
    private TextView tvItem1, tvItem2, tvNeg;

    public BottomAlertDialog(Context context) {
        this.context = context;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public BottomAlertDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(
                R.layout.dialog_content_circle, null);

        // 获取自定义Dialog布局中的控件
        lLayout_bg = (LinearLayout) view.findViewById(R.id.lLayout_bg);
        tvItem1 = (TextView) view.findViewById(R.id.tvItem1);
        tvItem2 = (TextView) view.findViewById(R.id.tvItem2);
        tvNeg = (TextView) view.findViewById(R.id.tvNeg);

        // 定义Dialog布局和参数
        dialog = new Dialog(context, R.style.AlertDialogStyle);
        dialog.setContentView(view);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        params.width = context.getResources().getDisplayMetrics().widthPixels;
        // 调整dialog背景大小
        lLayout_bg.setLayoutParams(params);
        dialog.getWindow().setGravity(Gravity.BOTTOM);
        return this;
    }

    public BottomAlertDialog setItem(String item1, String item2) {
        tvItem1.setText(item1);
        tvItem2.setText(item2);
        return this;
    }

    public BottomAlertDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }


    public BottomAlertDialog setNegativeButton(String text) {
        tvNeg.setText(text);
        return this;
    }

    public BottomAlertDialog setItemListener(final ItemOnClickListener listener) {
        tvItem1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(tvItem1.getText().toString());
                dialog.dismiss();
            }
        });
        tvItem2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(tvItem2.getText().toString());
                dialog.dismiss();
            }
        });
        tvNeg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(tvNeg.getText().toString());
                dialog.dismiss();
            }
        });
        return this;
    }

    public void show() {
        dialog.show();
    }

    public interface ItemOnClickListener {
        void onClick(String text);
    }
}
