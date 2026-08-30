package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.model.AmountFormatter
import com.example.ui.theme.LocalAppBackgroundConfig
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
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
    val isLight = bgConfig.isLight

    // Editorial Color Palette
    val canvasBg = if (isLight) Color(0xFFFAFAF7) else Color(0xFF242E24)
    val cardBg = if (isLight) Color(0xFFF6F4EE) else Color(0xFF1E281E)
    val itemBg = if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF2B372B)
    val dividerColor = if (isLight) Color(0xFFE4DFD3) else Color(0xFF374637)
    val inkPrimary = if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7)
    val inkSecondary = if (isLight) Color(0xFF5A5852) else Color(0xFFB5B3AA)
    val inkMuted = if (isLight) Color(0xFF8A8780) else Color(0xFF889689)
    val clayAccent = Color(0xFFC4623D)
    val forestGreen = if (isLight) Color(0xFF2D6A4F) else Color(0xFF52B788)
    val debtRed = if (isLight) Color(0xFFDC2626) else Color(0xFFEF4444)

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }

    val todayDateStr = remember {
        SimpleDateFormat("yyyy.MM.dd · EEEE", Locale.CHINESE).format(Date())
    }

    // Calculate historical asset values from earliest data to now
    val assetHistory = remember(accounts, expenses, totalPositiveAssets, totalNetAssets, totalDebts) {
        val currentAsset = if (totalDebts > 0.0) totalNetAssets else totalPositiveAssets
        val points = mutableListOf<AssetHistoryPoint>()
        
        if (expenses.isEmpty()) {
            for (i in 5 downTo 0) {
                val monthCal = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -i)
                    if (i != 0) {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }
                }
                val targetTimestamp = monthCal.timeInMillis
                val monthLabel = "${monthCal.get(Calendar.MONTH) + 1}月"
                points.add(AssetHistoryPoint(label = monthLabel, value = currentAsset, timestamp = targetTimestamp))
            }
        } else {
            val minTimestamp = expenses.minOf { it.dateTimestamp }
            val startCal = Calendar.getInstance().apply {
                timeInMillis = minTimestamp
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val nowCal = Calendar.getInstance()
            
            val cursorCal = startCal.clone() as Calendar
            while (cursorCal.get(Calendar.YEAR) < nowCal.get(Calendar.YEAR) ||
                (cursorCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) && cursorCal.get(Calendar.MONTH) <= nowCal.get(Calendar.MONTH))
            ) {
                val isCurrentMonth = cursorCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                        cursorCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH)
                
                val targetTimestamp = if (isCurrentMonth) {
                    nowCal.timeInMillis
                } else {
                    val endOfMonth = cursorCal.clone() as Calendar
                    endOfMonth.set(Calendar.DAY_OF_MONTH, endOfMonth.getActualMaximum(Calendar.DAY_OF_MONTH))
                    endOfMonth.set(Calendar.HOUR_OF_DAY, 23)
                    endOfMonth.set(Calendar.MINUTE, 59)
                    endOfMonth.set(Calendar.SECOND, 59)
                    endOfMonth.set(Calendar.MILLISECOND, 999)
                    endOfMonth.timeInMillis
                }
                
                val year = cursorCal.get(Calendar.YEAR) % 100
                val month = cursorCal.get(Calendar.MONTH) + 1
                val monthLabel = if (nowCal.get(Calendar.YEAR) != startCal.get(Calendar.YEAR)) {
                    String.format(Locale.CHINA, "%02d.%02d", year, month)
                } else {
                    "${month}月"
                }

                val laterExpenses = expenses.filter { it.dateTimestamp > targetTimestamp }
                val netChangeAfterT = laterExpenses.sumOf { if (it.type == "EXPENSE") it.amount else -it.amount }
                val historicalAsset = (currentAsset + netChangeAfterT).coerceAtLeast(0.0)

                points.add(AssetHistoryPoint(label = monthLabel, value = historicalAsset, timestamp = targetTimestamp))
                cursorCal.add(Calendar.MONTH, 1)
            }
            
            if (points.size == 1) {
                val prevCal = (nowCal.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                val prevLabel = if (nowCal.get(Calendar.YEAR) != startCal.get(Calendar.YEAR)) {
                    String.format(Locale.CHINA, "%02d.%02d", prevCal.get(Calendar.YEAR) % 100, prevCal.get(Calendar.MONTH) + 1)
                } else {
                    "${prevCal.get(Calendar.MONTH) + 1}月"
                }
                points.add(0, AssetHistoryPoint(label = prevLabel, value = points[0].value, timestamp = prevCal.timeInMillis))
            }
        }
        points
    }

    val currentMonthAsset = assetHistory.lastOrNull()?.value ?: (if (totalDebts > 0.0) totalNetAssets else totalPositiveAssets)
    val lastMonthAsset = assetHistory.getOrNull(assetHistory.size - 2)?.value ?: currentMonthAsset
    val monthGrowthRate = if (lastMonthAsset > 0.0) {
        ((currentMonthAsset - lastMonthAsset) / lastMonthAsset) * 100.0
    } else if (currentMonthAsset > 0.0) {
        100.0
    } else {
        0.0
    }

    val growthRateFormatted = if (monthGrowthRate > 0.0) {
        "+${String.format(Locale.CHINA, "%.1f", monthGrowthRate)}% 环比"
    } else if (monthGrowthRate < 0.0) {
        "${String.format(Locale.CHINA, "%.1f", monthGrowthRate)}% 环比"
    } else {
        "持平 环比"
    }

    val rateColor = if (monthGrowthRate >= 0.0) forestGreen else debtRed

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Editorial Masthead Header
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Accounts",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = inkPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "LEDGER & ASSETS · $todayDateStr",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = inkMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Add Account Button (Editorial Pill Style)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(clayAccent.copy(alpha = 0.12f))
                            .border(1.dp, clayAccent.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .clickable {
                                accountToEdit = null
                                showAddDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("add_account_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "添加账户",
                            tint = clayAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "添加账户",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = clayAccent
                        )
                    }
                }
            }

            // 2. Editorial Top Balance Ticket Card (Panoramic Asset Banner)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(cardBg)
                        .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Card Top Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (totalDebts > 0.0) "NET ASSETS · 净资产" else "TOTAL ASSETS · 当前总资产",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkMuted,
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (totalDebts <= 0.0) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(forestGreen.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "零负债",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = forestGreen
                                        )
                                    }
                                }
                                Text(
                                    text = "${accounts.size} 个账户",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = inkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Large Net Asset Number (Monospace)
                        val targetNetAsset = if (totalDebts > 0.0) totalNetAssets else totalPositiveAssets
                        val animatedNetAsset = remember { Animatable(0f) }
                        LaunchedEffect(targetNetAsset) {
                            animatedNetAsset.animateTo(
                                targetValue = targetNetAsset.toFloat(),
                                animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing)
                            )
                        }

                        val targetCents = AmountFormatter.yuanToCents(animatedNetAsset.value.toDouble())
                        val displayNetAssetStr = AmountFormatter.formatCentsAsYuan(targetCents)

                        Text(
                            text = "¥ $displayNetAssetStr",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkPrimary,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Conditional: Two-column ledger summary when debts exist, or Asset Trend Sparkline when no debt
                        if (totalDebts > 0.0) {
                            // Total Positive Assets vs Total Debts (Dual-Column Ledger Layout)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 资产总额 (存款/理财)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(canvasBg)
                                        .border(1.dp, dividerColor, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowUpward,
                                                contentDescription = "资产总额",
                                                tint = forestGreen,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "资产总额 (储蓄/理财)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = inkMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val posCents = AmountFormatter.yuanToCents(totalPositiveAssets)
                                        Text(
                                            text = "¥${AmountFormatter.formatCentsAsYuan(posCents)}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = forestGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // 负债总额 (信用卡透支/借贷)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(canvasBg)
                                        .border(1.dp, dividerColor, RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDownward,
                                                contentDescription = "总负债",
                                                tint = debtRed,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "总负债 (信用卡透支)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = inkMuted
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val debtCents = AmountFormatter.yuanToCents(totalDebts)
                                        Text(
                                            text = "¥${AmountFormatter.formatCentsAsYuan(debtCents)}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = debtRed,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            // 无负债时展示：资产走势折线 (Asset Trend Sparkline)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(canvasBg)
                                    .border(1.dp, dividerColor, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = "资产走势",
                                                tint = clayAccent,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "历史资产走势",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = inkPrimary
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(rateColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        ) {
                                            Text(
                                                text = growthRateFormatted,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = rateColor
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Canvas Bezier Asset Trend Chart
                                    EditorialAssetTrendChart(
                                        historyPoints = assetHistory,
                                        isLight = isLight,
                                        inkPrimary = inkPrimary,
                                        inkMuted = inkMuted,
                                        accentColor = clayAccent,
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

            // 3. Account List Section Title
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "账户明细",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkPrimary
                    )
                    Text(
                        text = "左滑可删除 · 点击可编辑",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = inkMuted
                    )
                }
            }

            // 4. Account Items Matrix
            if (accounts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(cardBg)
                            .border(1.dp, dividerColor, RoundedCornerShape(18.dp))
                            .padding(vertical = 36.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = inkMuted,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "暂无账户",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右上角「添加账户」配置微信、支付宝、银行卡或信用卡",
                                fontSize = 12.sp,
                                color = inkMuted
                            )
                        }
                    }
                }
            } else {
                items(accounts, key = { it.id }) { account ->
                    EditorialAccountCardItem(
                        account = account,
                        canvasBg = canvasBg,
                        itemBg = itemBg,
                        dividerColor = dividerColor,
                        inkPrimary = inkPrimary,
                        inkSecondary = inkSecondary,
                        inkMuted = inkMuted,
                        clayAccent = clayAccent,
                        forestGreen = forestGreen,
                        debtRed = debtRed,
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
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }

    if (showAddDialog) {
        EditorialAddOrEditAccountDialog(
            accountToEdit = accountToEdit,
            canvasBg = canvasBg,
            dividerColor = dividerColor,
            inkPrimary = inkPrimary,
            inkSecondary = inkSecondary,
            inkMuted = inkMuted,
            clayAccent = clayAccent,
            forestGreen = forestGreen,
            debtRed = debtRed,
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
fun EditorialAccountCardItem(
    account: AccountEntity,
    canvasBg: Color,
    itemBg: Color,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    debtRed: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val accountColor = try {
        Color(android.graphics.Color.parseColor(account.colorHex))
    } catch (e: Exception) {
        Color(0xFF3B82F6)
    }

    val typeIcon = getAccountTypeIcon(account.type)
    val typeName = getAccountTypeName(account.type)

    var isRevealed by remember { mutableStateOf(false) }
    val maxSwipeDp = 80.dp
    val maxSwipePx = with(density) { maxSwipeDp.toPx() }
    var dragAmountAccumulated by remember { mutableFloatStateOf(0f) }

    val animatedOffsetPx by animateFloatAsState(
        targetValue = if (isRevealed) -maxSwipePx else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "editorialAccountSwipeOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        // Revealed Red Delete Button
        if (isRevealed || animatedOffsetPx < -1f) {
            val swipeProgress = (abs(animatedOffsetPx) / maxSwipePx).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = swipeProgress }
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .width(maxSwipeDp)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFDC2626))
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
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "删除",
                            fontSize = 11.5.sp,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(itemBg)
                    .border(1.dp, dividerColor, RoundedCornerShape(16.dp))
                    .clickable {
                        if (isRevealed) {
                            isRevealed = false
                        } else {
                            onEdit()
                        }
                    }
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vintage Stamp Icon Bubble
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accountColor.copy(alpha = 0.14f))
                            .border(1.dp, accountColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = typeIcon,
                            contentDescription = account.name,
                            tint = accountColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Account Details
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = inkPrimary
                            )
                            if (account.cardSuffix.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${account.cardSuffix})",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    color = inkMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(3.dp))

                        Text(
                            text = typeName,
                            fontSize = 11.5.sp,
                            color = inkSecondary
                        )
                    }

                    // Balance Display (Monospace)
                    Column(horizontalAlignment = Alignment.End) {
                        val isNegative = account.balance < 0
                        val balanceCents = AmountFormatter.yuanToCents(abs(account.balance))
                        val formattedBalance = AmountFormatter.formatCentsAsYuan(balanceCents)

                        Text(
                            text = "${if (isNegative) "-" else ""}¥$formattedBalance",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isNegative) debtRed else inkPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "编辑账户",
                                tint = inkMuted,
                                modifier = Modifier.size(14.dp)
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
fun EditorialAddOrEditAccountDialog(
    accountToEdit: AccountEntity?,
    canvasBg: Color,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    debtRed: Color,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, suffix: String, color: String, saveAsMissedRecord: Boolean, oldBalance: Double) -> Unit
) {
    var isDismissing by remember { mutableStateOf(false) }
    val animProgress by animateFloatAsState(
        targetValue = if (isDismissing) 0f else 1f,
        animationSpec = if (isDismissing) tween(160) else tween(220),
        label = "account_dialog_anim",
        finishedListener = { value ->
            if (value == 0f && isDismissing) {
                onDismiss()
            }
        }
    )

    val dialogScale by animateFloatAsState(
        targetValue = if (isDismissing) 0.90f else 1f,
        animationSpec = if (isDismissing) tween(160) else spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "account_dialog_scale"
    )

    var nameInput by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var balanceInput by remember { mutableStateOf(if (accountToEdit != null) accountToEdit.balance.toString() else "0.0") }
    var suffixInput by remember { mutableStateOf(accountToEdit?.cardSuffix ?: "") }

    val originalBalance = remember(accountToEdit) { accountToEdit?.balance ?: 0.0 }
    val currentBalanceParsed = balanceInput.toDoubleOrNull()
    val balanceDiff = if (currentBalanceParsed != null && accountToEdit != null) currentBalanceParsed - originalBalance else 0.0
    val hasBalanceChanged = accountToEdit != null && abs(balanceDiff) > 0.001
    var saveAsMissedRecord by remember { mutableStateOf(true) }

    val accountTypes = listOf(
        "WECHAT" to "微信钱包",
        "ALIPAY" to "支付宝",
        "BANK_CARD" to "储蓄卡",
        "CREDIT_CARD" to "信用卡",
        "CASH" to "现金零钱",
        "INVESTMENT" to "投资理财",
        "OTHER" to "其他账户"
    )

    var selectedType by remember {
        mutableStateOf(accountToEdit?.type ?: "BANK_CARD")
    }

    val presetColors = listOf(
        "#07C160", "#1677FF", "#E60012", "#C4623D", "#8B5CF6", "#F59E0B", "#06B6D4", "#52525B"
    )

    var selectedColor by remember {
        mutableStateOf(accountToEdit?.colorHex ?: "#1677FF")
    }

    BackHandler {
        if (!isDismissing) isDismissing = true
    }

    Dialog(
        onDismissRequest = {
            if (!isDismissing) isDismissing = true
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f * animProgress))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    if (!isDismissing) isDismissing = true
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .graphicsLayer {
                        scaleX = dialogScale
                        scaleY = dialogScale
                        alpha = animProgress
                    }
                    .clip(RoundedCornerShape(20.dp))
                    .background(canvasBg)
                    .border(1.dp, dividerColor, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* 阻止冒泡 */ }
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkPrimary
                        )
                        IconButton(
                            onClick = { if (!isDismissing) isDismissing = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = inkMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account Name
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("账户名称", color = inkSecondary, fontSize = 13.sp) },
                        placeholder = { Text("如：招商银行卡、微信零钱", color = inkMuted, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inkPrimary,
                            unfocusedTextColor = inkPrimary,
                            focusedContainerColor = dividerColor.copy(alpha = 0.25f),
                            unfocusedContainerColor = dividerColor.copy(alpha = 0.15f),
                            focusedBorderColor = clayAccent,
                            unfocusedBorderColor = dividerColor,
                            cursorColor = clayAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_name_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Balance
                    OutlinedTextField(
                        value = balanceInput,
                        onValueChange = { balanceInput = it },
                        label = { Text("基准余额 (¥)", color = inkSecondary, fontSize = 13.sp) },
                        placeholder = { Text("0.00 (负数代表信用卡已透支)", color = inkMuted, fontSize = 13.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inkPrimary,
                            unfocusedTextColor = inkPrimary,
                            focusedContainerColor = dividerColor.copy(alpha = 0.25f),
                            unfocusedContainerColor = dividerColor.copy(alpha = 0.15f),
                            focusedBorderColor = forestGreen,
                            unfocusedBorderColor = dividerColor,
                            cursorColor = forestGreen
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("account_balance_input")
                    )

                    // Option: Save as Missed Transaction
                    if (hasBalanceChanged) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val isIncome = balanceDiff > 0
                        val absDiff = abs(balanceDiff)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(dividerColor.copy(alpha = 0.35f))
                                .border(
                                    1.dp,
                                    if (saveAsMissedRecord) (if (isIncome) forestGreen.copy(alpha = 0.5f) else debtRed.copy(alpha = 0.5f)) else dividerColor,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { saveAsMissedRecord = !saveAsMissedRecord }
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = saveAsMissedRecord,
                                    onCheckedChange = { saveAsMissedRecord = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = if (isIncome) forestGreen else debtRed,
                                        uncheckedColor = inkMuted,
                                        checkmarkColor = Color.White
                                    )
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "保存为漏记款",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = inkPrimary
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (isIncome) forestGreen.copy(alpha = 0.15f) else debtRed.copy(alpha = 0.15f)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.5.dp)
                                        ) {
                                            Text(
                                                text = if (isIncome) "+¥${String.format(Locale.CHINA, "%.2f", absDiff)}" else "-¥${String.format(Locale.CHINA, "%.2f", absDiff)}",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isIncome) forestGreen else debtRed
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isIncome) "自动生成一笔【漏记款】收入" else "自动生成一笔【漏记款】支出",
                                        fontSize = 11.sp,
                                        color = inkMuted
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Account Type Chips
                    Text(
                        text = "账户类型",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = inkSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accountTypes.forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedType == typeKey
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) clayAccent.copy(alpha = 0.15f) else dividerColor.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) clayAccent else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedType = typeKey }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) clayAccent else inkSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Color Theme Selector
                    Text(
                        text = "账户卡片配色",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = inkSecondary
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
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) inkPrimary else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(Color.White, CircleShape)
                                    )
                                }
                            }
                        }
                    }

                    // Card Suffix
                    if (selectedType == "BANK_CARD" || selectedType == "CREDIT_CARD") {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = suffixInput,
                            onValueChange = { suffixInput = it },
                            label = { Text("卡号尾号 (选填)", color = inkSecondary, fontSize = 13.sp) },
                            placeholder = { Text("如：8899", color = inkMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = inkPrimary,
                                unfocusedTextColor = inkPrimary,
                                focusedContainerColor = dividerColor.copy(alpha = 0.25f),
                                unfocusedContainerColor = dividerColor.copy(alpha = 0.15f),
                                focusedBorderColor = clayAccent,
                                unfocusedBorderColor = dividerColor,
                                cursorColor = clayAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("account_suffix_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

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
                            containerColor = clayAccent,
                            disabledContainerColor = dividerColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("save_account_button")
                    ) {
                        Text(
                            text = if (accountToEdit == null) "保存新账户" else "更新账户信息",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
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
 * Editorial Asset Trend Bezier Chart with Interactive Scrubbing
 */
@Composable
fun EditorialAssetTrendChart(
    historyPoints: List<AssetHistoryPoint>,
    isLight: Boolean,
    inkPrimary: Color,
    inkMuted: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    if (historyPoints.isEmpty()) return

    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(historyPoints) {
        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
    }
    val progress = animProgress.value

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val values = historyPoints.map { it.value }
    val minVal = (values.minOrNull() ?: 0.0).toFloat().coerceAtLeast(0f)
    val maxVal = (values.maxOrNull() ?: 100.0).toFloat().coerceAtLeast(0f)
    val isAllZero = maxVal <= 0.001f
    val isFlat = (maxVal - minVal) < 0.001f

    val padMin = 0f
    val padMax = if (isAllZero) 100f else (if (isFlat) maxVal * 1.25f else maxVal * 1.15f)
    val effectiveRange = (padMax - padMin).coerceAtLeast(1f)

    Column(modifier = modifier) {
        if (selectedIndex != null && selectedIndex in historyPoints.indices) {
            val point = historyPoints[selectedIndex!!]
            val pointCents = AmountFormatter.yuanToCents(point.value)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 3.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "时点: ${point.label}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "净资产: ¥${AmountFormatter.formatCentsAsYuan(pointCents)}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = inkPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(historyPoints) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            val width = size.width
                            val count = historyPoints.size
                            if (count > 1 && width > 0) {
                                val idx = ((offset.x / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                selectedIndex = idx
                            }
                        },
                        onDragEnd = { selectedIndex = null },
                        onDragCancel = { selectedIndex = null },
                        onHorizontalDrag = { change, _ ->
                            val width = size.width
                            val count = historyPoints.size
                            if (count > 1 && width > 0) {
                                val idx = ((change.position.x / width) * (count - 1)).roundToInt().coerceIn(0, count - 1)
                                selectedIndex = idx
                            }
                        }
                    )
                }
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val width = size.width
                val height = size.height
                val count = historyPoints.size
                if (count < 2) return@Canvas

                val stepX = width / (count - 1)

                val coords = historyPoints.mapIndexed { index, p ->
                    val x = index * stepX
                    val normalizedY = if (isAllZero) 0.1f else if (isFlat) 0.5f else ((p.value.toFloat() - padMin) / effectiveRange).coerceIn(0.08f, 0.92f)
                    val y = height - (normalizedY * height * progress)
                    Offset(x, y)
                }

                // Gradient Area Fill under Bezier curve
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
                            accentColor.copy(alpha = 0.22f * progress),
                            Color.Transparent
                        )
                    )
                )

                // Smooth Bezier Line
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
                    color = accentColor,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Point Markers
                coords.forEachIndexed { index, coord ->
                    val isLast = index == coords.size - 1
                    val isSelected = selectedIndex == index
                    val pointRadius = if (isSelected) 5.dp.toPx() else if (isLast) 4.dp.toPx() else 2.dp.toPx()

                    drawCircle(
                        color = if (isSelected || isLast) accentColor else accentColor.copy(alpha = 0.6f),
                        radius = pointRadius,
                        center = coord
                    )
                    if (isLast || isSelected) {
                        drawCircle(
                            color = accentColor.copy(alpha = 0.25f * progress),
                            radius = (if (isSelected) 8.dp else 6.5.dp).toPx(),
                            center = coord
                        )
                    }
                }

                // Vertical Scrubbing Line
                selectedIndex?.let { selIdx ->
                    if (selIdx in coords.indices) {
                        val selCoord = coords[selIdx]
                        drawLine(
                            color = accentColor.copy(alpha = 0.5f),
                            start = Offset(selCoord.x, 0f),
                            end = Offset(selCoord.x, height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(3.dp))

        // Month Labels Row
        val step = if (historyPoints.size > 8) (historyPoints.size / 6).coerceAtLeast(1) else 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            historyPoints.forEachIndexed { index, item ->
                val isFirst = index == 0
                val isLast = index == historyPoints.size - 1
                val isSampled = index % step == 0 || isFirst || isLast
                if (isSampled) {
                    Text(
                        text = item.label,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        color = if (isLast) accentColor else inkMuted
                    )
                }
            }
        }
    }
}
