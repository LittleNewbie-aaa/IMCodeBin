package com.pkurg.lib.ui.vote

data class OptionsItem(
        var options: String
)

data class CreateVoteResult(
        val vote: ResultVoteInfo
)

data class ResultVoteInfo(
        val createUserId: Long,
        val id: Long,
        val pageNo: Int,
        val pageSize: Int,
        val sid: String,
        val title: String,
        val total: Int
)

/**
 * 投票列表数据
 */
data class VoteResult(
        val pageData: VotePageData
)

data class VotePageData(
        val asc: Boolean,
        val current: Int,
        val limit: Int,
        val offset: Int,
        val offsetCurrent: Int,
        val openSort: Boolean,
        val pages: Int,
        val records: List<Record>,
        val searchCount: Boolean,
        val size: Int,
        val total: Int
)

data class Record(
        val createTime: Long,
        val createUserId: Long,
        val id: Long,
        val invalidTime: Long,
        val pageNo: Int,
        val pageSize: Int,
        val sid: String,
        val start: Int,
        val title: String,
        val total: Int,
        val userIdList: List<Long>
)

/**
 * 投票详情数据
 */
data class VoteInfo(
        val vote: Vote,
        val voteOption: List<VoteOption>
)

data class Vote(
        val createTime: Long,
        val createUserId: Long,
        val id: Long,
        val invalidTime: Long,
        val pageNo: Int,
        val pageSize: Int,
        val sid: String,
        val start: Int,
        val title: String,
        val total: Int,
        val userIdList: List<Long>,
        val userIds: String
)

data class VoteOption(
        val id: Int,
        val option: String,
        val userIdList: List<Long>,
        var total: Int,//投票总数
        var isChoose: Boolean = false,//是否展示选择项
        var isChecked: Boolean = false//是否选中
)