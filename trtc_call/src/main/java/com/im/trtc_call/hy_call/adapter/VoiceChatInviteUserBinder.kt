package com.im.trtc_call.hy_call.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.im.trtc_call.R
import com.liheit.im.common.ext.setCircleUserHeader
import com.liheit.im.core.bean.User
import kotlinx.android.synthetic.main.item_voice_chat_invite_user_list.view.*
import me.drakeet.multitype.ItemViewBinder

/**
 * 语音聊天展示人员列表
 */
class VoiceChatInviteUserBinder : ItemViewBinder<User, VoiceChatInviteUserBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_voice_chat_invite_user_list, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, user: User) {
        holder.itemView.ivUserIcon.setCircleUserHeader(user.id)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}