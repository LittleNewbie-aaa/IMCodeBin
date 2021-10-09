package com.pkurg.lib.ui.solitaire

data class SolitaireTitle(
        var userId: Long,
        var title: String = "",
        var userName: String,
        var example: String,
        var id: String = "",//此接龙的id
        var userCount: Int = 0
)

data class ContentItem(
        var itemId: String = "",//接龙中某一项的id
        var userId: Long,
        var userName: String? = "",
        var context: String,
        var chainsId: String = ""//此接龙的id
) {
    override fun equals(other: Any?): Boolean {
        val other: ContentItem = other as ContentItem
        if (context == other.context) {
            return true
        }
        return false
    }
}

data class ContentAdd(val name: String = "")
