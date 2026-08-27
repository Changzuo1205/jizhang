package com.example.data.migration

import android.content.Context
import java.io.File

/**
 * 旧版数据库（v10 及更早）的清理与备份。
 *
 * 背景：v11 引入全新六表 schema（含 Double→Int 分等不兼容变更），
 * 且项目尚无线上存量用户（versionCode=1），因此选择「备份 + 全新安装」
 * 而非编写跨结构 Migration。老库以原始文件形式完整备份到应用私有目录，
 * 避免任何静默数据丢失。
 */
object LegacyBackup {

    /** 历史上出现过的全部旧库文件名 */
    private val LEGACY_DB_NAMES = listOf(
        "daily_expense_db_v6",
        "daily_expense_v10.db"
    )

    /**
     * 若检测到旧库文件：复制到 filesDir/legacy_backup/<时间戳>/ 后删除原文件。
     *
     * @return 实际完成备份的旧库文件名列表（通常为空）
     */
    fun backupAndRemoveLegacyDatabases(context: Context): List<String> {
        val dbDir = context.getDatabasePath("unused").parentFile ?: return emptyList()
        val backupRoot = File(context.filesDir, "legacy_backup").apply { mkdirs() }
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val targetDir = File(backupRoot, stamp).apply { mkdirs() }

        val backedUp = mutableListOf<String>()
        for (name in LEGACY_DB_NAMES) {
            for (suffix in listOf("", "-wal", "-shm")) {
                val f = File(dbDir, name + suffix)
                if (!f.exists()) continue
                runCatching {
                    if (suffix.isEmpty()) {
                        f.copyTo(File(targetDir, name), overwrite = true)
                        backedUp.add(name)
                    } else {
                        f.copyTo(File(targetDir, name + suffix), overwrite = true)
                    }
                }
                runCatching { f.delete() }
            }
        }
        return backedUp
    }
}
