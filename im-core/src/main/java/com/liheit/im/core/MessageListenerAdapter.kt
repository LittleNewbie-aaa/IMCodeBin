package com.liheit.im.core

import com.liheit.im.core.bean.ChatMessage
import com.liheit.im.core.manager.ChatManager

/**
 * Created by daixun on 2018/9/30.
 */

class MessageListenerAdapter : ChatManager.MessageListener {
    override fun onMessageReceived(messages: List<ChatMessage>, isOfflin:Boolean) {
    }

    override fun onCmdMessageReceived(messages: List<ChatMessage>) {
    }

    override fun onMessageRead(messages: List<ChatMessage>) {
    }

    override fun onMessageDelivered(messages: List<ChatMessage>) {
    }

    override fun onMessageChanged(messages: List<ChatMessage>) {
    }

    override fun onMessageDelete(sid: String?,mid: String) {
    }

    override fun onOneselfSendMessage(messages: List<ChatMessage>) {
    }

    override fun onMessageRecall(messages: ChatMessage) {
    }

    override fun onFileMessageUpProgress(msg: ChatMessage, upProgress: Int) {
    }
}
