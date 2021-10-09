package com.im.trtc_call.videolayout;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.liheit.im.utils.Log;

import java.util.ArrayList;

public class Utils {
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 堆叠布局初始化参数：
     * <p>
     * 大画面在最下面，左右两排各三个小画面
     *
     * @param context
     * @param layoutWidth
     * @param layoutHeight
     * @return
     */
    public static ArrayList<RelativeLayout.LayoutParams> initFloatParamList(Context context, int layoutWidth, int layoutHeight) {
        ArrayList<RelativeLayout.LayoutParams> list = new ArrayList<RelativeLayout.LayoutParams>();
        // 底部最大的布局
        RelativeLayout.LayoutParams layoutParams0 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        list.add(layoutParams0);

        final int midMargin = Utils.dip2px(context, 10);
        final int lrMargin = Utils.dip2px(context, 15);
        final int bottomMargin = Utils.dip2px(context, 50);
        final int subWidth = Utils.dip2px(context, 120);
        final int subHeight = Utils.dip2px(context, 180);

        for (int i = 2; i >= 0; i--) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(subWidth, subHeight);
            layoutParams.leftMargin = layoutWidth - lrMargin - subWidth;
            layoutParams.topMargin = layoutHeight - (bottomMargin + midMargin * (i + 1) + subHeight * i) - subHeight;
            list.add(layoutParams);
        }

        for (int i = 2; i >= 0; i--) {
            RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(subWidth, subHeight);
            layoutParams.leftMargin = lrMargin;
            layoutParams.topMargin = layoutHeight - (bottomMargin + midMargin * (i + 1) + subHeight * i) - subHeight;
            list.add(layoutParams);
        }
        return list;
    }

    /**
     * 三宫格布局，品字形
     *
     * @param context
     * @param layoutWidth
     * @param layoutHeight
     * @return
     */
    public static ArrayList<RelativeLayout.LayoutParams> initGrid3Param(Context context, int layoutWidth, int layoutHeight) {
        int margin = dip2px(context, 10);
        int centreMargin = dip2px(context, 5);

        ArrayList<RelativeLayout.LayoutParams> list = new ArrayList<>();
        int grid4W = (layoutWidth - (margin + centreMargin) * 2) / 2;

        RelativeLayout.LayoutParams layoutParams0 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams0.topMargin = margin;
        layoutParams0.leftMargin = margin;
        layoutParams0.rightMargin = centreMargin;

        RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams1.topMargin = margin;
        layoutParams1.rightMargin = margin;
        layoutParams1.leftMargin = centreMargin;

        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams2.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams2.topMargin = margin + grid4W + margin;

        list.add(layoutParams0);
        list.add(layoutParams1);
        list.add(layoutParams2);
        return list;
    }

    /**
     * 四宫格布局参数
     *
     * @param context
     * @param layoutWidth
     * @param layoutHeight
     * @return
     */
    public static ArrayList<RelativeLayout.LayoutParams> initGrid4Param(Context context, int layoutWidth, int layoutHeight) {
        int margin = dip2px(context, 10);
        int centreMargin = dip2px(context, 5);

        ArrayList<RelativeLayout.LayoutParams> list = new ArrayList<>();
        int grid4W = (layoutWidth - (margin + centreMargin) * 2) / 2;
//        int grid4H = (layoutHeight - margin * 2) / 2;
//        Log.e("aaa grid4W="+grid4W+"   grid4H="+grid4H);
//        if (grid4H < grid4W) {
//            grid4W = grid4H;
//        }

        RelativeLayout.LayoutParams layoutParams0 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams0.topMargin = margin;
        layoutParams0.rightMargin = centreMargin;
        layoutParams0.leftMargin = margin;

        RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams1.topMargin = margin;
        layoutParams1.leftMargin = centreMargin;
        layoutParams1.rightMargin = margin;

        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams2.topMargin = margin + grid4W + margin;
        layoutParams2.rightMargin = centreMargin;
        layoutParams2.leftMargin = margin;

        RelativeLayout.LayoutParams layoutParams3 = new RelativeLayout.LayoutParams(grid4W, grid4W);
        layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams3.topMargin = margin + grid4W + margin;
        layoutParams3.leftMargin = centreMargin;
        layoutParams3.rightMargin = margin;

        list.add(layoutParams0);
        list.add(layoutParams1);
        list.add(layoutParams2);
        list.add(layoutParams3);
        return list;
    }

    /**
     * 九宫格布局参数
     *
     * @param context
     * @param layoutWidth
     * @param layoutHeight
     * @return
     */
    public static ArrayList<RelativeLayout.LayoutParams> initGrid9Param(Context context, int layoutWidth, int layoutHeight) {
        int margin = dip2px(context, 10);
        int centreMargin = dip2px(context, 5);

        ArrayList<RelativeLayout.LayoutParams> list = new ArrayList<>();

        int grid9W = (layoutWidth - margin * 2 - centreMargin * 4) / 3;
//        int grid9H = (layoutHeight - margin * 3) / 3;
//        if (grid9H < grid9W) {
//            grid9W = grid9H;
//        }

        RelativeLayout.LayoutParams layoutParams0 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams0.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams0.rightMargin = centreMargin;
        layoutParams0.topMargin = margin;
        layoutParams0.leftMargin = margin;

        RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams1.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams1.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams1.leftMargin = centreMargin;
        layoutParams1.rightMargin = centreMargin;
        layoutParams1.topMargin = margin;

        RelativeLayout.LayoutParams layoutParams2 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams2.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams2.leftMargin = centreMargin;
        layoutParams2.topMargin = margin;
        layoutParams2.rightMargin = margin;

        RelativeLayout.LayoutParams layoutParams3 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams3.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams3.rightMargin = centreMargin;
        layoutParams3.leftMargin = margin;
        layoutParams3.topMargin = margin + grid9W + margin;

        RelativeLayout.LayoutParams layoutParams4 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams4.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams4.leftMargin = centreMargin;
        layoutParams4.rightMargin = centreMargin;
        layoutParams4.topMargin = margin + grid9W + margin;

        RelativeLayout.LayoutParams layoutParams5 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams5.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams5.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams5.leftMargin = centreMargin;
        layoutParams5.topMargin = margin + grid9W + margin;
        layoutParams5.rightMargin = margin;

        RelativeLayout.LayoutParams layoutParams6 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams6.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        layoutParams6.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams6.rightMargin = centreMargin;
        layoutParams6.topMargin = margin + grid9W * 2 + margin * 2;
        layoutParams6.leftMargin = margin;

        RelativeLayout.LayoutParams layoutParams7 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams7.addRule(RelativeLayout.CENTER_HORIZONTAL);
        layoutParams7.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams7.leftMargin = centreMargin;
        layoutParams7.rightMargin = centreMargin;
        layoutParams7.topMargin = margin + grid9W * 2 + margin * 2;

        RelativeLayout.LayoutParams layoutParams8 = new RelativeLayout.LayoutParams(grid9W, grid9W);
        layoutParams8.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        layoutParams8.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        layoutParams8.leftMargin = centreMargin;
        layoutParams8.topMargin = margin + grid9W * 2 + margin * 2;
        layoutParams8.rightMargin = margin;

        list.add(layoutParams0);
        list.add(layoutParams1);
        list.add(layoutParams2);
        list.add(layoutParams3);
        list.add(layoutParams4);
        list.add(layoutParams5);
        list.add(layoutParams6);
        list.add(layoutParams7);
        list.add(layoutParams8);
        return list;
    }

}
