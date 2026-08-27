package com.example.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.components.GlowPink
import com.example.ui.components.GlowViolet
import com.example.ui.theme.LocalAppBackgroundConfig
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class AssetHistoryPoint(
    val label: String,
    val value: Double,
    val timestamp: Long
)

@Composable
fun AccountsScreen(
    accounts: List<AccountEntity>,
    expenses: List<ExpenseEntity> = emptyList(),
    totalNetAssets: Double,
    totalPositiveAssets: Double,
    totalDebts: Double,
    onAddAccount: (name: String, type: String, balance: Double, suffix: String, color: String, note: String) -> Unit,
    onUpdateAccount: (account: AccountEntity, saveAsMissedRecord: Boolean, oldBalance: Double) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }

    // Dynamically calculate the real 6-month historical asset values from actual accounts & transactions
    // If a time period has no recorded data, it defaults to zero
    val assetHistory = remember(accounts, expenses, totalPositiveAssets) {
        val currentAsset = totalPositiveAssets
        val points = mutableListOf<AssetHistoryPoint>()
        val hasAnyExpenses = expenses.isNotEmpty()
        
        for (i in 5 downTo 0) {
            val monthCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, -i)
                if (i == 0) {
                    // For current month, take now
                } else {
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }
            }
            val targetTimestamp = monthCal.timeInMillis
            val monthLabel = "${monthCal.get(Calendar.MONTH) + 1}月"
            
            // Check if there is recorded data on or before this period
            val hasDataForThisPeriod = if (i == 0) {
                totalPositiveAssets > 0 || hasAnyExpenses
            } else {
                expenses.any { it.dateTimestamp <= targetTimestamp }
            }

            val historicalAsset = if (!hasDataForThisPeriod) {
                0.0
            } else {
                // Asset(t) = currentAsset + sum_{tx > t}(Expense) - sum_{tx > t}(Income)
                val laterExpenses = expenses.filter { it.dateTimestamp > targetTimestamp }
                val netChangeAfterT = laterExpenses.sumOf { if (it.type == "EXPENSE") it.amount else -it.amount }
                (currentAsset + netChangeAfterT).coerceAtLeast(0.0)
            }
            
            points.add(AssetHistoryPoint(label = monthLabel, value = historicalAsset, timestamp = targetTimestamp))
        }
        points
    }

    val currentMonthAsset = assetHistory.lastOrNull()?.value ?: totalPositiveAssets
    val lastMonthAsset = assetHistory.getOrNull(assetHistory.size - 2)?.value ?: currentMonthAsset
    val monthGrowthRate = if (lastMonthAsset > 0.0) {
        ((currentMonthAsset - lastMonthAsset) / lastMonthAsset) * 100.0
    } else if (currentMonthAsset > 0.0) {
        100.0
    } else {
        0.0
    }

    val growthRateFormatted = if (monthGrowthRate > 0.0) {
        "+${String.format(Locale.CHINA, "%.1f", monthGrowthRate)}% 本月"
    } else if (monthGrowthRate < 0.0) {
        "${String.format(Locale.CHINA, "%.1f", monthGrowthRate)}% 本月"
    } else {
        "持平 本月"
    }

    val rateColor = if (monthGrowthRate >= 0.0) {
        if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald
    } else {
        if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink
    }

    GlassBackgroundWithGlow(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = "资产与账户",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = bgConfig.textPrimary
                        )
                        Text(
                            text = "全景多账户资金管理与余额跟踪",
                            style = MaterialTheme.typography.bodySmall,
                            color = bgConfig.textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Add Account Quick Button
                    GlassCard(
                        shape = RoundedCornerShape(14.dp),
                        backgroundColor = if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.25f),
                        borderColor = Brush.linearGradient(
                            listOf(
                                Color(0xFF818CF8).copy(alpha = 0.8f),
                                if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
                            )
                        ),
                        onClick = {
                            accountToEdit = null
                            showAddDialog = true
                        },
                        modifier = Modifier.testTag("add_account_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加账户",
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "添加账户",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White
                            )
                        }
                    }
                }
            }

            // Top Total Assets Panoramic Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
                    borderColor = Brush.linearGradient(
                        if (bgConfig.isLight) listOf(
                            Color(0xFFE2E8F0),
                            Color(0xFFCBD5E1)
                        ) else listOf(
                            Color.White.copy(alpha = 0.45f),
                            GlowCyan.copy(alpha = 0.4f),
                            GlowViolet.copy(alpha = 0.3f),
                            Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    borderWidth = 1.5.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (bgConfig.isLight) Color(0xFF0284C7) else GlowCyan, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (totalDebts > 0.0) "净资产总值 (元)" else "当前总资产 (元)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = bgConfig.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (totalDebts <= 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (bgConfig.isLight) Color(0xFFECFDF5) else GlowEmerald.copy(alpha = 0.18f)
                                            )
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "零负债",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald
                                        )
                                    }
                                }
                                Text(
                                    text = "${accounts.size} 个有效账户",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bgConfig.textTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Asset Number (Total Net Assets when debts exist, or Current Total Assets when no debt)
                        Text(
                            text = "¥ ${String.format(Locale.CHINA, "%,.2f", if (totalDebts > 0.0) totalNetAssets else totalPositiveAssets)}",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 34.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = bgConfig.textPrimary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Positive Assets vs Debt OR Asset Trend Graph (When no debt)
                        if (totalDebts > 0.0) {
                            // Current layout when there are debts
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Total Positive Assets
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = if (bgConfig.isLight) Color(0xFFECFDF5) else GlowEmerald.copy(alpha = 0.12f),
                                    borderColor = Brush.linearGradient(
                                        listOf(GlowEmerald.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(GlowEmerald.copy(alpha = 0.20f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "总资产",
                                                tint = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "总资产 (存款/理财)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (bgConfig.isLight) Color(0xFF047857) else Color.White.copy(alpha = 0.65f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "¥ ${String.format(Locale.CHINA, "%,.2f", totalPositiveAssets)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                // Total Debts
                                GlassCard(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    backgroundColor = if (bgConfig.isLight) Color(0xFFFFF1F2) else GlowPink.copy(alpha = 0.12f),
                                    borderColor = Brush.linearGradient(
                                        listOf(GlowPink.copy(alpha = 0.5f), Color.White.copy(alpha = 0.1f))
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(GlowPink.copy(alpha = 0.20f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "总负债",
                                                tint = if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "总负债 (信用卡透支)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (bgConfig.isLight) Color(0xFFBE123C) else Color.White.copy(alpha = 0.65f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "¥ ${String.format(Locale.CHINA, "%,.2f", totalDebts)}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            // 无负债时：只显示资产变化趋势图
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                backgroundColor = if (bgConfig.isLight) Color(0xFFEEF2FF) else GlowCyan.copy(alpha = 0.10f),
                                borderColor = Brush.linearGradient(
                                    listOf(
                                        (if (bgConfig.isLight) Color(0xFF818CF8) else GlowCyan).copy(alpha = 0.45f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background((if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.20f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.TrendingUp,
                                                    contentDescription = "资产趋势",
                                                    tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "资产变化趋势",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (bgConfig.isLight) Color(0xFF3730A3) else Color.White
                                                )
                                                Text(
                                                    text = "近6个月资产趋势 · 零负债",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                    color = bgConfig.textSecondary
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(rateColor.copy(alpha = if (bgConfig.isLight) 0.12f else 0.20f))
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = growthRateFormatted,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                fontWeight = FontWeight.Bold,
                                                color = rateColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Canvas Asset Trend Sparkline Chart drawn from actual history
                                    AssetTrendMiniChart(
                                        historyPoints = assetHistory,
                                        isLight = bgConfig.isLight,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Monthly Income/Expense Chart
            item {
                MonthlyIncomeExpenseStatisticsPanel(
                    expenses = expenses,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Account List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "我的账户列表",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Text(
                        text = "左滑可删除 · 点击可编辑",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                }
            }

            // Account Items
            if (accounts.isEmpty()) {
                item {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "暂无自定义账户",
                                style = MaterialTheme.typography.titleMedium,
                                color = bgConfig.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右上角「添加账户」自定义微信、支付宝或银行卡",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    AccountCardItem(
                        account = account,
                        onEdit = {
                            accountToEdit = account
                            showAddDialog = true
                        },
                        onDelete = {
                            onDeleteAccount(account)
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // padding for bottom nav
            }
        }
    }

    if (showAddDialog) {
        AddOrEditAccountDialog(
            accountToEdit = accountToEdit,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance, suffix, color, saveAsMissedRecord, oldBalance ->
                if (accountToEdit == null) {
                    onAddAccount(name, type, balance, suffix, color, "")
                } else {
                    onUpdateAccount(
                        accountToEdit!!.copy(
                            name = name,
                            type = type,
                            balance = balance,
                            cardSuffix = suffix,
                            colorHex = color
                        ),
                        saveAsMissedRecord,
                        oldBalance
                    )
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AccountCardItem(
    account: AccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val density = LocalDensity.current
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val typeIcon = getAccountTypeIcon(account.type)
    val typeName = getAccountTypeName(account.type)

    // Swipe-to-delete state and smooth spring animation
    var isRevealed by remember { mutableStateOf(false) }
    val maxSwipeDp = 84.dp
    val maxSwipePx = with(density) { maxSwipeDp.toPx() }
    var dragAmountAccumulated by remember { mutableFloatStateOf(0f) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (isRevealed) -maxSwipePx else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accountCardSwipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Red Delete Action revealed on the right (Only rendered when swiped or revealed so it never bleeds through translucent card before swiping)
        if (isRevealed || animatedOffsetPx < -1f) {
            val swipeProgress = (kotlin.math.abs(animatedOffsetPx) / maxSwipePx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        alpha = swipeProgress
                    }
                    .clip(RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(maxSwipeDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFEF4444))
                        .clickable {
                            isRevealed = false
                            onDelete()
                        }
                        .testTag("account_delete_button_${account.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "删除",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Sliding Foreground Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                .pointerInput(account.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragAmountAccumulated = if (isRevealed) -maxSwipePx else 0f
                        },
                        onDragEnd = {
                            isRevealed = dragAmountAccumulated < -maxSwipePx * 0.4f
                        },
                        onDragCancel = {
                            isRevealed = dragAmountAccumulated < -maxSwipePx * 0.4f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragAmountAccumulated = (dragAmountAccumulated + dragAmount).coerceIn(-maxSwipePx * 1.2f, 0f)
                            if (dragAmountAccumulated < -maxSwipePx * 0.7f) {
                                isRevealed = true
                            } else if (dragAmountAccumulated > -maxSwipePx * 0.2f) {
                                isRevealed = false
                            }
                        }
                    )
                }
        ) {
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        if (isRevealed) {
                            isRevealed = false
                        } else {
                            onEdit()
                        }
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Colored Brand/Type Icon Bubble
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(accountColor.copy(alpha = if (bgConfig.isLight) 0.15f else 0.22f))
                            .border(1.dp, accountColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = account.name,
                            tint = accountColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Account Details
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                            if (account.cardSuffix.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${account.cardSuffix})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bgConfig.textTertiary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = typeName,
                                style = MaterialTheme.typography.labelSmall,
                                color = accountColor
                            )
                        }
                    }

                    // Balance & Edit Action
                    Column(horizontalAlignment = Alignment.End) {
                        val isNegative = account.balance < 0
                        Text(
                            text = "¥ ${String.format(Locale.CHINA, "%,.2f", account.balance)}",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = if (isNegative) GlowPink else bgConfig.textPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑账户",
                                tint = bgConfig.textTertiary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditAccountDialog(
    accountToEdit: AccountEntity?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, suffix: String, color: String, saveAsMissedRecord: Boolean, oldBalance: Double) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var nameInput by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var balanceInput by remember { mutableStateOf(if (accountToEdit != null) accountToEdit.balance.toString() else "0.0") }
    var suffixInput by remember { mutableStateOf(accountToEdit?.cardSuffix ?: "") }

    val originalBalance = remember(accountToEdit) { accountToEdit?.balance ?: 0.0 }
    val currentBalanceParsed = balanceInput.toDoubleOrNull()
    val balanceDiff = if (currentBalanceParsed != null && accountToEdit != null) currentBalanceParsed - originalBalance else 0.0
    val hasBalanceChanged = accountToEdit != null && kotlin.math.abs(balanceDiff) > 0.001
    var saveAsMissedRecord by remember { mutableStateOf(true) }

    val accountTypes = listOf(
        "WECHAT" to "微信钱包",
        "ALIPAY" to "支付宝",
        "BANK_CARD" to "银行储蓄卡",
        "CREDIT_CARD" to "信用卡",
        "CASH" to "现金零钱",
        "INVESTMENT" to "投资理财",
        "OTHER" to "其他账户"
    )

    var selectedType by remember {
        mutableStateOf(accountToEdit?.type ?: "BANK_CARD")
    }

    val presetColors = listOf(
        "#07C160", "#1677FF", "#E60012", "#8B5CF6", "#F59E0B", "#EC4899", "#06B6D4", "#64748B"
    )

    var selectedColor by remember {
        mutableStateOf(accountToEdit?.colorHex ?: "#1677FF")
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (accountToEdit == null) "添加新账户" else "编辑账户信息",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Name
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("账户名称", color = bgConfig.textSecondary) },
                    placeholder = { Text("如：招商银行卡、微信零钱通", color = bgConfig.textTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Current Balance（v2 语义：此处输入的是基准余额 initial_balance，实时余额由交易派生）
                OutlinedTextField(
                    value = balanceInput,
                    onValueChange = { balanceInput = it },
                    label = { Text("基准余额 (¥)", color = bgConfig.textSecondary) },
                    placeholder = { Text("0.00 (负数代表信用卡已透支)", color = bgConfig.textTertiary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                )

                // Option: Save as Missed Transaction ("保存为漏记款")
                if (hasBalanceChanged) {
                    Spacer(modifier = Modifier.height(10.dp))
                    val isIncome = balanceDiff > 0
                    val absDiff = kotlin.math.abs(balanceDiff)
                    
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (bgConfig.isLight) Color(0xFFF1F5F9).copy(alpha = 0.9f) else Color.White.copy(alpha = 0.08f),
                        borderColor = if (saveAsMissedRecord) {
                            if (isIncome) SolidColor(if (bgConfig.isLight) Color(0xFF10B981).copy(alpha = 0.5f) else GlowEmerald.copy(alpha = 0.6f))
                            else SolidColor(if (bgConfig.isLight) Color(0xFFF43F5E).copy(alpha = 0.5f) else GlowPink.copy(alpha = 0.6f))
                        } else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { saveAsMissedRecord = !saveAsMissedRecord }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = saveAsMissedRecord,
                                onCheckedChange = { saveAsMissedRecord = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = if (isIncome) (if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald) else (if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink),
                                    uncheckedColor = bgConfig.textTertiary,
                                    checkmarkColor = Color.White
                                )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "保存为漏记款",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isIncome)
                                                    (if (bgConfig.isLight) Color(0xFFDCFCE7) else Color(0xFF065F46))
                                                else
                                                    (if (bgConfig.isLight) Color(0xFFFEE2E2) else Color(0xFF991B1B))
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isIncome) "+¥${String.format(Locale.CHINA, "%.2f", absDiff)} 收入" else "-¥${String.format(Locale.CHINA, "%.2f", absDiff)} 支出",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            color = if (isIncome)
                                                (if (bgConfig.isLight) Color(0xFF166534) else Color(0xFF6EE7B7))
                                            else
                                                (if (bgConfig.isLight) Color(0xFF991B1B) else Color(0xFFFCA5A5))
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isIncome)
                                        "勾选后将自动在明细中生成一笔 ¥${String.format(Locale.CHINA, "%.2f", absDiff)} 的【漏记款】收入记录"
                                    else
                                        "勾选后将自动在明细中生成一笔 ¥${String.format(Locale.CHINA, "%.2f", absDiff)} 的【漏记款】支出记录",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = bgConfig.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Account Type Chips
                Text(
                    text = "账户类型",
                    style = MaterialTheme.typography.labelMedium,
                    color = bgConfig.textSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))

                // 账户类型 Chip：条目固定有限，改用 FlowRow 平铺换行，
                // 消除外层可滚动 Column 内嵌套 Lazy 横向滚动容器的嵌套滚动反模式
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    accountTypes.forEach { (typeKey, typeLabel) ->
                        val isSelected = selectedType == typeKey
                        GlassChip(
                            selected = isSelected,
                            onClick = { selectedType = typeKey },
                            selectedGlowColor = GlowViolet
                        ) {
                            Text(
                                text = typeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) GlowViolet else bgConfig.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Color Theme Selector
                Text(
                    text = "账户卡片配色",
                    style = MaterialTheme.typography.labelMedium,
                    color = bgConfig.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val color = Color(android.graphics.Color.parseColor(hex))
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }

                // Card Suffix: Only shown if selectedType is BANK_CARD or CREDIT_CARD
                if (selectedType == "BANK_CARD" || selectedType == "CREDIT_CARD") {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = suffixInput,
                        onValueChange = { suffixInput = it },
                        label = { Text("卡号尾号 (选填)", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：8899", color = bgConfig.textTertiary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = bgConfig.textPrimary,
                            unfocusedTextColor = bgConfig.textPrimary,
                            focusedContainerColor = bgConfig.inputFieldBg,
                            unfocusedContainerColor = bgConfig.inputFieldBg,
                            focusedBorderColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                            unfocusedBorderColor = bgConfig.inputFieldBorder,
                            cursorColor = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_suffix_input")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Confirm button
                val isValid = nameInput.isNotBlank() && balanceInput.toDoubleOrNull() != null
                Button(
                    onClick = {
                        val balance = balanceInput.toDoubleOrNull() ?: 0.0
                        val finalSuffix = if (selectedType == "BANK_CARD" || selectedType == "CREDIT_CARD") suffixInput.trim() else ""
                        onConfirm(
                            nameInput.trim(),
                            selectedType,
                            balance,
                            finalSuffix,
                            selectedColor,
                            saveAsMissedRecord && hasBalanceChanged,
                            originalBalance
                        )
                    },
                    enabled = isValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1),
                        disabledContainerColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_account_button")
                ) {
                    Text(
                        text = if (accountToEdit == null) "保存新账户" else "更新账户信息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun getAccountTypeIcon(type: String): ImageVector {
    return when (type) {
        "WECHAT" -> Icons.Default.Payment
        "ALIPAY" -> Icons.Default.Payment
        "BANK_CARD" -> Icons.Default.AccountBalance
        "CREDIT_CARD" -> Icons.Default.CreditCard
        "CASH" -> Icons.Default.AttachMoney
        "INVESTMENT" -> Icons.Default.TrendingUp
        else -> Icons.Default.AccountBalance
    }
}

fun getAccountTypeName(type: String): String {
    return when (type) {
        "WECHAT" -> "微信钱包"
        "ALIPAY" -> "支付宝"
        "BANK_CARD" -> "储蓄银行卡"
        "CREDIT_CARD" -> "信用卡"
        "CASH" -> "现金钱包"
        "INVESTMENT" -> "投资理财"
        else -> "普通账户"
    }
}

/**
 * Asset Trend Mini Sparkline Chart for the Zero-Debt Asset Header Card
 */
@Composable
fun AssetTrendMiniChart(
    historyPoints: List<AssetHistoryPoint>,
    isLight: Boolean,
    modifier: Modifier = Modifier
) {
    if (historyPoints.isEmpty()) return

    val values = historyPoints.map { it.value }
    val minVal = (values.minOrNull() ?: 0.0).toFloat().coerceAtLeast(0f)
    val maxVal = (values.maxOrNull() ?: 100.0).toFloat().coerceAtLeast(0f)
    val isAllZero = maxVal <= 0.001f
    val isFlat = (maxVal - minVal) < 0.001f

    val padMin = 0f
    val padMax = if (isAllZero) 100f else (if (isFlat) maxVal * 1.25f else maxVal * 1.15f)
    val effectiveRange = (padMax - padMin).coerceAtLeast(1f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val count = historyPoints.size
            if (count < 2) return@Canvas

            val stepX = width / (count - 1)

            val coords = historyPoints.mapIndexed { index, p ->
                val x = index * stepX
                val normalizedY = if (isAllZero) 0.1f else if (isFlat) 0.5f else ((p.value.toFloat() - padMin) / effectiveRange).coerceIn(0.08f, 0.92f)
                val y = height - (normalizedY * height)
                Offset(x, y)
            }

            // Draw Area Gradient under curve
            val fillPath = Path().apply {
                moveTo(0f, height)
                lineTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                    val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                    cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                }
                lineTo(width, height)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        (if (isLight) Color(0xFF6366F1) else GlowCyan).copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )

            // Draw Smooth Bezier Line
            val strokePath = Path().apply {
                moveTo(coords.first().x, coords.first().y)
                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2, p0.y)
                    val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2, p1.y)
                    cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                }
            }

            drawPath(
                path = strokePath,
                color = if (isLight) Color(0xFF4F46E5) else GlowCyan,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Draw Point Markers
            coords.forEachIndexed { index, coord ->
                val isLast = index == coords.size - 1
                drawCircle(
                    color = if (isLast) (if (isLight) Color(0xFF4338CA) else Color.White) else (if (isLight) Color(0xFF818CF8) else GlowCyan),
                    radius = if (isLast) 4.5.dp.toPx() else 2.5.dp.toPx(),
                    center = coord
                )
                if (isLast) {
                    drawCircle(
                        color = (if (isLight) Color(0xFF4F46E5) else GlowCyan).copy(alpha = 0.4f),
                        radius = 8.dp.toPx(),
                        center = coord
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            historyPoints.forEachIndexed { index, item ->
                val isCurrent = index == historyPoints.size - 1
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                    ),
                    color = if (isCurrent) (if (isLight) Color(0xFF4F46E5) else GlowCyan) else (if (isLight) Color(0xFF94A3B8) else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}



@Composable
fun MonthlyIncomeExpenseStatisticsPanel(
    expenses: List<ExpenseEntity>,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var excludeCapitalFlow by remember { mutableStateOf(true) }

    val format = java.text.SimpleDateFormat("MM月", java.util.Locale.CHINA)
    
    val monthlyStats = remember(expenses, excludeCapitalFlow) {
        val stats = mutableMapOf<String, Pair<Double, Double>>() // "MM月" -> Pair(Income, Expense)
        
        // Initialize last 6 months to 0 to maintain order
        val cal = java.util.Calendar.getInstance()
        val monthsList = mutableListOf<String>()
        for (i in 5 downTo 0) {
            val m = cal.clone() as java.util.Calendar
            m.add(java.util.Calendar.MONTH, -i)
            val monthStr = format.format(m.time)
            monthsList.add(monthStr)
            stats[monthStr] = Pair(0.0, 0.0)
        }

        expenses.forEach { expense ->
            val isCapitalFlow = expense.category == "资金流转" || expense.category == "应收款" || expense.category == "报销款"
            if (!excludeCapitalFlow || !isCapitalFlow) {
                val monthStr = format.format(java.util.Date(expense.dateTimestamp))
                if (stats.containsKey(monthStr)) {
                    val current = stats[monthStr]!!
                    if (expense.type == "INCOME") {
                        stats[monthStr] = current.copy(first = current.first + expense.amount)
                    } else {
                        stats[monthStr] = current.copy(second = current.second + expense.amount)
                    }
                }
            }
        }
        
        monthsList.map { Pair(it, stats[it]!!) }
    }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        backgroundColor = if (bgConfig.isLight) Color.White.copy(alpha = 0.95f) else Color(0xFF131C35).copy(alpha = 0.65f),
        borderColor = Brush.linearGradient(
            if (bgConfig.isLight) listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)) 
            else listOf(Color.White.copy(alpha = 0.45f), GlowViolet.copy(alpha = 0.3f), Color.White.copy(alpha = 0.1f))
        ),
        borderWidth = 1.5.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (bgConfig.isLight) Color(0xFF8B5CF6) else GlowViolet, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "月度收支统计",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "排除垫付/报销",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                    Checkbox(
                        checked = excludeCapitalFlow,
                        onCheckedChange = { excludeCapitalFlow = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = if (bgConfig.isLight) Color(0xFF8B5CF6) else GlowViolet,
                            uncheckedColor = bgConfig.textTertiary
                        ),
                        modifier = Modifier.size(24.dp).padding(start = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bar Chart
            val maxAmount = monthlyStats.maxOfOrNull { maxOf(it.second.first, it.second.second) }?.coerceAtLeast(1.0) ?: 1.0
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                monthlyStats.forEach { (month, amounts) ->
                    val (income, expense) = amounts
                    val incomeHeight = (income / maxAmount).toFloat()
                    val expenseHeight = (expense / maxAmount).toFloat()
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight().weight(1f)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Income Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(incomeHeight.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            // Expense Bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .fillMaxHeight(expenseHeight.coerceAtLeast(0.02f))
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = month,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = bgConfig.textSecondary,
                            maxLines = 1
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).background(if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("收入", style = MaterialTheme.typography.labelSmall, color = bgConfig.textSecondary)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(modifier = Modifier.size(8.dp).background(if (bgConfig.isLight) Color(0xFFE11D48) else GlowPink, CircleShape))
                Spacer(modifier = Modifier.width(4.dp))
                Text("支出", style = MaterialTheme.typography.labelSmall, color = bgConfig.textSecondary)
            }
        }
    }
}
