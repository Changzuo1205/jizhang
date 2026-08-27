package com.example.data.local

import androidx.compose.runtime.Immutable

/**
 * [过渡期 UI 领域模型] 资产账户。
 *
 * 存储层已迁移至 [com.example.data.local.entity.AccountEntityV2]（基准余额语义，
 * 实时余额由「基准 ± Σ交易」派生）；本类型承载派生后的展示余额（元）供现有页面渲染。
 */
@Deprecated("过渡期 DTO：由 AccountEntityV2 映射而来，Phase 3 后由 UI 模型取代")
@Immutable
data class AccountEntity(
    val id: Long = 0,
    val name: String,
    val type: String,
    /** 展示余额（元）＝ initial_balance ± Σ未删除交易，由 ViewModel 映射层计算 */
    val balance: Double = 0.0,
    val cardSuffix: String = "",
    val colorHex: String = "#3B82F6",
    val note: String = ""
)
