package com.liheit.im.utils;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Created by daixun on 2018/7/10.
 */

public class IDUtil {

    /**
     * 单聊会话 ID 的生成规则：{XXXXXXXX-0000-0000-0000-0000YYYYYYYY}
     * XXXXXXXX：为单聊参与人，ID 较小的用户 ID 的十六进制数值
     * YYYYYYYY：为单聊参与人，ID 较大的用户 ID 的十六进制数值
     */

    /**
     * 单聊SID：uid1#uid2
     * 示例：DBBA3#DBBA4
     * 部门群SID：#did
     * 示例：#ABDE1
     * 格式化时，也不再要填充0前缀。
     * 其它的不变。
     *
     * @param meId
     * @param objId
     * @return
     */

    public static String createSingleChatId(long meId, long objId) {
        long left = Math.min(meId, objId);
        long right = Math.max(meId, objId);
        StringBuilder sb = new StringBuilder();
        sb.append(idToHex(left));
        sb.append("#");
        sb.append(idToHex(right));
        return sb.toString();
    }

    /**
     * 解析通过单聊sessionId解析出对方id
     *
     * @param myId
     * @param sessionId
     * @return
     */
    public static Long parseTargetId(long myId, String sessionId) {
        //TODO 校验session格式
        String[] ids = sessionId.split("#");
        String idString1 = ids[0];
        long parseId1 = new BigInteger(idString1, 16).longValue();
        if (parseId1 != 0 && parseId1 != myId) {
            return parseId1;
        }

        String idString2 = ids[1];
        long parseId2 = new BigInteger(idString2, 16).longValue();
        if (parseId2 != 0 && parseId2 != myId) {
            return parseId2;
        }
        return -1L;
    }

    public static String generatorMsgId() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    private static String generatorUUID() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    public static String generatorSessionId() {
        return generatorUUID();
    }


    private static String idToHex(long id) {
        String hexId = Long.toHexString(id).toUpperCase();
        return hexId;
        /*if (hexId.length() == 8) {
            return hexId;
        } else {
            int size = 8 - hexId.length();
            StringBuilder idStr = new StringBuilder();
            while (size-- > 0) {
                idStr.append("0");
            }
            idStr.append(hexId);
            return idStr.toString();
        }*/
    }
}
