package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.calculateTargetValue
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.data.local.AccountEntity
import com.example.data.local.ExpenseEntity
import com.example.model.AmountFormatter
import com.example.ui.theme.LocalAppBackgroundConfig
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 卡包分类定义数据模型
 */
enum class AccountCategoryType(
    val categoryKey: String,
    val title: String,
    val tag: String,
    val defaultColor: Color
) {
    CASH("CASH", "现金", "CASH", Color(0xFF2D6A4F)),
    CREDIT_CARD("CREDIT_CARD", "信用卡", "CREDIT CARD", Color(0xFFC4623D)),
    DEBIT_CARD("DEBIT_CARD", "储蓄卡", "DEBIT CARD", Color(0xFF2563EB)),
    DIGITAL_WALLET("DIGITAL_WALLET", "网络账户", "DIGITAL WALLET", Color(0xFF0D9488)),
    INVESTMENT("INVESTMENT", "投资账户", "INVESTMENT", Color(0xFF7C3AED)),
    STORED_VALUE("STORED_VALUE", "储值卡", "STORED VALUE", Color(0xFFD97706))
}

/**
 * 预设银行列表（用于信用卡与储蓄卡选择，避免用户手动输错）
 */
val PRESET_BANKS = listOf(
    "招商银行",
    "工商银行",
    "建设银行",
    "中国银行",
    "农业银行",
    "交通银行",
    "中信银行",
    "浦发银行",
    "民生银行",
    "光大银行",
    "兴业银行",
    "平安银行",
    "广发银行",
    "华夏银行",
    "邮政储蓄银行",
    "上海银行",
    "北京银行",
    "宁波银行",
    "汇丰银行",
    "渣打银行",
    "其他银行"
)

/**
 * 预设网络账户列表
 */
val PRESET_DIGITAL_WALLETS = listOf(
    "支付宝",
    "微信钱包",
    "QQ钱包",
    "其他网络账户"
)

/**
 * 推荐投资账户快捷标签
 */
val PRESET_INVESTMENTS = listOf(
    "天天基金",
    "华泰证券",
    "理财通",
    "富途证券",
    "国泰君安",
    "东方财富",
    "中信证券"
)

/**
 * 推荐储值卡快捷标签
 */
val PRESET_STORED_VALUES = listOf(
    "星巴克星礼卡",
    "山姆会员卡",
    "商超购物卡",
    "公交地铁卡",
    "盒马礼品卡",
    "健身房年卡",
    "瑞幸咖啡卡"
)

/**
 * 账户页面卡包聚合数据
 */
data class AccountPocketGroup(
    val category: AccountCategoryType,
    val accounts: List<AccountEntity>,
    val usageCount: Int
)

/**
 * 瑞士极简圆柱滚筒资产账户页 (正式版)
 */
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

    // 调色盘（平滑主题过渡）
    val themeAnimSpec = tween<Color>(durationMillis = 400, easing = FastOutSlowInEasing)
    val canvasBg by animateColorAsState(if (isLight) Color(0xFFFAFAF7) else Color(0xFF242E24), animationSpec = themeAnimSpec, label = "canvasBg")
    val cardBg by animateColorAsState(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1E281E), animationSpec = themeAnimSpec, label = "cardBg")
    val dividerColor by animateColorAsState(if (isLight) Color(0xFFE4DFD3) else Color(0xFF374637), animationSpec = themeAnimSpec, label = "dividerColor")
    val inkPrimary by animateColorAsState(if (isLight) Color(0xFF141414) else Color(0xFFFAFAF7), animationSpec = themeAnimSpec, label = "inkPrimary")
    val inkSecondary by animateColorAsState(if (isLight) Color(0xFF5A5852) else Color(0xFFB5B3AA), animationSpec = themeAnimSpec, label = "inkSecondary")
    val inkMuted by animateColorAsState(if (isLight) Color(0xFF8A8780) else Color(0xFF889689), animationSpec = themeAnimSpec, label = "inkMuted")
    val clayAccent = Color(0xFFC4623D)
    val forestGreen by animateColorAsState(if (isLight) Color(0xFF2D6A4F) else Color(0xFF52B788), animationSpec = themeAnimSpec, label = "forestGreen")

    var showAddDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    // 统计各账户使用频率
    val pocketUsageMap = remember(accounts, expenses) {
        accounts.associate { acc ->
            val count = expenses.count { it.accountId == acc.id || it.transferToAccountId == acc.id }
            acc.id to count
        }
    }

    // 账户映射到对应卡包分类
    val pockets: List<AccountPocketGroup> = remember(accounts, pocketUsageMap) {
        val cashList = accounts.filter { it.type == "CASH" }
        val creditList = accounts.filter { it.type == "CREDIT_CARD" }
        val debitList = accounts.filter { it.type == "BANK_CARD" || it.type == "DEBIT_CARD" }
        val digitalList = accounts.filter {
            it.type == "WECHAT" || it.type == "ALIPAY" || it.type == "QQ" || it.type == "DIGITAL_WALLET" || it.type == "ONLINE"
        }
        val investList = accounts.filter { it.type == "INVESTMENT" }
        val storedValueList = accounts.filter { it.type == "STORED_VALUE" || it.type == "PREPAID" || it.type == "OTHER" }

        val rawGroups = listOf(
            AccountPocketGroup(AccountCategoryType.CASH, cashList, cashList.sumOf { pocketUsageMap[it.id] ?: 0 }),
            AccountPocketGroup(AccountCategoryType.CREDIT_CARD, creditList, creditList.sumOf { pocketUsageMap[it.id] ?: 0 }),
            AccountPocketGroup(AccountCategoryType.DEBIT_CARD, debitList, debitList.sumOf { pocketUsageMap[it.id] ?: 0 }),
            AccountPocketGroup(AccountCategoryType.DIGITAL_WALLET, digitalList, digitalList.sumOf { pocketUsageMap[it.id] ?: 0 }),
            AccountPocketGroup(AccountCategoryType.INVESTMENT, investList, investList.sumOf { pocketUsageMap[it.id] ?: 0 }),
            AccountPocketGroup(AccountCategoryType.STORED_VALUE, storedValueList, storedValueList.sumOf { pocketUsageMap[it.id] ?: 0 })
        )

        // 规则1：没有账户的类别不要创建卡包
        // 规则2：卡包显示顺序按使用频率从上往下
        rawGroups
            .filter { it.accounts.isNotEmpty() }
            .sortedWith(
                compareByDescending<AccountPocketGroup> { it.usageCount }
                    .thenByDescending { it.accounts.size }
            )
    }

    // 展开卡包状态
    var expandedPocketKey by remember { mutableStateOf<String?>(null) }

    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(60)
        startAnim = true
    }

    // 滚轮总资产平滑滚动动画
    val animatedNetAssets by animateFloatAsState(
        targetValue = if (startAnim) totalNetAssets.toFloat() else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "animated_net_assets"
    )
    val animatedPositiveAssets by animateFloatAsState(
        targetValue = if (startAnim) totalPositiveAssets.toFloat() else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animated_pos_assets"
    )
    val animatedDebts by animateFloatAsState(
        targetValue = if (startAnim) totalDebts.toFloat() else 0f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
        label = "animated_debts"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(canvasBg)
            .statusBarsPadding()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. 顶部 Header (参照首页顶部 Ledger 字体与排版)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Account",
                            fontFamily = FontFamily.Serif,
                            fontStyle = FontStyle.Italic,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Normal,
                            color = inkPrimary,
                            letterSpacing = (-0.5).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "NET ASSET LEDGER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = inkMuted,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // 新增账户：删除背景色，保持极简纯粹字体与图标
                    Row(
                        modifier = Modifier
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(bounded = false, radius = 24.dp)
                            ) {
                                accountToEdit = null
                                showAddDialog = true
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp)
                            .testTag("add_account_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "新增账户",
                            tint = inkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "新增账户",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkPrimary
                        )
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 2. Swiss Master Hero Net Asset Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(canvasBg)
                        .padding(horizontal = 22.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "总核算净资产",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = inkSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

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

                    // Sub Ledger Row & Accent Bar
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

                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            }

            // 3. Pocket Count Row: 删除了手势提示，pocket数移到左边
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${pockets.size} POCKETS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = inkMuted,
                        letterSpacing = 1.sp
                    )
                }
            }

            // 4. 卡包列表渲染（按使用频率排序，空类别自动过滤）
            if (pockets.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "暂无活跃卡包",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "点击右上角「新增账户」开立银行卡、网络账户或现金卡包",
                                fontSize = 12.sp,
                                color = inkMuted
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(
                    items = pockets,
                    key = { _, group -> group.category.categoryKey }
                ) { index, group ->
                    val pocketNumStr = String.format(Locale.US, "%02d", index + 1)
                    val isExpanded = expandedPocketKey == group.category.categoryKey

                    Box(modifier = Modifier.padding(horizontal = 22.dp)) {
                        SwissPolishedCylinderPocketCard(
                            pocketId = group.category.categoryKey,
                            pocketNumber = pocketNumStr,
                            pocketTag = group.category.tag,
                            pocketTitle = group.category.title,
                            accounts = group.accounts,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedPocketKey = if (isExpanded) null else group.category.categoryKey
                            },
                            isLight = isLight,
                            cardBg = cardBg,
                            dividerColor = dividerColor,
                            inkPrimary = inkPrimary,
                            inkSecondary = inkSecondary,
                            inkMuted = inkMuted,
                            accentColor = group.category.defaultColor,
                            clayAccent = clayAccent,
                            onEditAccount = { acc ->
                                accountToEdit = acc
                                showAddDialog = true
                            }
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 新增 / 编辑账户弹窗
    if (showAddDialog) {
        SwissAddEditAccountDialog(
            accountToEdit = accountToEdit,
            isLight = isLight,
            cardBg = cardBg,
            inkPrimary = inkPrimary,
            inkSecondary = inkSecondary,
            inkMuted = inkMuted,
            dividerColor = dividerColor,
            clayAccent = clayAccent,
            onDismiss = { showAddDialog = false },
            onDeleteClick = { acc ->
                accountToDelete = acc
            },
            onConfirm = { name, type, balance, suffix, color, saveAsMissedRecord, oldBalance, note ->
                if (accountToEdit == null) {
                    onAddAccount(name, type, balance, suffix, color, note)
                } else {
                    val updated = accountToEdit!!.copy(
                        name = name,
                        type = type,
                        balance = balance,
                        cardSuffix = suffix,
                        colorHex = color,
                        note = note
                    )
                    onUpdateAccount(updated, saveAsMissedRecord, oldBalance)
                }
                showAddDialog = false
            }
        )
    }

    // 删除确认弹窗
    if (accountToDelete != null) {
        val targetAcc = accountToDelete!!
        AlertDialog(
            onDismissRequest = { accountToDelete = null },
            title = {
                Text(
                    text = "确认删除账户",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = inkPrimary
                )
            },
            text = {
                Text(
                    text = "确定要删除账户「${targetAcc.name}」吗？已关联的历史账单将保留，但不再参与此卡统计。",
                    fontSize = 13.sp,
                    color = inkSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(targetAcc)
                        accountToDelete = null
                        showAddDialog = false
                    }
                ) {
                    Text(text = "删除", color = clayAccent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { accountToDelete = null }) {
                    Text(text = "取消", color = inkMuted)
                }
            },
            containerColor = cardBg,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

/**
 * 单个瑞士极简圆柱滚筒卡包
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
    dividerColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    accentColor: Color,
    clayAccent: Color,
    onEditAccount: (AccountEntity) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollPos = remember { Animatable(0f) }
    val decaySpec = rememberSplineBasedDecay<Float>()
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
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = inkMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            rotationZ = expandIconRotation
                        }
                )
            }
        }

        // 展开后的滚筒内容体（纯净聚焦卡片本身，无多余边框与底部辅助栏）
        if (isExpanded) {
            HorizontalDivider(color = dividerColor.copy(alpha = 0.5f), thickness = 0.5.dp)

            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "[暂无收纳账户]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = inkMuted
                        )
                    }
                } else if (safeCount == 1) {
                    // 仅单张账户：直接展示精致卡片
                    val singleAcc = accounts[0]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(70.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(cardBg)
                            .border(0.7.dp, dividerColor, RoundedCornerShape(10.dp))
                            .clickable { onEditAccount(singleAcc) }
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
                                        text = singleAcc.name,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (singleAcc.cardSuffix.isNotBlank()) "CARD •••• ${singleAcc.cardSuffix}" else singleAcc.type,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = inkMuted
                                )
                            }

                            val cents = AmountFormatter.yuanToCents(singleAcc.balance)
                            Text(
                                text = "${if (singleAcc.balance < 0) "-" else ""}¥${AmountFormatter.formatCentsAsYuan(cents)}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (singleAcc.balance < 0) clayAccent else inkPrimary
                            )
                        }
                    }
                } else {
                    // 多张账户：瑞士极简连续圆柱转轴物理逻辑
                    val baseIndex by remember { derivedStateOf { scrollPos.value.roundToInt() } }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(146.dp)
                            .pointerInput(safeCount) {
                                coroutineScope {
                                    val velocityTracker = VelocityTracker()
                                    detectVerticalDragGestures(
                                        onDragStart = {
                                            velocityTracker.resetTracking()
                                        },
                                        onDragEnd = {
                                            val velocityY = velocityTracker.calculateVelocity().y
                                            // 计算每秒滚过的卡片插槽速度，并限制极值防止极端甩动预测过大
                                            val flingIndexVelocity = (-velocityY / itemSlotPx).coerceIn(-14f, 14f)
                                            val currentPos = scrollPos.value
                                            val speed = abs(flingIndexVelocity)

                                            val settleIndex = if (speed < 0.6f) {
                                                // 手指近乎静止松开：直接就近吸附，避免衰减模型预测引入噪声
                                                currentPos.roundToInt()
                                            } else {
                                                val decayTarget = decaySpec.calculateTargetValue(
                                                    initialValue = currentPos,
                                                    initialVelocity = flingIndexVelocity
                                                )
                                                val predicted = decayTarget.roundToInt()
                                                // 方向保险：确保结算落点不会发生反向回退
                                                if (flingIndexVelocity > 0) {
                                                    maxOf(predicted, currentPos.roundToInt())
                                                } else {
                                                    minOf(predicted, currentPos.roundToInt())
                                                }
                                            }

                                            // 根据滑动速度自适应弹簧刚度与阻尼比
                                            val springStiffness = when {
                                                speed > 6f -> Spring.StiffnessLow
                                                speed > 2f -> Spring.StiffnessMediumLow
                                                else -> Spring.StiffnessMedium
                                            }
                                            val dampingRatio = if (speed > 6f) 0.78f else 0.9f

                                            launch {
                                                scrollPos.animateTo(
                                                    targetValue = settleIndex.toFloat(),
                                                    initialVelocity = flingIndexVelocity,
                                                    animationSpec = spring(
                                                        dampingRatio = dampingRatio,
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
                                                        dampingRatio = 0.86f,
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
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // 按距中心的距离升序排列候选槽位，用 Set 去重（保留离中心最近的那个），再按距离降序排回用于绘制层级
                        val candidateSlots = listOf(0, -1, 1, -2, 2).sortedBy { abs(it) }
                        val seenAccounts = mutableSetOf<Int>()
                        val slotsToRender = candidateSlots
                            .filter { offset ->
                                val accIndex = ((baseIndex + offset) % safeCount + safeCount) % safeCount
                                seenAccounts.add(accIndex)
                            }
                            .sortedByDescending { abs(it) }

                        for (slotOffset in slotsToRender) {
                            val accIndex = ((baseIndex + slotOffset) % safeCount + safeCount) % safeCount
                            val acc = accounts[accIndex]

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(70.dp)
                                    .zIndex(10f - abs(slotOffset))
                                    .graphicsLayer {
                                        val currentPos = scrollPos.value
                                        val subOffset = currentPos - baseIndex
                                        val itemFraction = slotOffset - subOffset
                                        val distance = abs(itemFraction)

                                        if (distance > 2.1f) {
                                            alpha = 0f
                                        } else {
                                            val translateY = itemFraction * 44f * density
                                            val rotX = (-itemFraction * 20f).coerceIn(-45f, 45f)
                                            val scale = (1f - distance * 0.06f).coerceIn(0.80f, 1f)
                                            val itemAlpha = (1f - (distance * 0.50f)).coerceIn(0f, 1f)

                                            this.translationY = translateY
                                            this.rotationX = rotX
                                            this.scaleX = scale
                                            this.scaleY = scale
                                            this.alpha = itemAlpha
                                            this.cameraDistance = 16f * density
                                        }
                                    }
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(cardBg)
                                    .border(
                                        width = 0.7.dp,
                                        color = dividerColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val currentPos = scrollPos.value
                                        val targetPos = (baseIndex + slotOffset).toFloat()
                                        if (abs(currentPos - targetPos) < 0.35f) {
                                            onEditAccount(acc)
                                        } else {
                                            coroutineScope.launch {
                                                scrollPos.animateTo(
                                                    targetValue = targetPos,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.86f,
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
                                                fontWeight = FontWeight.Bold,
                                                color = inkPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (acc.cardSuffix.isNotBlank()) "CARD •••• ${acc.cardSuffix}" else acc.type,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = inkMuted
                                        )
                                    }

                                    val cents = AmountFormatter.yuanToCents(acc.balance)
                                    Text(
                                        text = "${if (acc.balance < 0) "-" else ""}¥${AmountFormatter.formatCentsAsYuan(cents)}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 17.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (acc.balance < 0) clayAccent else inkPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 瑞士极简新建 / 编辑账户弹窗
 *
 * 支持卡包分类：
 * 1. 现金：现金
 * 2. 信用卡：各个银行（系统预设，不让用户手动敲银行）
 * 3. 储蓄卡/借记卡：各个银行（系统预设）
 * 4. 网络账户：支付宝，微信钱包，QQ钱包，其他
 * 5. 投资账户：用户自定义名称直接添加
 * 6. 储值卡：用户自定义名称直接添加
 */
/**
 * 方案二：瑞士网格自适应开户/调账面板 (Swiss Editorial Sheet)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SwissAddEditAccountDialog(
    accountToEdit: AccountEntity?,
    isLight: Boolean,
    cardBg: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    dividerColor: Color,
    clayAccent: Color,
    onDismiss: () -> Unit,
    onDeleteClick: (AccountEntity) -> Unit,
    onConfirm: (name: String, type: String, balance: Double, suffix: String, color: String, saveAsMissedRecord: Boolean, oldBalance: Double, note: String) -> Unit
) {
    val isEditing = accountToEdit != null

    var categoryType by remember {
        mutableStateOf(
            when (accountToEdit?.type) {
                "CASH" -> AccountCategoryType.CASH
                "CREDIT_CARD" -> AccountCategoryType.CREDIT_CARD
                "BANK_CARD", "DEBIT_CARD" -> AccountCategoryType.DEBIT_CARD
                "WECHAT", "ALIPAY", "QQ", "DIGITAL_WALLET", "ONLINE" -> AccountCategoryType.DIGITAL_WALLET
                "INVESTMENT" -> AccountCategoryType.INVESTMENT
                "STORED_VALUE", "PREPAID", "OTHER" -> AccountCategoryType.STORED_VALUE
                else -> AccountCategoryType.DEBIT_CARD
            }
        )
    }

    var selectedBank by remember {
        mutableStateOf(
            if (accountToEdit != null && (categoryType == AccountCategoryType.CREDIT_CARD || categoryType == AccountCategoryType.DEBIT_CARD)) {
                PRESET_BANKS.firstOrNull { accountToEdit.name.contains(it) } ?: "招商银行"
            } else "招商银行"
        )
    }

    var selectedDigitalWallet by remember {
        mutableStateOf(
            if (accountToEdit != null && categoryType == AccountCategoryType.DIGITAL_WALLET) {
                when {
                    accountToEdit.type == "ALIPAY" || accountToEdit.name.contains("支付宝") -> "支付宝"
                    accountToEdit.type == "WECHAT" || accountToEdit.name.contains("微信") -> "微信钱包"
                    accountToEdit.type == "QQ" || accountToEdit.name.contains("QQ") -> "QQ钱包"
                    else -> "其他网络账户"
                }
            } else "支付宝"
        )
    }

    var customName by remember {
        mutableStateOf(
            if (accountToEdit != null) {
                accountToEdit.name
            } else {
                when (categoryType) {
                    AccountCategoryType.CASH -> "现金"
                    AccountCategoryType.CREDIT_CARD -> "$selectedBank(信用卡)"
                    AccountCategoryType.DEBIT_CARD -> "$selectedBank(储蓄卡)"
                    AccountCategoryType.DIGITAL_WALLET -> selectedDigitalWallet
                    AccountCategoryType.INVESTMENT, AccountCategoryType.STORED_VALUE -> ""
                }
            }
        )
    }

    var balanceText by remember {
        mutableStateOf(accountToEdit?.let { String.format(Locale.US, "%.2f", it.balance) } ?: "")
    }
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
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. 顶部 Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing) {
                        Text(
                            text = "编辑账户",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = inkPrimary,
                            letterSpacing = 0.3.sp
                        )
                    } else {
                        Column {
                            Text(
                                text = "NEW ACCOUNT / 开立账户",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = inkPrimary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "SWISS GRID · 极速网格录入",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = inkMuted
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = inkMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                HorizontalDivider(color = dividerColor.copy(alpha = 0.6f), thickness = 0.5.dp)

                if (!isEditing) {
                    // 新建模式下保留类别选择
                    // 2. 六大类别横向单排网格
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLight) Color(0xFFEFECE3) else Color(0xFF1B231B))
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        AccountCategoryType.values().forEach { cat ->
                            val isSelected = categoryType == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) inkPrimary else Color.Transparent)
                                    .clickable {
                                        categoryType = cat
                                        when (cat) {
                                            AccountCategoryType.CASH -> {
                                                customName = "现金"
                                                colorHex = "#2D6A4F"
                                            }
                                            AccountCategoryType.CREDIT_CARD -> {
                                                customName = "$selectedBank(信用卡)"
                                                colorHex = "#C4623D"
                                            }
                                            AccountCategoryType.DEBIT_CARD -> {
                                                customName = "$selectedBank(储蓄卡)"
                                                colorHex = "#2563EB"
                                            }
                                            AccountCategoryType.DIGITAL_WALLET -> {
                                                customName = selectedDigitalWallet
                                                colorHex = "#0D9488"
                                            }
                                            AccountCategoryType.INVESTMENT -> {
                                                if (customName == "现金" || customName.contains("银行") || customName.contains("钱包")) customName = ""
                                                colorHex = "#7C3AED"
                                            }
                                            AccountCategoryType.STORED_VALUE -> {
                                                if (customName == "现金" || customName.contains("银行") || customName.contains("钱包")) customName = ""
                                                colorHex = "#D97706"
                                            }
                                        }
                                    }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cat.title,
                                    fontSize = 10.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) cardBg else inkSecondary
                                )
                            }
                        }
                    }

                    // 3. 场景化瓦片选择区
                    when (categoryType) {
                        AccountCategoryType.CREDIT_CARD, AccountCategoryType.DEBIT_CARD -> {
                            val cardTypeLabel = if (categoryType == AccountCategoryType.CREDIT_CARD) "信用卡" else "储蓄卡"
                            Column {
                                Text(
                                    text = "SELECT BANK / 选择发卡银行 (3列网格)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = inkMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val banks = PRESET_BANKS.take(15)
                                val rows = banks.chunked(3)
                                rows.forEach { rowBanks ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowBanks.forEach { bank ->
                                            val isSelected = selectedBank == bank
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(34.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) inkPrimary else if (isLight) Color(0xFFF7F5F0) else Color(0xFF1B231B))
                                                    .border(
                                                        width = if (isSelected) 1.dp else 0.5.dp,
                                                        color = if (isSelected) inkPrimary else dividerColor,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedBank = bank
                                                        customName = "$bank($cardTypeLabel)"
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = bank,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) cardBg else inkPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }

                        AccountCategoryType.DIGITAL_WALLET -> {
                            Column {
                                Text(
                                    text = "SELECT WALLET / 选择网络账户 (2x2网格)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = inkMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                val walletRows = PRESET_DIGITAL_WALLETS.chunked(2)
                                walletRows.forEach { rowWallets ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        rowWallets.forEach { wallet ->
                                            val isSelected = selectedDigitalWallet == wallet
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isSelected) inkPrimary else if (isLight) Color(0xFFF7F5F0) else Color(0xFF1B231B))
                                                    .border(
                                                        width = if (isSelected) 1.dp else 0.5.dp,
                                                        color = if (isSelected) inkPrimary else dividerColor,
                                                        shape = RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        selectedDigitalWallet = wallet
                                                        customName = wallet
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = wallet,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) cardBg else inkPrimary
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }

                        AccountCategoryType.INVESTMENT -> {
                            Column {
                                Text(
                                    text = "RECOMMENDED / 投资机构快捷点选",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = inkMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PRESET_INVESTMENTS.forEach { tag ->
                                        val isSelected = customName == tag
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) inkPrimary else if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                                .clickable { customName = tag }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "+ $tag",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) cardBg else inkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        AccountCategoryType.STORED_VALUE -> {
                            Column {
                                Text(
                                    text = "RECOMMENDED / 储值卡快捷点选",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = inkMuted
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PRESET_STORED_VALUES.forEach { tag ->
                                        val isSelected = customName == tag
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSelected) inkPrimary else if (isLight) Color(0xFFF0ECE1) else Color(0xFF1B231B))
                                                .clickable { customName = tag }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "+ $tag",
                                                fontSize = 10.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSelected) cardBg else inkSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {}
                    }
                }

                // 4. 账目属性编辑功能 (纯净无背景色块无外边框，通透呼吸感)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("账户名称", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace) },
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = balanceText,
                            onValueChange = { balanceText = it },
                            label = {
                                Text(
                                    text = if (categoryType == AccountCategoryType.CREDIT_CARD) "当前欠款(负数)/余额" else "当前余额 (¥)",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = inkPrimary,
                                unfocusedBorderColor = dividerColor,
                                focusedTextColor = inkPrimary,
                                unfocusedTextColor = inkPrimary
                            )
                        )

                        OutlinedTextField(
                            value = suffix,
                            onValueChange = { if (it.length <= 4) suffix = it },
                            label = { Text("卡号后4位", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = inkPrimary,
                                unfocusedBorderColor = dividerColor,
                                focusedTextColor = inkPrimary,
                                unfocusedTextColor = inkPrimary
                            )
                        )
                    }

                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("备注说明 (选填)", fontSize = 11.5.sp, fontFamily = FontFamily.Monospace) },
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
                }

                // Balance Diff Record Toggle (if editing)
                if (isEditing && accountToEdit != null) {
                    val newBal = balanceText.toDoubleOrNull() ?: accountToEdit.balance
                    val diff = newBal - accountToEdit.balance
                    if (abs(diff) > 0.009) {
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
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = inkPrimary,
                                    checkedTrackColor = inkPrimary.copy(alpha = 0.3f)
                                )
                            )
                        }
                    }
                }

                // 6. 确认提交 / 删除
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && accountToEdit != null) {
                        IconButton(
                            onClick = { onDeleteClick(accountToEdit) },
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(clayAccent.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除此账户",
                                tint = clayAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    val finalTypeString = when (categoryType) {
                        AccountCategoryType.CASH -> "CASH"
                        AccountCategoryType.CREDIT_CARD -> "CREDIT_CARD"
                        AccountCategoryType.DEBIT_CARD -> "BANK_CARD"
                        AccountCategoryType.DIGITAL_WALLET -> when (selectedDigitalWallet) {
                            "支付宝" -> "ALIPAY"
                            "微信钱包" -> "WECHAT"
                            "QQ钱包" -> "QQ"
                            else -> "DIGITAL_WALLET"
                        }
                        AccountCategoryType.INVESTMENT -> "INVESTMENT"
                        AccountCategoryType.STORED_VALUE -> "STORED_VALUE"
                    }

                    // 优化后的保存按钮配色与微质感：优雅纸墨深翠绿色/高对比度雅致主题色
                    val saveButtonColor = if (isEditing) {
                        if (isLight) Color(0xFF234E3E) else Color(0xFF386641)
                    } else {
                        inkPrimary
                    }

                    Button(
                        onClick = {
                            val parsedBalance = balanceText.toDoubleOrNull() ?: 0.0
                            val finalName = customName.ifBlank {
                                when (categoryType) {
                                    AccountCategoryType.CASH -> "现金"
                                    AccountCategoryType.CREDIT_CARD -> "$selectedBank(信用卡)"
                                    AccountCategoryType.DEBIT_CARD -> "$selectedBank(储蓄卡)"
                                    AccountCategoryType.DIGITAL_WALLET -> selectedDigitalWallet
                                    AccountCategoryType.INVESTMENT -> "投资账户"
                                    AccountCategoryType.STORED_VALUE -> "储值卡"
                                }
                            }

                            onConfirm(
                                finalName,
                                finalTypeString,
                                parsedBalance,
                                suffix,
                                colorHex,
                                saveAsMissedRecord,
                                accountToEdit?.balance ?: 0.0,
                                note
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = saveButtonColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.5.dp,
                            pressedElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = if (isEditing) "保存账户变更" else "CONFIRM & CREATE / 确认开立",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
