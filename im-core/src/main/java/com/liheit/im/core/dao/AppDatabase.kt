package com.liheit.im.core.dao

import android.arch.persistence.db.SupportSQLiteDatabase
import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters
import android.arch.persistence.room.migration.Migration
import com.liheit.im.core.bean.*
import com.liheit.im.utils.json.MessageListConverter


/**
 * Created by daixun on 2018/6/18.
 * 创建数据库配置类
 */
@Database(entities = [User::class, UserDepartment::class, Session::class,
            SessionMember::class, ReceiptStatus::class, MessageFile::class,
            ForwardMsg::class, Department::class, Conversation::class,
            ChatMessage::class, CollectMsg::class,Subscription::class], version = AppDatabase.VERSION
)
@TypeConverters(MessageListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE Session "
                        + " ADD COLUMN notice TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS collect_msg" +
                        "(id INTEGER PRIMARY KEY NOT NULL," +
                        "createTime INTEGER NOT NULL DEFAULT 0," +
                        "type INTEGER NOT NULL DEFAULT 0," +
                        "describe TEXT," +
                        "userId INTEGER NOT NULL DEFAULT 0," +
                        "content TEXT," +
                        "tag TEXT,"+
                        "msgs TEXT)")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE User "
                        + " ADD COLUMN upinyin TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5: Migration = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS Subscription" +
                        "(id INTEGER PRIMARY KEY NOT NULL," +
                        "createTime INTEGER NOT NULL DEFAULT 0," +
                        "updateTime INTEGER NOT NULL DEFAULT 0," +
                        "sid TEXT NOT NULL DEFAULT ''," +
                        "logo TEXT," +
                        "name TEXT,"+
                        "status INTEGER NOT NULL DEFAULT 1)")
            }
        }

        val MIGRATION_5_6: Migration = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE User "
                        + " ADD COLUMN json_extend TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE User "
                        + " ADD COLUMN rank INTEGER NOT NULL DEFAULT 0")
            }
        }
        const val VERSION = 6
    }

    abstract fun userDao(): UserDao//用户数据表管理器
    abstract fun msgDao(): MessageDao//聊天消息表管理器
    abstract fun convDao(): ConversationDao//消息列表表管理器
    abstract fun sessionDao(): SessionDao//群组表管理器
    abstract fun receiptStatusDao(): ReceiptStatusDao
    abstract fun depDao(): DepartmentDao
    abstract fun uDepDao(): UserDepartmentDao
    abstract fun sessionMemberDao(): SessionMemberDao//群组成员表管理器
    abstract fun msgFileDao(): MessageFileDao//聊天文件表管理器
    abstract fun collectDao(): CollectDao//收藏消息表管理器
    abstract fun subscriptionDao(): SubscriptionDao//公众号表管理器
}

@Database(entities = [Config::class], version = ConfigDatabase.VERSION)
@TypeConverters(MessageListConverter::class)
abstract class ConfigDatabase : RoomDatabase() {

    companion object {
        const val VERSION = 1
    }

    abstract fun configDao(): ConfigDao
}