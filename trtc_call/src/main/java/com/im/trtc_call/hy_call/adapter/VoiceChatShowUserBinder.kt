package com.im.trtc_call.hy_call.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.im.trtc_call.R
import com.liheit.im.common.ext.setCircleUserHeader
import com.liheit.im.core.bean.User
import kotlinx.android.synthetic.main.item_voice_chat_user_list.view.*
import me.drakeet.multitype.ItemViewBinder

/**
 * 语音聊天展示人员列表
 */
class VoiceChatShowUserBinder : ItemViewBinder<User, VoiceChatShowUserBinder.ViewHolder>() {

    override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
        val root = inflater.inflate(R.layout.item_voice_chat_user_list, parent, false)
        return ViewHolder(root)
    }

    override fun onBindViewHolder(holder: ViewHolder, user: User) {
        holder.itemView.tvUserName.text = user.cname
        holder.itemView.ivUserIcon.setCircleUserHeader(user.id)
        holder.itemView.tvUserState.text = when (user.voiceChatStates) {
            0 -> "等待接听"
            1 -> "通话中"
            2 -> "已退出"
            else -> "等待接听"
        }
        if (user.voiceChatStates == 1) {
//            holder.itemView.ivVolumes.visibility = View.VISIBLE
            when {
                user.isMute -> {
                    holder.itemView.ivVolumes.setImageResource(R.drawable.ic_mute)
                    return
                }
                user.volumes > 0 -> {
                    when {
                        user.volumes <= 33 -> {
                            holder.itemView.ivVolumes.setImageResource(R.drawable.ic_volume1)
                        }
                        user.volumes in 34..65 -> {
                            holder.itemView.ivVolumes.setImageResource(R.drawable.ic_volume2)
                        }
                        else -> {
                            holder.itemView.ivVolumes.setImageResource(R.drawable.ic_volume3)
                        }
                    }
                }
                else -> {
                    holder.itemView.ivVolumes.visibility = View.GONE
                }
            }
        } else {
            holder.itemView.ivVolumes.visibility = View.GONE
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}