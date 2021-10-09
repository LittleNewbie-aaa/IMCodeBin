package com.liheit.im.core;

import com.liheit.im.core.bean.Session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by daixun on 2018/7/11.
 */

public interface SessionChangeListener {
    /**
     * 创建群
     * @param session
     */
    void onCreateSession(@NotNull Session session);

    /**
     * 修改群名称
     * @param sid
     * @param title
     */
    void onTitleChange(@NotNull String sid, @NotNull String title);

    /**
     * 有成员加入群
     * @param sid
     * @param adds
     */
    void onMemberJoined(@NotNull String sid, @Nullable List<Long> adds);

    /**
     * 群管理员变化
     * @param sid
     * @param admins
     */
    void onAdminChange(@NotNull String sid, @Nullable List<Long> admins);

    /**
     * 修改群创建者
     * @param sid
     * @param cid
     */
    void onOwnerChanged(@NotNull String sid, long cid);

    /**
     * 有成员退出群
     * @param sid
     * @param operationUser
     * @param dels
     */
    void onMemberExited(@NotNull String sid, long operationUser, @Nullable List<Long> dels);

    /**
     * 群类型变化
     * @param sid
     * @param type
     */
    void onSessionTypeChanged(@NotNull String sid, int type);

    /**
     * 群解散
     * @param sid
     */
    void onSessionRemove(@NotNull String sid);
}
