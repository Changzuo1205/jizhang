package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryItem
import com.example.data.local.CategoryManager
import com.example.data.local.ExpenseEntity
import com.example.ui.components.*
import com.example.ui.theme.LocalAppBackgroundConfig
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 质感手帐美学 · 明细记账与编辑页面 (Tactile Editorial Add / Edit Screen)
 *
 * 核心设计亮点：
 * 1. 墨水与光晕弥散 (Ambient Ink Glow) - 巨幅 Serif 算式面板 + 呼吸感微光晕
 * 2. 杂志风一级大类与微浮雕实体印章二级细分 (Tactile Paper Chips & Magazine Labeling)
 * 3. 手帐点阵便签纸条元数据行 (Dot-Grid Metadata Slip)
 * 4. 转账能量流向卡片 (Magnetic Transfer Stream)
 * 5. 专业 5 列陶瓷触感紧凑计算键盘：
 *    - Col 1-3: 789 / 456 / 123 / C 0 .
 *    - Col 4: ÷ × - +
 *    - Col 5: ⌫ 退格 / 再记 (连记) / 保存 (占下两行的高级渐变微光印章大键)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorialExpenseAddEditScreen(
    expenseToEdit: ExpenseEntity? = null,
    allExpenses: List<ExpenseEntity> = emptyList(),
    accounts: List<AccountEntity> = emptyList(),
    initialTimestamp: Long = System.currentTimeMillis(),
    isPreviewMode: Boolean = false,
    onDismiss: () -> Unit = {},
    onConfirm: (
        type: String,
        category: String,
        subCategory: String,
        amount: Double,
        note: String,
        accountId: Long,
        accountName: String,
        timestamp: Long,
        transferToAccountId: Long?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val globalBgConfig = LocalAppBackgroundConfig.current
    val view = LocalView.current

    // 深浅色独立预览开关
    var forceDarkPreview by remember { mutableStateOf<Boolean?>(null) }
    val isLight = forceDarkPreview?.let { !it } ?: globalBgConfig.isLight

    // 配色令牌
    val paperBg = if (isLight) Color(0xFFFBF9F4) else Color(0xFF111316)
    val inkPrimary = if (isLight) Color(0xFF1A1A18) else Color(0xFFF4F3EE)
    val inkSecondary = if (isLight) Color(0xFF6B6962) else Color(0xFFA6A49C)
    val inkMuted = if (isLight) Color(0xFFA4A299) else Color(0xFF5D5F66)
    val borderSubtle = if (isLight) Color(0xFFEBE7DD) else Color(0xFF22262D)
    val paperSlipBg = if (isLight) Color(0xFFF2EFE8) else Color(0xFF1A1D23)
    val chipSurface = if (isLight) Color(0xFFFFFFFF) else Color(0xFF1C2027)

    // 点缀强调色
    val clayAccent = Color(0xFFC05834) // 支出暖陶红
    val forestSage = Color(0xFF2D7B43) // 收入森绿
    val royalIndigo = Color(0xFF4361EE) // 转账靛蓝

    // 默认账户回退数据
    val safeAccounts = remember(accounts) {
        if (accounts.isNotEmpty()) accounts
        else listOf(
            AccountEntity(id = 1L, name = "微信钱包", type = "WECHAT", balance = 3280.50),
            AccountEntity(id = 2L, name = "支付宝", type = "ALIPAY", balance = 8450.00),
            AccountEntity(id = 3L, name = "招商银行卡", type = "BANK", balance = 24600.00),
            AccountEntity(id = 4L, name = "日常现金", type = "CASH", balance = 500.00)
        )
    }

    // 1. 类型状态 (0=支出, 1=收入, 2=转账)
    var selectedTypeIndex by remember {
        mutableIntStateOf(
            when (expenseToEdit?.type) {
                "INCOME" -> 1
                "TRANSFER" -> 2
                else -> 0
            }
        )
    }
    val isExpense = selectedTypeIndex == 0
    val isIncome = selectedTypeIndex == 1
    val isTransfer = selectedTypeIndex == 2
    val currentType = if (isIncome) "INCOME" else "EXPENSE"

    val activeAccentColor = when (selectedTypeIndex) {
        1 -> forestSage
        2 -> royalIndigo
        else -> clayAccent
    }

    // 2. 资金账户
    var selectedAccountId by remember {
        mutableLongStateOf(
            expenseToEdit?.accountId ?: safeAccounts.firstOrNull()?.id ?: 1L
        )
    }
    var transferToAccountId by remember {
        mutableLongStateOf(
            expenseToEdit?.transferToAccountId?.takeIf { it != 0L }
                ?: safeAccounts.firstOrNull { it.id != selectedAccountId }?.id
                ?: safeAccounts.getOrNull(1)?.id
                ?: 2L
        )
    }
    val selectedAccount = safeAccounts.find { it.id == selectedAccountId } ?: safeAccounts.firstOrNull()
    val targetTransferAccount = safeAccounts.find { it.id == transferToAccountId } ?: safeAccounts.getOrNull(1)

    // 3. 分类与二级细分
    var categoriesRefreshKey by remember { mutableIntStateOf(0) }
    val allCategories = remember(currentType, categoriesRefreshKey, allExpenses) {
        val baseCategories = CategoryManager.getCategories(context, currentType)
        val frequencyMap = allExpenses
            .filter { it.type == currentType }
            .groupingBy { it.category }
            .eachCount()
        baseCategories.sortedByDescending { frequencyMap[it.name] ?: 0 }
    }

    var selectedCategory by remember(currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (expenseToEdit != null && allCategories.any { it.name == expenseToEdit.category }) {
                expenseToEdit.category
            } else if (isExpense) {
                "餐饮"
            } else {
                val lastInc = CategoryManager.getLastIncomeCategory(context)
                if (lastInc.isNotBlank() && allCategories.any { it.name == lastInc }) lastInc
                else allCategories.firstOrNull()?.name ?: "工资薪水"
            }
        )
    }

    var selectedSubCategory by remember(selectedCategory, currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (isIncome) {
                if (expenseToEdit != null && expenseToEdit.category.isNotBlank()) expenseToEdit.category
                else selectedCategory
            } else if (expenseToEdit != null && expenseToEdit.category == selectedCategory && expenseToEdit.subCategory.isNotBlank()) {
                expenseToEdit.subCategory
            } else {
                CategoryManager.getDefaultSubcategory(
                    context = context,
                    categoryName = selectedCategory,
                    type = currentType,
                    isFreshCreation = (expenseToEdit == null && selectedCategory == "餐饮")
                )
            }
        )
    }

    val currentSubcategories = remember(selectedCategory, currentType, categoriesRefreshKey) {
        if (isTransfer) emptyList()
        else CategoryManager.getSubcategories(context, selectedCategory, currentType)
    }

    // 4. 输入与状态
    var amountInput by remember {
        mutableStateOf(if (expenseToEdit != null) expenseToEdit.amount.toString() else "")
    }
    var noteInput by remember {
        mutableStateOf(expenseToEdit?.note ?: "")
    }
    var selectedTimestamp by remember {
        mutableLongStateOf(expenseToEdit?.dateTimestamp ?: initialTimestamp)
    }

    // 5. 弹窗与抽屉
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var showTransferTargetPickerSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubCategoryDialog by remember { mutableStateOf(false) }
    var showSavedToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    // 灵感便签标签池
    val quickNotes = remember(selectedCategory, selectedSubCategory, isExpense, isIncome, isTransfer) {
        when {
            isTransfer -> listOf("生活费转账", "信用卡还款", "零钱充值", "亲友借还", "备用金")
            isIncome -> listOf("月度工资", "季度奖金", "理财收益", "副业兼职", "公积金提现", "红包礼金")
            selectedCategory == "餐饮" -> listOf("日常三餐", "外卖便当", "手冲咖啡", "朋友小聚", "夜宵烧烤", "生鲜买菜")
            selectedCategory == "交通" -> listOf("早晚高峰打车", "地铁通勤", "加油充值", "路桥停车", "高铁出行")
            selectedCategory == "购物" -> listOf("日用杂货", "当季服饰", "数码配件", "美妆护肤", "图书杂志")
            selectedCategory == "居家" -> listOf("月度房租", "水电燃气", "物业杂费", "宽带充值", "家居清洁")
            selectedCategory == "娱乐" -> listOf("院线电影", "周末桌游", "运动健身", "游戏订阅", "展览演出")
            selectedCategory == "医教" -> listOf("日常配药", "体检套餐", "课程培训", "书籍学习")
            else -> listOf("日常消费", "临时开销", "生活补贴", "周末聚餐")
        }
    }

    // 保存逻辑
    fun doSave(closeOnFinish: Boolean) {
        val calculatedAmount = if (amountInput.isNotEmpty()) {
            evaluateExpression(amountInput).toDoubleOrNull() ?: 0.0
        } else 0.0

        val isAccountSelected = selectedAccount != null
        val transferTargetValid = !isTransfer ||
            (transferToAccountId != selectedAccountId && safeAccounts.any { it.id == transferToAccountId })

        if (calculatedAmount > 0 && isAccountSelected && transferTargetValid) {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

            if (!isTransfer && selectedSubCategory.isNotBlank()) {
                CategoryManager.saveLastSelectedSubcategory(context, selectedCategory, selectedSubCategory)
            }
            if (isIncome) {
                CategoryManager.saveLastIncomeCategory(context, selectedCategory)
            }
            onConfirm(
                when (selectedTypeIndex) {
                    1 -> "INCOME"
                    2 -> "TRANSFER"
                    else -> "EXPENSE"
                },
                if (isTransfer) "" else selectedCategory,
                if (isTransfer) "" else selectedSubCategory,
                calculatedAmount,
                noteInput.trim(),
                selectedAccount!!.id,
                selectedAccount.name,
                selectedTimestamp,
                if (isTransfer) transferToAccountId else null
            )

            toastMessage = if (isTransfer) "✦ 已记转账 ¥${String.format(Locale.US, "%.2f", calculatedAmount)}"
            else "✦ 已记录 ¥${String.format(Locale.US, "%.2f", calculatedAmount)}"
            showSavedToast = true

            if (closeOnFinish) {
                if (!isPreviewMode) {
                    onDismiss()
                }
            } else {
                amountInput = ""
                noteInput = ""
            }
        }
    }

    LaunchedEffect(showSavedToast) {
        if (showSavedToast) {
            delay(1800)
            showSavedToast = false
        }
    }

    if (!isPreviewMode) {
        BackHandler(onBack = onDismiss)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(paperBg)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // ── 1. 顶部手帐质感导航栏 (Magazine Style Top Bar) ─────────────
            TactileTopNavigationBar(
                selectedTypeIndex = selectedTypeIndex,
                onSelectTypeIndex = {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    selectedTypeIndex = it
                },
                activeColor = activeAccentColor,
                inkPrimary = inkPrimary,
                inkSecondary = inkSecondary,
                inkMuted = inkMuted,
                borderSubtle = borderSubtle,
                isLight = isLight,
                isPreviewMode = isPreviewMode,
                onBack = onDismiss,
                onToggleLightDark = { forceDarkPreview = !(isLight) }
            )

            // ── 2. 手帐纸感核心画布区（金额光晕、二级分类、便签纸条） ──────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 上半区：巨幅 Serif 金额 + 呼吸感微光晕
                TactileAmbientAmountSection(
                    expression = amountInput,
                    activeColor = activeAccentColor,
                    inkPrimary = inkPrimary,
                    inkMuted = inkMuted,
                    isLight = isLight,
                    onClear = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        amountInput = ""
                    }
                )

                // 中间区：两级分类抽屉 / 转账能量流向卡
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (isTransfer) {
                        TactileMagneticTransferCard(
                            fromAccount = selectedAccount?.name ?: "转出账户",
                            toAccount = targetTransferAccount?.name ?: "转入账户",
                            inkPrimary = inkPrimary,
                            inkSecondary = inkSecondary,
                            paperSlipBg = paperSlipBg,
                            accentColor = royalIndigo,
                            borderSubtle = borderSubtle,
                            onSelectFrom = { showAccountPickerSheet = true },
                            onSelectTo = { showTransferTargetPickerSheet = true }
                        )
                    } else {
                        // 一级大类：杂志风排版单行 (Magazine Primary Stream)
                        TactileMagazineCategoryRow(
                            categories = allCategories,
                            selectedCategory = selectedCategory,
                            activeColor = activeAccentColor,
                            inkPrimary = inkPrimary,
                            inkSecondary = inkSecondary,
                            onSelectCategory = { catName ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                selectedCategory = catName
                                if (isIncome) {
                                    selectedSubCategory = catName
                                } else {
                                    selectedSubCategory = CategoryManager.getDefaultSubcategory(
                                        context = context,
                                        categoryName = catName,
                                        type = currentType,
                                        isFreshCreation = false
                                    )
                                }
                            },
                            onAddCategory = { showAddCategoryDialog = true }
                        )

                        // 二级细分：纸张微浮雕实体印章胶囊 (Tactile Paper Chips)
                        if (isExpense && currentSubcategories.isNotEmpty()) {
                            TactileSubcategoryPaperChips(
                                subcategories = currentSubcategories,
                                selectedSubCategory = selectedSubCategory,
                                activeColor = activeAccentColor,
                                inkPrimary = inkPrimary,
                                inkSecondary = inkSecondary,
                                chipSurface = chipSurface,
                                borderSubtle = borderSubtle,
                                isLight = isLight,
                                onSelectSubCategory = {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    selectedSubCategory = it
                                },
                                onAddSubCategory = { showAddSubCategoryDialog = true }
                            )
                        }
                    }

                    // ── 3. 手帐便签纸条元数据行 (Dot-Grid Metadata Slip) ──────
                    TactileDotGridMetaSlip(
                        selectedTimestamp = selectedTimestamp,
                        accountName = selectedAccount?.name ?: "选择账户",
                        isTransfer = isTransfer,
                        note = noteInput,
                        onNoteChange = { noteInput = it },
                        quickNotes = quickNotes,
                        inkPrimary = inkPrimary,
                        inkSecondary = inkSecondary,
                        inkMuted = inkMuted,
                        paperSlipBg = paperSlipBg,
                        borderSubtle = borderSubtle,
                        activeColor = activeAccentColor,
                        onOpenTimePicker = { showTimePickerSheet = true },
                        onOpenAccountPicker = { showAccountPickerSheet = true }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
            }

            // ── 4. 专业 5 列陶瓷触感紧凑计算键盘 ───────────────────────────
            TactileFiveColumnNumpad(
                expression = amountInput,
                onExpressionChange = { amountInput = it },
                onConfirm = { doSave(closeOnFinish = true) },
                onSaveAndNext = { doSave(closeOnFinish = false) },
                activeColor = activeAccentColor,
                paperBg = paperBg,
                chipSurface = chipSurface,
                inkPrimary = inkPrimary,
                inkSecondary = inkSecondary,
                borderSubtle = borderSubtle,
                isLight = isLight
            )
        }

        // 保存成功 Floating 质感印章提示
        AnimatedVisibility(
            visible = showSavedToast,
            enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 2 },
            exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isLight) Color(0xFF1E1E1C) else Color(0xFFF3F2EE))
                    .border(1.dp, activeAccentColor.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = toastMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isLight) Color(0xFFFAF9F4) else Color(0xFF111316)
                )
            }
        }
    }

    // ── 底部抽屉与自定义弹窗 ─────────────────────────────────────────
    if (showTimePickerSheet) {
        WheelTimePickerSheet(
            initialTimestamp = selectedTimestamp,
            onDismiss = { showTimePickerSheet = false },
            onConfirm = {
                selectedTimestamp = it
                showTimePickerSheet = false
            },
            accentColor = activeAccentColor
        )
    }

    if (showAccountPickerSheet) {
        AccountPickerSheet(
            accounts = safeAccounts,
            selectedAccountId = selectedAccountId,
            recentAccountIds = emptyList(),
            onDismiss = { showAccountPickerSheet = false },
            onSelectAccount = { acc ->
                selectedAccountId = acc.id
                if (isTransfer && transferToAccountId == acc.id) {
                    transferToAccountId = safeAccounts.firstOrNull { it.id != acc.id }?.id ?: acc.id
                }
                showAccountPickerSheet = false
            },
            accentColor = activeAccentColor
        )
    }

    if (showTransferTargetPickerSheet) {
        AccountPickerSheet(
            accounts = safeAccounts.filter { it.id != selectedAccountId },
            selectedAccountId = transferToAccountId,
            recentAccountIds = emptyList(),
            onDismiss = { showTransferTargetPickerSheet = false },
            onSelectAccount = { acc ->
                transferToAccountId = acc.id
                showTransferTargetPickerSheet = false
            },
            accentColor = activeAccentColor
        )
    }

    // 自定义大类弹窗
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newSubCatText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1C2027))
                    .border(1.dp, borderSubtle, RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column {
                    Text("新建一级分类", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = inkPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        placeholder = { Text("如：数码、宠物、私教", color = inkMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSubCatText,
                        onValueChange = { newSubCatText = it },
                        placeholder = { Text("初始细分（逗号隔开，如：猫粮, 玩具）", color = inkMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text("取消", color = inkSecondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val trimmed = newCatName.trim()
                                if (trimmed.isNotBlank()) {
                                    val subList = newSubCatText.split(",", "，", " ")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    CategoryManager.addCustomCategory(context, trimmed, currentType, subList)
                                    categoriesRefreshKey++
                                    selectedCategory = trimmed
                                    if (subList.isNotEmpty()) selectedSubCategory = subList.first()
                                    showAddCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("保存", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // 自定义细分弹窗
    if (showAddSubCategoryDialog) {
        var newSubName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddSubCategoryDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isLight) Color(0xFFFFFFFF) else Color(0xFF1C2027))
                    .border(1.dp, borderSubtle, RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column {
                    Text("为「$selectedCategory」添加细分", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = inkPrimary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        placeholder = { Text("如：手冲咖啡、特调", color = inkMuted) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddSubCategoryDialog = false }) {
                            Text("取消", color = inkSecondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = {
                                val trimmed = newSubName.trim()
                                if (trimmed.isNotBlank()) {
                                    CategoryManager.addCustomSubcategory(context, selectedCategory, trimmed)
                                    categoriesRefreshKey++
                                    selectedSubCategory = trimmed
                                    showAddSubCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeAccentColor),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("保存", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ── 组件 1: 顶部手帐质感导航栏 ───────────────────────────────────────
@Composable
private fun TactileTopNavigationBar(
    selectedTypeIndex: Int,
    onSelectTypeIndex: (Int) -> Unit,
    activeColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    borderSubtle: Color,
    isLight: Boolean,
    isPreviewMode: Boolean,
    onBack: () -> Unit,
    onToggleLightDark: () -> Unit
) {
    val types = listOf("支出", "收入", "转账")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 关闭/返回
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isPreviewMode) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = inkPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        // 杂志文字 Tab 切换 (· 支出 · 收入 · 转账 ·)
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            types.forEachIndexed { index, title ->
                val isSelected = selectedTypeIndex == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectTypeIndex(index) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = if (isSelected) 15.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) inkPrimary else inkMuted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .width(if (isSelected) 12.dp else 0.dp)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) activeColor else Color.Transparent)
                    )
                }
            }
        }

        // 昼夜预览微调
        IconButton(
            onClick = onToggleLightDark,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = if (isLight) Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "切换预览昼夜",
                tint = inkSecondary,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

// ── 组件 2: 巨幅 Serif 金额面板 + 呼吸感微光晕 ───────────────────────
@Composable
private fun TactileAmbientAmountSection(
    expression: String,
    activeColor: Color,
    inkPrimary: Color,
    inkMuted: Color,
    isLight: Boolean,
    onClear: () -> Unit
) {
    val evaluatedPreview = remember(expression) {
        if (hasOperator(expression) && canEvaluate(expression)) {
            val res = evaluateExpression(expression)
            if (res != expression) "≈ ¥$res" else ""
        } else ""
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // 背景温润墨水微光晕 (Ambient Ink Glow)
        Box(
            modifier = Modifier
                .size(160.dp, 60.dp)
                .blur(32.dp)
                .background(
                    activeColor.copy(alpha = if (isLight) 0.08f else 0.12f),
                    CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 算式即时求值微弱提示
            Box(
                modifier = Modifier.height(18.dp),
                contentAlignment = Alignment.Center
            ) {
                if (evaluatedPreview.isNotEmpty()) {
                    Text(
                        text = evaluatedPreview,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = activeColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 巨幅 Serif 金额与手写斜体 ¥
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¥",
                    fontSize = 28.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    color = activeColor,
                    modifier = Modifier.padding(end = 8.dp, bottom = 2.dp)
                )

                Text(
                    text = if (expression.isEmpty()) "0.00" else expression,
                    fontSize = if (expression.length > 9) 34.sp else 44.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = if (expression.isEmpty()) inkMuted.copy(alpha = 0.5f) else inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (expression.isNotEmpty()) {
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "清空",
                            tint = inkMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── 组件 3: 杂志风一级大类排版 ───────────────────────────────────────
@Composable
private fun TactileMagazineCategoryRow(
    categories: List<CategoryItem>,
    selectedCategory: String,
    activeColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    onSelectCategory: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(categories, key = { it.name }) { cat ->
            val isSelected = selectedCategory == cat.name
            val icon = CategoryManager.getCategoryIcon(cat.name)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectCategory(cat.name) }
                    .padding(vertical = 4.dp, horizontal = 2.dp)
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = activeColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = cat.name,
                    fontSize = if (isSelected) 15.sp else 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) inkPrimary else inkSecondary.copy(alpha = 0.65f)
                )
            }
        }

        item {
            Text(
                text = "+ 新增",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = activeColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onAddCategory() }
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }
    }
}

// ── 组件 4: 纸张微浮雕实体印章胶囊 (Tactile Paper Chips) ──────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TactileSubcategoryPaperChips(
    subcategories: List<String>,
    selectedSubCategory: String,
    activeColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    chipSurface: Color,
    borderSubtle: Color,
    isLight: Boolean,
    onSelectSubCategory: (String) -> Unit,
    onAddSubCategory: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        subcategories.forEach { subCat ->
            val isSelected = selectedSubCategory == subCat

            Box(
                modifier = Modifier
                    .shadow(
                        elevation = if (isSelected && isLight) 2.dp else 0.dp,
                        shape = RoundedCornerShape(12.dp),
                        clip = false
                    )
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) chipSurface else chipSurface.copy(alpha = 0.6f))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) activeColor.copy(alpha = 0.7f) else borderSubtle,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelectSubCategory(subCat) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subCat,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) activeColor else inkSecondary
                )
            }
        }

        // 自定义细分微胶囊
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(chipSurface.copy(alpha = 0.3f))
                .border(1.dp, borderSubtle, RoundedCornerShape(12.dp))
                .clickable { onAddSubCategory() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = "+ 细分",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = inkSecondary.copy(alpha = 0.7f)
            )
        }
    }
}

// ── 组件 5: 转账能量流向卡片 (Magnetic Transfer Stream) ─────────────
@Composable
private fun TactileMagneticTransferCard(
    fromAccount: String,
    toAccount: String,
    inkPrimary: Color,
    inkSecondary: Color,
    paperSlipBg: Color,
    accentColor: Color,
    borderSubtle: Color,
    onSelectFrom: () -> Unit,
    onSelectTo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(paperSlipBg)
            .border(1.dp, borderSubtle, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelectFrom)
        ) {
            Text("转出账户", fontSize = 11.sp, color = inkSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(fromAccount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = inkPrimary)
        }

        Icon(
            imageVector = Icons.Default.SwapHoriz,
            contentDescription = "流向",
            tint = accentColor,
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(20.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onSelectTo),
            horizontalAlignment = Alignment.End
        ) {
            Text("转入账户", fontSize = 11.sp, color = inkSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(toAccount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = accentColor)
        }
    }
}

// ── 组件 6: 手帐便签纸条元数据行 (Dot-Grid Metadata Slip) ────────────
@Composable
private fun TactileDotGridMetaSlip(
    selectedTimestamp: Long,
    accountName: String,
    isTransfer: Boolean,
    note: String,
    onNoteChange: (String) -> Unit,
    quickNotes: List<String>,
    inkPrimary: Color,
    inkSecondary: Color,
    inkMuted: Color,
    paperSlipBg: Color,
    borderSubtle: Color,
    activeColor: Color,
    onOpenTimePicker: () -> Unit,
    onOpenAccountPicker: () -> Unit
) {
    val dateText = remember(selectedTimestamp) {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
        val now = Calendar.getInstance()
        val isToday = cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        val timeStr = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(selectedTimestamp))
        if (isToday) "今天 $timeStr" else SimpleDateFormat("MM.dd $timeStr", Locale.CHINA).format(Date(selectedTimestamp))
    }

    var showQuickTagPool by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 主便签条 (Dot-grid style slip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(paperSlipBg)
                .border(1.dp, borderSubtle, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间印章
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clickable(onClick = onOpenTimePicker)
                    .padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = dateText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = inkSecondary
                )
            }

            Text("┆", fontSize = 12.sp, color = inkMuted.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 4.dp))

            // 账户选择 (非转账模式)
            if (!isTransfer) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clickable(onClick = onOpenAccountPicker)
                        .padding(end = 4.dp)
                ) {
                    Text(
                        text = accountName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = inkSecondary
                    )
                }

                Text("┆", fontSize = 12.sp, color = inkMuted.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 4.dp))
            }

            // 备注输入框
            BasicTextField(
                value = note,
                onValueChange = onNoteChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal,
                    color = inkPrimary
                ),
                cursorBrush = SolidColor(inkPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (note.isEmpty()) {
                        Text("添写备注...", fontSize = 12.sp, color = inkMuted)
                    }
                    innerTextField()
                }
            )

            // 便签快捷标签展开按钮
            IconButton(
                onClick = { showQuickTagPool = !showQuickTagPool },
                modifier = Modifier.size(22.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tag,
                    contentDescription = "标签池",
                    tint = if (showQuickTagPool) activeColor else inkMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // 展开的便利贴灵感标签池 (Quick Tag Pool)
        AnimatedVisibility(
            visible = showQuickTagPool,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickNotes) { qNote ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(paperSlipBg)
                            .border(1.dp, borderSubtle, RoundedCornerShape(8.dp))
                            .clickable {
                                onNoteChange(if (note.isBlank()) qNote else "$note $qNote")
                            }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(qNote, fontSize = 11.sp, color = inkSecondary)
                    }
                }
            }
        }
    }
}

// ── 组件 7: 专业 5 列陶瓷触感紧凑计算键盘 ───────────────────────────
@Composable
private fun TactileFiveColumnNumpad(
    expression: String,
    onExpressionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onSaveAndNext: () -> Unit,
    activeColor: Color,
    paperBg: Color,
    chipSurface: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    borderSubtle: Color,
    isLight: Boolean
) {
    val view = LocalView.current
    val hasOp = hasOperator(expression)
    val canEval = canEvaluate(expression)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(paperBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 第一行：7, 8, 9, ÷, ⌫ (退格)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TactileNumpadBtn("7", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("7", expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn("8", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("8", expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn("9", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("9", expression, onExpressionChange, onConfirm)
            }
            TactileOperatorBtn("÷", isLight, inkSecondary, borderSubtle, Modifier.weight(1f)) {
                handleInput("÷", expression, onExpressionChange, onConfirm)
            }
            // Col 5 Row 1: 退格
            TactileFuncBtn(
                isLight = isLight,
                borderSubtle = borderSubtle,
                modifier = Modifier.weight(1f),
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    if (expression.isNotEmpty()) {
                        onExpressionChange(expression.dropLast(1))
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "退格",
                    tint = inkPrimary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // 第二行：4, 5, 6, ×, 再记
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TactileNumpadBtn("4", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("4", expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn("5", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("5", expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn("6", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput("6", expression, onExpressionChange, onConfirm)
            }
            TactileOperatorBtn("×", isLight, inkSecondary, borderSubtle, Modifier.weight(1f)) {
                handleInput("×", expression, onExpressionChange, onConfirm)
            }
            // Col 5 Row 2: 再记 (连记)
            TactileTextFuncBtn(
                text = "再记",
                isLight = isLight,
                textColor = activeColor,
                borderSubtle = borderSubtle,
                modifier = Modifier.weight(1f),
                onClick = onSaveAndNext
            )
        }

        // 第三与第四行联合布局：左边 4 列（123- / C0.+），右边第 5 列占用双倍高度作为「保存」微光印章大键
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 左侧 4 列（2行）
            Column(
                modifier = Modifier.weight(4f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 3 (1, 2, 3, -)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TactileNumpadBtn("1", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("1", expression, onExpressionChange, onConfirm)
                    }
                    TactileNumpadBtn("2", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("2", expression, onExpressionChange, onConfirm)
                    }
                    TactileNumpadBtn("3", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("3", expression, onExpressionChange, onConfirm)
                    }
                    TactileOperatorBtn("-", isLight, inkSecondary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("-", expression, onExpressionChange, onConfirm)
                    }
                }

                // Row 4 (C, 0, ., +)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // 清除键 C
                    TactileTextFuncBtn(
                        text = "C",
                        isLight = isLight,
                        textColor = inkSecondary,
                        borderSubtle = borderSubtle,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                            onExpressionChange("")
                        }
                    )
                    TactileNumpadBtn("0", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("0", expression, onExpressionChange, onConfirm)
                    }
                    TactileNumpadBtn(".", isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput(".", expression, onExpressionChange, onConfirm)
                    }
                    TactileOperatorBtn("+", isLight, inkSecondary, borderSubtle, Modifier.weight(1f)) {
                        handleInput("+", expression, onExpressionChange, onConfirm)
                    }
                }
            }

            // 右侧第 5 列：双倍高度的高级手帐印章「保存 / =」按钮
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(102.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                activeColor,
                                activeColor.copy(alpha = 0.88f)
                            )
                        )
                    )
                    .clickable {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        if (hasOp && canEval) {
                            val evaluated = evaluateExpression(expression)
                            onExpressionChange(evaluated)
                            onConfirm()
                        } else {
                            onConfirm()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (hasOp && canEval) {
                        Text(
                            text = "=",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "保存",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "保存",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ── 键盘微组件封装 ───────────────────────────────────────────────────
@Composable
private fun TactileNumpadBtn(
    symbol: String,
    isLight: Boolean,
    chipSurface: Color,
    inkPrimary: Color,
    borderSubtle: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(chipSurface)
            .border(1.dp, borderSubtle, RoundedCornerShape(10.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Serif,
            color = inkPrimary
        )
    }
}

@Composable
private fun TactileOperatorBtn(
    symbol: String,
    isLight: Boolean,
    inkSecondary: Color,
    borderSubtle: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val opBg = if (isLight) Color(0xFFF1EFE8) else Color(0xFF1B1E24)

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(opBg)
            .border(1.dp, borderSubtle, RoundedCornerShape(10.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 19.sp,
            fontWeight = FontWeight.SemiBold,
            color = inkSecondary
        )
    }
}

@Composable
private fun TactileFuncBtn(
    isLight: Boolean,
    borderSubtle: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val funcBg = if (isLight) Color(0xFFF1EFE8) else Color(0xFF1B1E24)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(funcBg)
            .border(1.dp, borderSubtle, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun TactileTextFuncBtn(
    text: String,
    isLight: Boolean,
    textColor: Color,
    borderSubtle: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val view = LocalView.current
    val funcBg = if (isLight) Color(0xFFF1EFE8) else Color(0xFF1B1E24)
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(funcBg)
            .border(1.dp, borderSubtle, RoundedCornerShape(10.dp))
            .clickable {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}
