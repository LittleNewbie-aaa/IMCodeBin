package com.liheit.im.core;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by daixun on 2018/8/2.
 */

public class Constants {

    public static final int SessionTitleMax = 128;// 会话标题最大长度
    public static final int SessionDiscMemberMax = 50;// 讨论组最大成员数
    public static final int SessionFixMemberMax = 2000;// 固定群最大成员数
    public static final int SessionGroupMemberMax = 500;// 普通群最大成员数
    public static final int MsgMax = 10000;// 消息内容最大长度
    public static final int UserNameMax = 128;// 用户名字最大长度
    public static final int UserEmailMax = 128;// 用户邮箱最大长度
    public static final int UserPhoneMax = 128;// 用户手机号最大长度
    public static final int UserTelMax = 128;// 用户座机号最大长度
    public static final int UserSignMax = 128;// 用户签名最大长度
    public static final int UserJobMax = 128;// 用户职务最大长度
    public static final Long FILE_HELP_ID=99L;//文件传输助手id
    public static final Long HY_CUSTOMER_SERVICE_ID=11244L;//文件客服id
    public static final Long SD_CUSTOMER_SERVICE_ID=99L;//实地文件客服id

    public static final Long MOSTOFTEN=19999L;//展示常用聊天群组的虚拟id，跟服务器无关
    public static final Long FIXSESSION=19998L;//展示固定群组群组的虚拟id，跟服务器无关


    public static final int MAX_SQL_PARAM_SIZE = 970;//sqlite 限制每个sql参数不能大于999 设置批量插入最大的大小

    public static Map<String, String> emojiDescs = new HashMap<String, String>();

    static {
        emojiDescs.put("wx", "微笑");
        emojiDescs.put("pz", "撇嘴");
        emojiDescs.put("se", "色");
        emojiDescs.put("fd", "发呆");
        emojiDescs.put("dy", "得意");
        emojiDescs.put("ll", "流泪");
        emojiDescs.put("hx", "害羞");
        emojiDescs.put("bz", "闭嘴");
        emojiDescs.put("shui", "睡");
        emojiDescs.put("dk", "大哭");
        emojiDescs.put("gg", "尴尬");
        emojiDescs.put("fn", "发怒");
        emojiDescs.put("tp", "调皮");
        emojiDescs.put("cy", "呲牙");
        emojiDescs.put("jy", "惊讶");
        emojiDescs.put("ng", "难过");
        emojiDescs.put("ku", "酷");
        emojiDescs.put("jiong", "囧");
        emojiDescs.put("zk", "抓狂");
        emojiDescs.put("tu", "吐");
        emojiDescs.put("tx", "偷笑");
        emojiDescs.put("yk", "愉快");
        emojiDescs.put("by", "白眼");
        emojiDescs.put("am", "傲慢");
        emojiDescs.put("je", "饥饿");
        emojiDescs.put("kun", "困");
        emojiDescs.put("jk", "惊恐");
        emojiDescs.put("lh", "流汗");
        emojiDescs.put("hxiao", "憨笑");
        emojiDescs.put("yx", "悠闲");
        emojiDescs.put("fdou", "奋斗");
        emojiDescs.put("zm", "咒骂");
        emojiDescs.put("yw", "疑问");
        emojiDescs.put("xu", "嘘");
        emojiDescs.put("yun", "晕");
        emojiDescs.put("fl", "疯了");
        emojiDescs.put("shuai", "衰");
        emojiDescs.put("kl", "骷髅");
        emojiDescs.put("qd", "敲打");
        emojiDescs.put("zj", "再见");
        emojiDescs.put("ch", "擦汗");
        emojiDescs.put("kb", "抠鼻");
        emojiDescs.put("gz", "鼓掌");
        emojiDescs.put("qdl", "糗大了");
        emojiDescs.put("huaix", "坏笑");
        emojiDescs.put("zhh", "左哼哼");
        emojiDescs.put("yhh", "右哼哼");
        emojiDescs.put("hq", "哈欠");
        emojiDescs.put("bs", "鄙视");
        emojiDescs.put("wq", "委屈");
        emojiDescs.put("kk", "快哭");
        emojiDescs.put("yxian", "阴险");
        emojiDescs.put("qq", "亲亲");
        emojiDescs.put("he", "吓");
        emojiDescs.put("klian", "可怜");
        emojiDescs.put("cd", "菜刀");
        emojiDescs.put("xg", "西瓜");
        emojiDescs.put("pj", "啤酒");
        emojiDescs.put("lq", "篮球");
        emojiDescs.put("pp", "乒乓");
        emojiDescs.put("kf", "咖啡");
        emojiDescs.put("fan", "饭");
        emojiDescs.put("zt", "猪头");
        emojiDescs.put("mg", "玫瑰");
        emojiDescs.put("dx", "凋谢");
        emojiDescs.put("zc", "嘴唇");
        emojiDescs.put("ax", "爱心");
        emojiDescs.put("xs", "心碎");
        emojiDescs.put("dg", "蛋糕");
        emojiDescs.put("sd", "闪电");
        emojiDescs.put("zd", "炸弹");
        emojiDescs.put("dao", "刀");
        emojiDescs.put("zq", "足球");
        emojiDescs.put("pc", "瓢虫");
        emojiDescs.put("bb", "便便");
        emojiDescs.put("yl", "月亮");
        emojiDescs.put("ty", "太阳");
        emojiDescs.put("lw", "礼物");
        emojiDescs.put("yb", "拥抱");
        emojiDescs.put("qiang", "强");
        emojiDescs.put("ruo", "弱");
        emojiDescs.put("ws", "握手");
        emojiDescs.put("sl", "胜利");
        emojiDescs.put("bq", "抱拳");
        emojiDescs.put("gy", "勾引");
        emojiDescs.put("qt", "拳头");
        emojiDescs.put("cj", "差劲");
        emojiDescs.put("an", "爱你");
        emojiDescs.put("no", "NO");
        emojiDescs.put("ok", "OK");
        emojiDescs.put("jx", "奸笑");
        emojiDescs.put("hh", "嘿哈");
        emojiDescs.put("wl", "捂脸");
        emojiDescs.put("jz", "机智");
        emojiDescs.put("cha", "茶");
        emojiDescs.put("hb", "红包");
        emojiDescs.put("lz", "蜡烛");
    }
}
