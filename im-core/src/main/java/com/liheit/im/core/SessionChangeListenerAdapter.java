package com.liheit.im.core;

import com.liheit.im.core.bean.Session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Created by daixun on 2018/7/11.
 */

public abstract class SessionChangeListenerAdapter implements SessionChangeListener {
    @Override
    public void onCreateSession(@NotNull Session session) {

    }

    @Override
    public void onTitleChange(@NotNull String sid, @NotNull String title) {

    }

    @Override
    public void onMemberJoined(@NotNull String sid, @Nullable List<Long> adds) {

    }

    @Override
    public void onAdminChange(@NotNull String sid, @Nullable List<Long> admins) {

    }

    @Override
    public void onOwnerChanged(@NotNull String sid, long cid) {

    }

    @Override
    public void onMemberExited(@NotNull String sid, long operationUser, @Nullable List<Long> dels) {

    }

    @Override
    public void onSessionTypeChanged(@NotNull String sid, int type) {

    }

    @Override
    public void onSessionRemove(@NotNull String sid) {

    }
}
