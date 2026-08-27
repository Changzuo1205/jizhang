package com.example.data.local

import androidx.compose.runtime.Immutable

/**
 * [过渡期 UI 领域模型] 记账明细。
 *
 * Phase 1 起存储层已切换至规范化的 [com.example.data.local.entity.TransactionEntity]
 * （金额存分、软删除、账本/转账支持）。本类型仅作为 ViewModel→UI 的映射载体保留，
 * 字段与旧版完全一致以避免大规模 UI 改动；将在 Phase 3 视图层统一后退役。
 *
 * @property type "EXPENSE"/"INCOME"/"TRANSFER"
 * @property amount 元（Double，显示口径；分↔元换算统一走 AmountFormatter）
 */
@Deprecated("过渡期 DTO：由 TransactionEntity 映射而来，Phase 3 后由 UI 模型取代")
@Immutable
data class ExpenseEntity(
    val id: Long = 0,
    val type: String = "EXPENSE",
    val category: String,
    val subCategory: String = "",
    val amount: Double,
    val note: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val accountId: Long = 1L,
    val accountName: String = "默认账户",
    /** 转账对端账户 id（type=TRANSFER 时由映射层填充）；0 表示非转账 */
    val transferToAccountId: Long = 0L,
    /** 转账对端账户名（仅用于展示） */
    val transferToAccountName: String = "",
    /** 业务唯一标识（v2 CSV 导出用；由映射层从 TransactionEntity.uuid 回填） */
    val uuid: String = ""
) {
    /** 显示用分类名称（处理空值和未分类情况） */
    val displayCategory: String get() = category.takeIf { it.isNotBlank() } ?: "未分类"
    
    /** 显示用子分类名称（处理空值情况） */
    val displaySubCategory: String get() = subCategory.takeIf { it.isNotBlank() } ?: ""
    
    /** 完整分类路径显示（主分类 > 子分类） */
    val fullCategoryPath: String get() = when {
        displaySubCategory.isNotBlank() -> "$displayCategory > $displaySubCategory"
        else -> displayCategory
    }
}
