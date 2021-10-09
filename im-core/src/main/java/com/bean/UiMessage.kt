package com.bean

import com.liheit.im.core.bean.ChatMessage

data class UiMessage(
        val type: Int,
        val notificationContent: String = "",
        val rawMsg: ChatMessage
)