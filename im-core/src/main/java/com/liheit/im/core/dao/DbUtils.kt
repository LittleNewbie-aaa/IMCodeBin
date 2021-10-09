package com.liheit.im.core.dao

import android.arch.persistence.room.Room
import android.arch.persistence.room.RoomDatabase
import android.content.Context
import android.text.SpannableStringBuilder
import com.commonsware.cwac.saferoom.SafeHelperFactory
import com.liheit.im.core.IMClient
import com.liheit.im.core.bean.DBConfig
import com.liheit.im.utils.json.fromJson
import java.io.FileOutputStream

/**
 * Created by daixun on 2018/6/18.
 * 初始化数据库，用于设置数据库名称和数据库的行为
 *
 */

object DbUtils {

    var isSafeDB = true
    var debugModel = false

    fun init(context: Context) {
        var DB_NAME = "config"
        configDB = Room.databaseBuilder(context.applicationContext, ConfigDatabase::class.java, DB_NAME)
                .apply {
                    if (isSafeDB) {
                        val passphraseField = context.packageName
                        val factory = SafeHelperFactory.fromUser(SpannableStringBuilder().append(passphraseField))
                        openHelperFactory(factory)
                    }
                }
                .allowMainThreadQueries()
                .build()
    }

    lateinit var currentDB: AppDatabase
    lateinit var configDB: ConfigDatabase

    fun isSetup(): Boolean {
        return DbUtils::currentDB.isInitialized
    }

    fun switchToUser(context: Context, account: String) {

        var DB_NAME = "${account}_msg_database"

        val db = context.getDatabasePath(DB_NAME)

        if (isSafeDB) {
            if (!db.exists()) {
                try {
                    val stream = context.resources.assets.open("db")
                    val outputStream = FileOutputStream(db)
                    stream.copyTo(outputStream)
                    stream.close()
                    outputStream.close()
                    context.resources.assets.open("dbConfig.json").bufferedReader().use { ins ->
                        val json = ins.readText()
                        val dbConfig = json.fromJson<DBConfig>()
                        IMClient.departmentManager.lastDepUpdateTime = dbConfig.lastDepUpdateTime
                        IMClient.departmentManager.lastDepUserUpdateTime = dbConfig.lastDepUserUpdateTime
                        IMClient.userManager.lastUserUpdateTime = dbConfig.lastUserUpdateTime
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        currentDB = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DB_NAME)
                .apply {
                    if (debugModel) {
                        setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    }
                    if (isSafeDB) {
                        val passphraseField = "56AG8tqu"
                        val factory = SafeHelperFactory.fromUser(SpannableStringBuilder().append(passphraseField))
                        openHelperFactory(factory)
                    }
                }
                .addMigrations(AppDatabase.MIGRATION_1_2,AppDatabase.MIGRATION_2_3,
                        AppDatabase.MIGRATION_3_4,AppDatabase.MIGRATION_4_5,
                        AppDatabase.MIGRATION_5_6)
                .allowMainThreadQueries()
                .setJournalMode(RoomDatabase.JournalMode.AUTOMATIC)
                .build()

        var dbpath = currentDB.openHelper.writableDatabase.path
        dbpath.length
    }
}
