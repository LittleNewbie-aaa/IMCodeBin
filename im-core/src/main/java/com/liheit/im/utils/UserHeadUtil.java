package com.liheit.im.utils;

import com.liheit.im.common.ext.Ext;
import com.liheit.im.widget.TextDrawable;
import com.liheit.im_core.R;

/**
 * Created by daixun on 2018/7/7.
 */

public class UserHeadUtil {
    private static ColorGenerator generator = ColorGenerator.MATERIAL;
    private static int[] colors=new int[]{
            0xff4ba9e9, 0xff16c194, 0xfff1735d, 0xfff6b45d, 0xffb28977, 0xff558aac, 0xff9aa1d2};

    public static TextDrawable genDefaultHead(String name) {
        int color2 = generator.getColor(name);
        return TextDrawable.builder()
                .beginConfig()
                .withBorder(1)
                .fontSize(Ext.ctx.getResources().getDimensionPixelOffset(R.dimen.icon_font_size))
                .endConfig()
                .buildRoundRect(name.substring(Math.max(name.length() - 2, 0), name.length()), color2, 0);
    }

    public static int getColor(int value) {
        return colors[(int) (value % colors.length)];
    }

    public static TextDrawable genDefaultHead(String name,Long userId) {
        int color2 = colors[(int) (userId % colors.length)];
        return TextDrawable.builder()
                .beginConfig()
                .withBorder(1)
                .fontSize(Ext.ctx.getResources().getDimensionPixelOffset(R.dimen.icon_font_size))
                .endConfig()
                .buildRoundRect(name.substring(Math.max(name.length() - 2, 0), name.length()), color2, 0);
    }
}
