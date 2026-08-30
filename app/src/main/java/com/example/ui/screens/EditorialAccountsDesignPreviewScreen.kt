package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlin.math.sign
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.model.AmountFormatter
import com.example.ui.theme.BackgroundConfig
import com.example.ui.theme.LocalAppBackgroundConfig
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

enum class SwissScenario(
    val title: String,
    val subtitle: String,
    val icon: ImageVector
) {
    LIVE_DATA("真实数据", "当前本地账户", Icons.Default.AccountBalance),
    BALANCED_MATRIX("标准三卡包", "流动+储蓄+信用", Icons.Default.ViewCarousel),
    MULTI_CARDS("多卡饱和", "每个包 4~5 张卡", Icons.Default.FormatListNumbered),
    ZERO_DEBT("零负债清爽", "无借贷包", Icons.Default.Savings)
}

/**
 * 瑞士极简圆柱滚筒总览与设计稿深度打磨版
 * 
 * 优化重点：
 * 1. 卡包折叠/展开选中态（默认折叠仅展示账户数与总额，点击流畅展开）
 * 2. 移除繁复生硬黑色方框，统一采用与首页和谐的暖纸白/墨绿柔和极简风，提升界面呼吸感
 * 3. 增强三维立体滚筒视差、透明度与 3D 旋转翻滚阻尼动效，打造如实体机械转轮般的精致触感
 */
@Composable
fun EditorialAccountsDesignPreviewScreen(
    realAccounts: List<AccountEntity>,
    realExpenses: List<ExpenseEntity>,
    realTotalNetAssets: Double,
    realTotalPositiveAssets: Double,
    realTotalDebts: Double,
    onAddAccount: (name: String, type: String, balance: Double, suffix: String, color: String, note: String) -> Unit,
    onUpdateAccount: (account: AccountEntity, saveAsMissedRecord: Boolean, oldBalance: Double) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val systemBgConfig = LocalAppBackgroundConfig.current
    var selectedScenario by remember { mutableStateOf(SwissScenario.LIVE_DATA) }
    var forceDarkPreview by remember { mutableStateOf<Boolean?>(null) }

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }

    // Sandbox Datasets for deep preview & polish
    var sandboxBalancedAccounts by remember {
        mutableStateOf(
            listOf(
                AccountEntity(id = 101, name = "微信钱包零钱", type = "WECHAT", balance = 3420.5, cardSuffix = "", colorHex = "#2D6A4F", note = "日常扫码买菜"),
                AccountEntity(id = 102, name = "支付宝余额宝", type = "ALIPAY", balance = 12500.0, cardSuffix = "", colorHex = "#2D6A4F", note = "7日年化 1.82%"),
                AccountEntity(id = 103, name = "随身备用现金", type = "CASH", balance = 800.0, cardSuffix = "", colorHex = "#D97706", note = "皮夹备用"),

                AccountEntity(id = 104, name = "招商银行工资卡", type = "BANK_CARD", balance = 68400.0, cardSuffix = "6821", colorHex = "#C4623D", note = "每月10号发薪"),
                AccountEntity(id = 105, name = "工商银行定期存单", type = "BANK_CARD", balance = 50000.0, cardSuffix = "0088", colorHex = "#C4623D", note = "3年期定存"),

                AccountEntity(id = 106, name = "招行经典白金信用卡", type = "CREDIT_CARD", balance = -12800.0, cardSuffix = "9012", colorHex = "#C4623D", note = "还款日每月25日"),
                AccountEntity(id = 107, name = "易方达稳健理财基金", type = "INVESTMENT", balance = 158000.0, cardSuffix = "110011", colorHex = "#2D6A4F", note = "沪深300增强")
            )
        )
    }

    var sandboxMultiAccounts by remember {
        mutableStateOf(
            listOf(
                AccountEntity(id = 201, name = "微信零钱", type = "WECHAT", balance = 2400.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 202, name = "微信零钱通", type = "WECHAT", balance = 15000.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 203, name = "支付宝余额", type = "ALIPAY", balance = 850.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 204, name = "支付宝余额宝", type = "ALIPAY", balance = 32000.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 205, name = "应急备用金", type = "CASH", balance = 3000.0, cardSuffix = "", colorHex = "#D97706"),

                AccountEntity(id = 206, name = "招商银行工资卡", type = "BANK_CARD", balance = 84000.0, cardSuffix = "6821", colorHex = "#C4623D"),
                AccountEntity(id = 207, name = "建设银行公积金卡", type = "BANK_CARD", balance = 25600.0, cardSuffix = "3399", colorHex = "#2D6A4F"),
                AccountEntity(id = 208, name = "中国银行外币借记卡", type = "BANK_CARD", balance = 28000.0, cardSuffix = "5188", colorHex = "#C4623D"),
                AccountEntity(id = 209, name = "农业银行储蓄存折", type = "BANK_CARD", balance = 42000.0, cardSuffix = "8899", colorHex = "#2D6A4F"),

                AccountEntity(id = 210, name = "招行经典白信用卡", type = "CREDIT_CARD", balance = -9600.0, cardSuffix = "9012", colorHex = "#C4623D"),
                AccountEntity(id = 211, name = "交行白麒麟信用卡", type = "CREDIT_CARD", balance = -4200.0, cardSuffix = "4311", colorHex = "#C4623D"),
                AccountEntity(id = 212, name = "浦发AE白信用卡", type = "CREDIT_CARD", balance = -3100.0, cardSuffix = "5566", colorHex = "#C4623D"),
                AccountEntity(id = 213, name = "中欧医疗健康基金", type = "INVESTMENT", balance = 68000.0, cardSuffix = "003095", colorHex = "#2D6A4F"),
                AccountEntity(id = 214, name = "实物黄金存单", type = "OTHER", balance = 45000.0, cardSuffix = "AU9999", colorHex = "#D97706")
            )
        )
    }

    var sandboxZeroDebtAccounts by remember {
        mutableStateOf(
            listOf(
                AccountEntity(id = 301, name = "微信零钱", type = "WECHAT", balance = 4520.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 302, name = "支付宝余额宝", type = "ALIPAY", balance = 28000.0, cardSuffix = "", colorHex = "#2D6A4F"),
                AccountEntity(id = 303, name = "招商银行储蓄卡", type = "BANK_CARD", balance = 120000.0, cardSuffix = "6821", colorHex = "#C4623D"),
                AccountEntity(id = 304, name = "稳健理财基金", type = "INVESTMENT", balance = 200000.0, cardSuffix = "天弘", colorHex = "#2D6A4F")
            )
        )
    }

    val currentAccounts = when (selectedScenario) {
        SwissScenario.LIVE_DATA -> realAccounts
        SwissScenario.BALANCED_MATRIX -> sandboxBalancedAccounts
        SwissScenario.MULTI_CARDS -> sandboxMultiAccounts
        SwissScenario.ZERO_DEBT -> sandboxZeroDebtAccounts
    }

    val currentTotalPositive = currentAccounts.filter { it.balance > 0 }.sumOf { it.balance }
    val currentTotalDebts = currentAccounts.filter { it.balance < 0 }.sumOf { -it.balance }
    val currentNetAssets = currentTotalPositive - currentTotalDebts

    val effectiveIsLight = forceDarkPreview?.let { !it } ?: systemBgConfig.isLight
    val effectiveBgConfig = remember(effectiveIsLight) {
        BackgroundConfig(isLight = effectiveIsLight)
    }

    // 统一与首页完全一致的优雅调色盘（消除刺眼黑色方框，增加呼吸感）
    val canvasBg = if (effectiveIsLight) Color(0xFFFAFAF7) else Color(0xFF242E24)
    val cardBg = if (effectiveIsLight) Color(0xFFFFFFFF) else Color(0xFF1E281E)
    val cylinderTrackBg = if (effectiveIsLight) Color(0xFFF3EFE8) else Color(0xFF1A221A)
    val dividerColor = if (effectiveIsLight) Color(0xFFE4DFD3) else Color(0xFF374637)
    val inkPrimary = if (effectiveIsLight) Color(0xFF141414) else Color(0xFFFAFAF7)
    val inkSecondary = if (effectiveIsLight) Color(0xFF5A5852) else Color(0xFFB5B3AA)
    val inkMuted = if (effectiveIsLight) Color(0xFF8A8780) else Color(0xFF889689)
    val clayAccent = Color(0xFFC4623D)
    val forestGreen = if (effectiveIsLight) Color(0xFF2D6A4F) else Color(0xFF52B788)
    val warningAmber = Color(0xFFD97706)

    CompositionLocalProvider(LocalAppBackgroundConfig provides effectiveBgConfig) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(canvasBg)
        ) {
            // 1. Swiss Editorial Top Bar: Scene & Mode Control
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(canvasBg)
                    .padding(horizontal = 22.dp, vertical = 12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SWISS CYLINDER",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "· 资产卡包",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = inkMuted
                            )
                        }

                        // Light / Dark Switch (与首页一致的胶囊 pill 风格)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (effectiveIsLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { forceDarkPreview = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LightMode,
                                    contentDescription = "Cream Edition",
                                    tint = if (effectiveIsLight) clayAccent else inkMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            IconButton(
                                onClick = { forceDarkPreview = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DarkMode,
                                    contentDescription = "Forest Edition",
                                    tint = if (!effectiveIsLight) clayAccent else inkMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Scenario Filter Chips (柔和轻质胶囊)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SwissScenario.values().forEach { sc ->
                            val isSelected = selectedScenario == sc
                            val chipBg = if (isSelected) inkPrimary else if (effectiveIsLight) Color(0xFFF0ECE1) else Color(0xFF1B231B)
                            val chipText = if (isSelected) canvasBg else inkSecondary

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(chipBg)
                                    .clickable { selectedScenario = sc }
                                    .padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = sc.icon,
                                    contentDescription = null,
                                    tint = chipText,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = sc.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = chipText
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // 2. Main Body: Polished Swiss Cylinder Experience
            SwissCylinderPolishedScreen(
                accounts = currentAccounts,
                totalNetAssets = currentNetAssets,
                totalPositiveAssets = currentTotalPositive,
                totalDebts = currentTotalDebts,
                isLight = effectiveIsLight,
                canvasBg = canvasBg,
                cardBg = cardBg,
                cylinderTrackBg = cylinderTrackBg,
                dividerColor = dividerColor,
                inkPrimary = inkPrimary,
                inkSecondary = inkSecondary,
                inkMuted = inkMuted,
                clayAccent = clayAccent,
                forestGreen = forestGreen,
                warningAmber = warningAmber,
                onAddAccountClick = {
                    accountToEdit = null
                    showAddDialog = true
                },
                onEditAccount = { acc ->
                    accountToEdit = acc
                    showAddDialog = true
                },
                onDeleteAccount = { acc ->
                    if (selectedScenario == SwissScenario.LIVE_DATA) {
                        onDeleteAccount(acc)
                    } else {
                        when (selectedScenario) {
                            SwissScenario.BALANCED_MATRIX -> sandboxBalancedAccounts = sandboxBalancedAccounts.filter { it.id != acc.id }
                            SwissScenario.MULTI_CARDS -> sandboxMultiAccounts = sandboxMultiAccounts.filter { it.id != acc.id }
                            SwissScenario.ZERO_DEBT -> sandboxZeroDebtAccounts = sandboxZeroDebtAccounts.filter { it.id != acc.id }
                            else -> {}
                        }
                    }
                }
            )
        }
    }

    if (showAddDialog) {
        SwissAddEditAccountDialog(
            accountToEdit = accountToEdit,
            isLight = effectiveIsLight,
            canvasBg = canvasBg,
            cardBg = cardBg,
            inkPrimary = inkPrimary,
            inkSecondary = inkSecondary,
            inkMuted = inkMuted,
            dividerColor = dividerColor,
            clayAccent = clayAccent,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, balance, suffix, color, saveAsMissedRecord, oldBalance, note ->
                if (accountToEdit == null) {
                    if (selectedScenario == SwissScenario.LIVE_DATA) {
                        onAddAccount(name, type, balance, suffix, color, note)
                    } else {
                        val newAcc = AccountEntity(
                            id = System.currentTimeMillis(),
                            name = name,
                            type = type,
                            balance = balance,
                            cardSuffix = suffix,
                            colorHex = color,
                            note = note
                        )
                        when (selectedScenario) {
                            SwissScenario.BALANCED_MATRIX -> sandboxBalancedAccounts = sandboxBalancedAccounts + newAcc
                            SwissScenario.MULTI_CARDS -> sandboxMultiAccounts = sandboxMultiAccounts + newAcc
                            SwissScenario.ZERO_DEBT -> sandboxZeroDebtAccounts = sandboxZeroDebtAccounts + newAcc
                            else -> {}
                        }
                    }
                } else {
                    val updated = accountToEdit!!.copy(
                        name = name,
                        type = type,
                        balance = balance,
                        cardSuffix = suffix,
                        colorHex = color,
                        note = note
                    )
                    if (selectedScenario == SwissScenario.LIVE_DATA) {
                        onUpdateAccount(updated, saveAsMissedRecord, oldBalance)
                    } else {
                        when (selectedScenario) {
                            SwissScenario.BALANCED_MATRIX -> sandboxBalancedAccounts = sandboxBalancedAccounts.map { if (it.id == updated.id) updated else it }
                            SwissScenario.MULTI_CARDS -> sandboxMultiAccounts = sandboxMultiAccounts.map { if (it.id == updated.id) updated else it }
                            SwissScenario.ZERO_DEBT -> sandboxZeroDebtAccounts = sandboxZeroDebtAccounts.map { if (it.id == updated.id) updated else it }
                            else -> {}
                        }
                    }
                }
                showAddDialog = false
            }
        )
    }
}

/**
 * 瑞士极简圆柱滚筒打磨核心视图（去多余方框，融入通透排版、可滚动资产区域与平滑数字滚动动画）
 */
@Composable
fun SwissCylinderPolishedScreen(
    accounts: List<AccountEntity>,
    totalNetAssets: Double,
    totalPositiveAssets: Double,
    totalDebts: Double,
    isLight: Boolean,
    canvasBg: Color,
    cardBg: Color,
    cylinderTrackBg: Color,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    clayAccent: Color,
    forestGreen: Color,
    warningAmber: Color,
    onAddAccountClick: () -> Unit,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val liquidAccounts = accounts.filter { it.type == "WECHAT" || it.type == "ALIPAY" || it.type == "CASH" }
    val savingsAccounts = accounts.filter { it.type == "BANK_CARD" && it.balance >= 0 }
    val creditInvestAccounts = accounts.filter { it.type == "CREDIT_CARD" || it.type == "INVESTMENT" || it.type == "OTHER" || it.balance < 0 }

    // 默认全收起，用户点击单个卡包选中后展开该卡包
    var expandedPocketId by remember { mutableStateOf<String?>(null) }

    // 总资产数额平滑滚动动画 (Rolling Counter Animation)
    val animatedNetAssets by animateFloatAsState(
        targetValue = totalNetAssets.toFloat(),
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "animated_net_assets"
    )
    val animatedPositiveAssets by animateFloatAsState(
        targetValue = totalPositiveAssets.toFloat(),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animated_pos_assets"
    )
    val animatedDebts by animateFloatAsState(
        targetValue = totalDebts.toFloat(),
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animated_debts"
    )

    // 全局统一滚动容器：总资产区域与卡包列表共同滚动
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. 可滑动的 Swiss Master Hero Net Asset Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(canvasBg)
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                // Header Meta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NET ASSET LEDGER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = inkMuted,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "总核算净资产",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = inkSecondary
                        )
                    }

                    // Open New Account 按钮 (取消胶囊背景，纯净优雅文字图标排版)
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp)
                            ) { onAddAccountClick() }
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增账户",
                            tint = inkPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "新增账户",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hero Net Asset Typography 带平滑滚动动画
                val netCents = AmountFormatter.yuanToCents(animatedNetAssets.toDouble())
                val netFormatted = AmountFormatter.formatCentsAsYuan(abs(netCents))
                val isNegativeNet = animatedNetAssets < 0

                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "¥",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 30.sp,
                        color = inkMuted,
                        modifier = Modifier.padding(bottom = 6.dp, end = 6.dp)
                    )
                    Text(
                        text = "${if (isNegativeNet) "-" else ""}$netFormatted",
                        fontFamily = FontFamily.Serif,
                        fontStyle = FontStyle.Italic,
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (isNegativeNet) clayAccent else inkPrimary,
                        letterSpacing = (-1.6).sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sub Ledger Row & Accent Bar (POS & NEG Breakdown 带滚动动画)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .height(2.5.dp)
                            .width(46.dp)
                            .background(clayAccent)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "实有 ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = inkMuted
                            )
                            val posCents = AmountFormatter.yuanToCents(animatedPositiveAssets.toDouble())
                            Text(
                                text = "¥${AmountFormatter.formatCentsAsYuan(posCents)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = forestGreen
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "负债 ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = inkMuted
                            )
                            val debtCents = AmountFormatter.yuanToCents(animatedDebts.toDouble())
                            Text(
                                text = if (debtCents > 0) "-¥${AmountFormatter.formatCentsAsYuan(debtCents)}" else "¥0.00",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (debtCents > 0) clayAccent else inkSecondary
                            )
                        }
                    }
                }
            }

            // Full Width Edge-to-Edge Divider
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        }

        // Section Gesture & Status Prompt
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(clayAccent)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "点击卡包展开 · 上下拖拽滚轮翻卡",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = inkMuted
                    )
                }
                Text(
                    text = "3 POCKETS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = inkMuted
                )
            }
        }

        // Pocket 01: Liquid Assets (流动资金滚筒)
        item {
            Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                SwissPolishedCylinderPocketCard(
                    pocketId = "pocket_01",
                    pocketNumber = "01",
                    pocketTag = "LIQUIDITY",
                    pocketTitle = "日常流动卡包",
                    accounts = liquidAccounts,
                    isExpanded = expandedPocketId == "pocket_01",
                    onToggleExpand = {
                        expandedPocketId = if (expandedPocketId == "pocket_01") null else "pocket_01"
                    },
                    isLight = isLight,
                    cardBg = cardBg,
                    trackBg = cylinderTrackBg,
                    dividerColor = dividerColor,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    inkMuted = inkMuted,
                    accentColor = forestGreen,
                    clayAccent = clayAccent,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }

        // Pocket 02: Core Deposits (储蓄底仓滚筒)
        item {
            Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                SwissPolishedCylinderPocketCard(
                    pocketId = "pocket_02",
                    pocketNumber = "02",
                    pocketTag = "CAPITAL BASE",
                    pocketTitle = "储蓄底仓卡包",
                    accounts = savingsAccounts,
                    isExpanded = expandedPocketId == "pocket_02",
                    onToggleExpand = {
                        expandedPocketId = if (expandedPocketId == "pocket_02") null else "pocket_02"
                    },
                    isLight = isLight,
                    cardBg = cardBg,
                    trackBg = cylinderTrackBg,
                    dividerColor = dividerColor,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    inkMuted = inkMuted,
                    accentColor = Color(0xFF2563EB),
                    clayAccent = clayAccent,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }

        // Pocket 03: Credit & Investment (信用与理财滚筒)
        item {
            Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                SwissPolishedCylinderPocketCard(
                    pocketId = "pocket_03",
                    pocketNumber = "03",
                    pocketTag = "CREDIT & ASSET",
                    pocketTitle = "信用与投资卡包",
                    accounts = creditInvestAccounts,
                    isExpanded = expandedPocketId == "pocket_03",
                    onToggleExpand = {
                        expandedPocketId = if (expandedPocketId == "pocket_03") null else "pocket_03"
                    },
                    isLight = isLight,
                    cardBg = cardBg,
                    trackBg = cylinderTrackBg,
                    dividerColor = dividerColor,
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    inkMuted = inkMuted,
                    accentColor = if (creditInvestAccounts.any { it.balance < 0 }) clayAccent else Color(0xFF7C3AED),
                    clayAccent = clayAccent,
                    onEditAccount = onEditAccount,
                    onDeleteAccount = onDeleteAccount
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * 深度打磨版：单个瑞士极简圆柱滚筒卡包
 * 
 * 全新滚动物理引擎特性：
 * 1. 【连续虚拟滚轴物理模型】：基于连续浮点位移 scrollPosition，支持无限循环平滑转盘与最近卡位 Spring 吸附；
 * 2. 【多层 3D 圆柱曲率与景深衰减】：每一张卡片根据距视口中心的连续相对距离（itemFraction），精确计算 3D 旋转角度、垂直位移、动态缩放与淡入淡出透明度；
 * 3. 【幽灵卡片即点即切】：点击上方或下方幽灵卡片直接平滑旋转到中心焦点；
 * 4. 【快速步进器同步】：底部「▲ 上一张」「▼ 下一张」按钮与手势转轴完全联动。
 */
@Composable
fun SwissPolishedCylinderPocketCard(
    pocketId: String,
    pocketNumber: String,
    pocketTag: String,
    pocketTitle: String,
    accounts: List<AccountEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    isLight: Boolean,
    cardBg: Color,
    trackBg: Color,
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    accentColor: Color,
    clayAccent: Color,
    onEditAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollPos = remember { Animatable(0f) }
    val density = LocalDensity.current.density
    val itemSlotPx = 54f * density

    val safeCount = accounts.size
    val totalBalance = accounts.sumOf { it.balance }

    val expandIconRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "expand_icon_rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(
                width = if (isExpanded) 1.dp else 0.6.dp,
                color = if (isExpanded) inkPrimary.copy(alpha = 0.4f) else dividerColor.copy(alpha = 0.7f),
                shape = RoundedCornerShape(16.dp)
            )
            .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.85f))
    ) {
        // Pocket Header Bar (可点击触发展开/折叠)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isExpanded) inkPrimary else if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pocketNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) cardBg else inkPrimary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = pocketTitle,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkPrimary
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = pocketTag,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = inkMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    val cents = AmountFormatter.yuanToCents(totalBalance)
                    Text(
                        text = "${if (totalBalance < 0) "-" else ""}¥${AmountFormatter.formatCentsAsYuan(cents)}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (totalBalance < 0) clayAccent else inkPrimary
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "$safeCount 张卡",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = inkMuted
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起卡包" else "展开卡包",
                    tint = inkMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = expandIconRotation
                        }
                )
            }
        }

        // 展开后的滚筒内容体
        if (isExpanded) {
            HorizontalDivider(color = dividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(88.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(trackBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[该卡包暂无收纳账户]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = inkMuted
                        )
                    }
                } else if (safeCount == 1) {
                    // 仅单张账户：展示居中精致卡片
                    val singleAcc = accounts[0]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(86.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(trackBg)
                            .border(0.5.dp, dividerColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(68.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(cardBg)
                                .border(0.8.dp, dividerColor, RoundedCornerShape(10.dp))
                                .clickable { onEditAccount(singleAcc) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(accentColor)
                                        )
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = singleAcc.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = inkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = if (singleAcc.cardSuffix.isNotBlank()) "CARD •••• ${singleAcc.cardSuffix} · 点击编辑" else "${singleAcc.type} · 点击编辑",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = inkMuted
                                    )
                                }

                                val cents = AmountFormatter.yuanToCents(singleAcc.balance)
                                Text(
                                    text = "${if (singleAcc.balance < 0) "-" else ""}¥${AmountFormatter.formatCentsAsYuan(cents)}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (singleAcc.balance < 0) clayAccent else inkPrimary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLOT 1 / 1 · 单卡直显",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = inkMuted
                        )
                    }
                } else {
                    // 多张账户：重写连续圆柱转轴物理逻辑
                    val currentFloat = scrollPos.value
                    val baseIndex = currentFloat.roundToInt()
                    val subOffset = currentFloat - baseIndex // [-0.5, +0.5]

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(146.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(trackBg)
                            .border(0.5.dp, dividerColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .pointerInput(safeCount) {
                                coroutineScope {
                                    val velocityTracker = VelocityTracker()
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            velocityTracker.resetTracking()
                                        },
                                        onDragEnd = {
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            // 计算每秒滚过的卡片插槽速度
                                            val flingIndexVelocity = -velocityY / itemSlotPx
                                            val currentPos = scrollPos.value

                                            // 惯性动力学投射：根据滑动初速度预测惯性滚动的落点卡片
                                            val speed = abs(flingIndexVelocity)
                                            val projectedCards = when {
                                                speed > 10f -> sign(flingIndexVelocity) * 3f
                                                speed > 5f -> sign(flingIndexVelocity) * 2f
                                                speed > 1.8f -> sign(flingIndexVelocity) * 1f
                                                else -> 0f
                                            }

                                            val targetIndex = (currentPos + projectedCards).roundToInt()
                                            val springStiffness = when {
                                                speed > 6f -> Spring.StiffnessLow
                                                speed > 2f -> Spring.StiffnessMediumLow
                                                else -> Spring.StiffnessMedium
                                            }

                                            launch {
                                                scrollPos.animateTo(
                                                    targetValue = targetIndex.toFloat(),
                                                    initialVelocity = flingIndexVelocity * 0.65f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.82f,
                                                        stiffness = springStiffness
                                                    )
                                                )
                                            }
                                        },
                                        onDragCancel = {
                                            launch {
                                                val target = scrollPos.value.roundToInt().toFloat()
                                                scrollPos.animateTo(
                                                    targetValue = target,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.82f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        },
                                        onVerticalDrag = { change, dragAmount ->
                                            change.consume()
                                            velocityTracker.addPosition(change.uptimeMillis, change.position)
                                            launch {
                                                // 向下拉（dragAmount > 0）-> 切换至上一张（scrollPos 减少）
                                                // 向上推（dragAmount < 0）-> 切换至下一张（scrollPos 增加）
                                                val deltaIndex = -dragAmount / itemSlotPx
                                                scrollPos.snapTo(scrollPos.value + deltaIndex)
                                            }
                                        }
                                    )
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // 动态渲染可视槽位（包含远端 -2 和 +2 槽位，确保高速惯性滚动与边缘旋转依然连续自然）
                        val slotsToRender = listOf(-2, 2, -1, 1, 0).sortedByDescending { abs(it - subOffset) }
                        for (slotOffset in slotsToRender) {
                            val accIndex = ((baseIndex + slotOffset) % safeCount + safeCount) % safeCount
                            val acc = accounts[accIndex]
                            val itemFraction = slotOffset - subOffset // 相对中心的精确连续距离
                            val distance = abs(itemFraction)

                            if (distance > 2.1f) continue

                            // 连续 3D 滚轴物理变换
                            val translateY = itemFraction * 44f * density
                            val rotX = (-itemFraction * 20f).coerceIn(-45f, 45f)
                            val scale = (1f - distance * 0.06f).coerceIn(0.80f, 1f)
                            
                            // 重叠淡入淡出曲线：在上一张卡片淡出消失前，下一张卡片提前大幅度淡入渲染，确保视觉平滑连贯
                            val itemAlpha = (1f - (distance * 0.48f)).coerceIn(0f, 1f)
                            if (itemAlpha <= 0.02f) continue
                            
                            val isCenterFocused = distance <= 0.38f

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .zIndex(10f - distance)
                                    .graphicsLayer {
                                        this.translationY = translateY
                                        this.rotationX = rotX
                                        this.scaleX = scale
                                        this.scaleY = scale
                                        this.alpha = itemAlpha
                                        this.cameraDistance = 14f * density
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCenterFocused) cardBg else cardBg.copy(alpha = 0.85f))
                                    .border(
                                        width = if (isCenterFocused) 0.8.dp else 0.5.dp,
                                        color = if (isCenterFocused) dividerColor else dividerColor.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        if (isCenterFocused) {
                                            onEditAccount(acc)
                                        } else {
                                            // 点击幽灵卡片提前平滑旋转进入中心舞台
                                            coroutineScope.launch {
                                                scrollPos.animateTo(
                                                    targetValue = (baseIndex + slotOffset).toFloat(),
                                                    animationSpec = spring(
                                                        dampingRatio = 0.82f,
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(accentColor)
                                            )
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text(
                                                text = acc.name,
                                                fontSize = 15.sp,
                                                fontWeight = if (isCenterFocused) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isCenterFocused) inkPrimary else inkSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isCenterFocused) {
                                                if (acc.cardSuffix.isNotBlank()) "CARD •••• ${acc.cardSuffix} · 点击编辑" else "${acc.type} · 点击编辑"
                                            } else {
                                                if (acc.cardSuffix.isNotBlank()) "•••• ${acc.cardSuffix} · 点击对齐" else "点击对齐"
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            color = inkMuted
                                        )
                                    }

                                    val cents = AmountFormatter.yuanToCents(acc.balance)
                                    Text(
                                        text = "${if (acc.balance < 0) "-" else ""}¥${AmountFormatter.formatCentsAsYuan(cents)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = if (isCenterFocused) 18.sp else 16.sp,
                                        fontWeight = if (isCenterFocused) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (acc.balance < 0) clayAccent else if (isCenterFocused) inkPrimary else inkSecondary
                                    )
                                }
                            }
                        }

                        // 顶底两端柔和淡入淡出遮罩
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(trackBg, trackBg.copy(alpha = 0f))
                                    )
                                )
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(trackBg.copy(alpha = 0f), trackBg)
                                    )
                                )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Footer Step Indicator & Quick Arrow Steppers (纯净连动步进器)
                    val activeSlot = ((baseIndex % safeCount) + safeCount) % safeCount + 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SLOT $activeSlot / $safeCount · 滚轮转盘",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.5.sp,
                            color = inkMuted
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                    .clickable {
                                        coroutineScope.launch {
                                            scrollPos.animateTo(
                                                targetValue = (scrollPos.value.roundToInt() - 1).toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = 0.78f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "▲ 上一张",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = inkPrimary
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                    .clickable {
                                        coroutineScope.launch {
                                            scrollPos.animateTo(
                                                targetValue = (scrollPos.value.roundToInt() + 1).toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = 0.78f,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            )
                                        }
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "▼ 下一张",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = inkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 瑞士风格新建 / 编辑账户弹窗 (与整体首页一致的圆角弹窗风格)
 */
@Composable
fun SwissAddEditAccountDialog(
    accountToEdit: AccountEntity?,
    isLight: Boolean,
    canvasBg: Color,
    cardBg: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    dividerColor: Color,
    clayAccent: Color,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, balance: Double, suffix: String, color: String, saveAsMissedRecord: Boolean, oldBalance: Double, note: String) -> Unit
) {
    var name by remember { mutableStateOf(accountToEdit?.name ?: "") }
    var type by remember { mutableStateOf(accountToEdit?.type ?: "BANK_CARD") }
    var balanceText by remember { mutableStateOf(accountToEdit?.let { String.format(Locale.US, "%.2f", it.balance) } ?: "") }
    var suffix by remember { mutableStateOf(accountToEdit?.cardSuffix ?: "") }
    var note by remember { mutableStateOf(accountToEdit?.note ?: "") }
    var colorHex by remember { mutableStateOf(accountToEdit?.colorHex ?: "#2D6A4F") }
    var saveAsMissedRecord by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = androidx.compose.foundation.BorderStroke(1.dp, dividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(22.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (accountToEdit == null) "NEW ACCOUNT / 开立新卡" else "EDIT ACCOUNT / 账户调账",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = inkPrimary,
                        letterSpacing = 0.8.sp
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(26.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = inkMuted, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider(color = dividerColor.copy(alpha = 0.6f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 12.dp))

                // Account Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("账户名称 (如: 招行工资卡 / 微信零钱)", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inkPrimary,
                        unfocusedBorderColor = dividerColor,
                        focusedTextColor = inkPrimary,
                        unfocusedTextColor = inkPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Account Type Selector
                Text(text = "账户类型 (分类进卡包)", fontFamily = FontFamily.Monospace, fontSize = 10.5.sp, color = inkMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val types = listOf(
                        "WECHAT" to "微信",
                        "ALIPAY" to "支付宝",
                        "BANK_CARD" to "储蓄卡",
                        "CREDIT_CARD" to "信用卡",
                        "INVESTMENT" to "理财",
                        "CASH" to "现金"
                    )
                    types.forEach { (tKey, tLabel) ->
                        val isSelected = type == tKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) inkPrimary else if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                .clickable { type = tKey }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tLabel,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) cardBg else inkSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Balance
                OutlinedTextField(
                    value = balanceText,
                    onValueChange = { balanceText = it },
                    label = { Text("当前账户余额 (信用卡欠款可输入负数)", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inkPrimary,
                        unfocusedBorderColor = dividerColor,
                        focusedTextColor = inkPrimary,
                        unfocusedTextColor = inkPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Suffix & Note
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = suffix,
                        onValueChange = { if (it.length <= 4) suffix = it },
                        label = { Text("卡号后4位", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = inkPrimary,
                            unfocusedBorderColor = dividerColor,
                            focusedTextColor = inkPrimary,
                            unfocusedTextColor = inkPrimary
                        )
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注标签", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                        singleLine = true,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = inkPrimary,
                            unfocusedBorderColor = dividerColor,
                            focusedTextColor = inkPrimary,
                            unfocusedTextColor = inkPrimary
                        )
                    )
                }

                // Balance Diff Record Toggle (if editing)
                if (accountToEdit != null) {
                    val newBal = balanceText.toDoubleOrNull() ?: accountToEdit.balance
                    val diff = newBal - accountToEdit.balance
                    if (abs(diff) > 0.009) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isLight) Color(0xFFF6F4EE) else Color(0xFF192219))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "差额 (${if (diff > 0) "+" else ""}${String.format(Locale.US, "%.2f", diff)}) 记为漏记收支",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = inkPrimary
                                )
                                Text(
                                    text = "自动补齐一条流水记录",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = inkMuted
                                )
                            }
                            Switch(
                                checked = saveAsMissedRecord,
                                onCheckedChange = { saveAsMissedRecord = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = inkPrimary, checkedTrackColor = inkPrimary.copy(alpha = 0.3f))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
                        onConfirm(
                            if (name.isBlank()) "未命名账户" else name,
                            type,
                            parsedBalance,
                            suffix,
                            colorHex,
                            saveAsMissedRecord,
                            accountToEdit?.balance ?: 0.0,
                            note
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = inkPrimary, contentColor = cardBg)
                ) {
                    Text(
                        text = if (accountToEdit == null) "CONFIRM / 建立并存入卡包" else "UPDATE / 保存调账变更",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
