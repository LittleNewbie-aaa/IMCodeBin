package com.liheit.im.core.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 聊天房间变化通知数据
 */
public class VoiceChatRoomChangeMsg {
    private String sid; //聊天会话id
    private int roomid;//聊天房间id
    private Long userid;//
    private Long inviterid;//邀请人id
    private int status;//0退出  1加入 2拒绝
    private List<Long> ids = new ArrayList<>();//聊天房间当前用户id集合
    private String dismissed;
    private int term;//终端类型

    public String getSid() {
        return sid;
    }

    public void setSid(String sid) {
        this.sid = sid;
    }

    public int getRoomid() {
        return roomid;
    }

    public void setRoomid(int roomid) {
        this.roomid = roomid;
    }

    public Long getUserid() {
        return userid;
    }

    public void setUserid(Long userid) {
        this.userid = userid;
    }

    public int getStatus() { return status; }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids;
    }

    public String getDismissed() {
        return dismissed;
    }

    public void setDismissed(String dismissed) {
        this.dismissed = dismissed;
    }

    public int getTerm() { return term; }

    public void setTerm(int term) {
        this.term = term;
    }

    public Long getInviterid() {
        return inviterid;
    }

    public void setInviterid(Long inviterid) {
        this.inviterid = inviterid;
    }
}
