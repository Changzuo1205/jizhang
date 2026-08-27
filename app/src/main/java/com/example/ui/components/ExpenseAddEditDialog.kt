package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryItem
import com.example.data.local.CategoryManager
import com.example.data.local.ExpenseEntity
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseAddEditDialog(
    expenseToEdit: ExpenseEntity? = null,
    allExpenses: List<ExpenseEntity> = emptyList(),
    accounts: List<AccountEntity>,
    initialTimestamp: Long = System.currentTimeMillis(),
    onDismiss: () -> Unit,
    onConfirm: (
        type: String,
        category: String,
        subCategory: String,
        amount: Double,
        note: String,
        accountId: Long,
        accountName: String,
        timestamp: Long,
        transferToAccountId: Long? // 转账目标账户；仅 type=TRANSFER 时非空
    ) -> Unit
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    // Focus Requester & Keyboard Controller for Auto Focus & Keyboard popup
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Segmented Type (0 = EXPENSE, 1 = INCOME, 2 = TRANSFER)
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
    val isTransfer = selectedTypeIndex == 2
    val currentType = if (selectedTypeIndex == 1) "INCOME" else "EXPENSE"

    // 转账 Tab 专属的紫罗兰语义色（与分类发光映射中「转账」一致）
    val transferColor = Color(0xFF8B5CF6)
    val activeColor = when (selectedTypeIndex) {
        1 -> colorScheme.incomeColor
        2 -> transferColor
        else -> colorScheme.expenseColor
    }

    // 转账对端账户（转入账户）；编辑既有转账回填，新建默认取与转出不同的第一个账户
    var transferToAccountId by remember {
        mutableLongStateOf(
            expenseToEdit?.transferToAccountId?.takeIf { it != 0L }
                ?: accounts.firstOrNull { it.id != (expenseToEdit?.accountId ?: accounts.firstOrNull()?.id) }?.id
                ?: accounts.firstOrNull()?.id
                ?: 1L
        )
    }

    /** 更换转出端后保证对端有效且不等于转出端 */
    fun ensureTransferTargetValid(fromId: Long) {
        if (!accounts.any { it.id == transferToAccountId } || transferToAccountId == fromId) {
            transferToAccountId = accounts.firstOrNull { it.id != fromId }?.id ?: fromId
        }
    }

    var categoriesRefreshKey by remember { mutableIntStateOf(0) }
    val allCategories = remember(currentType, categoriesRefreshKey, allExpenses) {
        val baseCategories = CategoryManager.getCategories(context, currentType)
        
        // Smart Category Sorting based on usage frequency
        val frequencyMap = allExpenses
            .filter { it.type == currentType }
            .groupingBy { it.category }
            .eachCount()
            
        baseCategories.sortedByDescending { frequencyMap[it.name] ?: 0 }
    }

    // 1. Category state
    var selectedCategory by remember(currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (expenseToEdit != null && allCategories.any { it.name == expenseToEdit.category }) {
                expenseToEdit.category
            } else if (isExpense) {
                "餐饮"
            } else {
                allCategories.firstOrNull()?.name ?: "工资"
            }
        )
    }

    // Subcategory state
    var selectedSubCategory by remember(selectedCategory, currentType, categoriesRefreshKey) {
        mutableStateOf(
            if (expenseToEdit != null && expenseToEdit.category == selectedCategory && expenseToEdit.subCategory.isNotBlank()) {
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

    // 2. Account state (Defaults to the most recent account used for the same category!)
    fun getRecentAccountForCategory(cat: String, type: String): Long {
        if (expenseToEdit != null) {
            return expenseToEdit.accountId
        }
        val recentExpenseWithSameCat = allExpenses
            .filter { it.type == type && it.category == cat }
            .maxByOrNull { it.dateTimestamp }

        if (recentExpenseWithSameCat != null && accounts.any { it.id == recentExpenseWithSameCat.accountId }) {
            return recentExpenseWithSameCat.accountId
        }
        return accounts.firstOrNull()?.id ?: 1L
    }

    var selectedAccountId by remember {
        mutableLongStateOf(
            if (expenseToEdit != null) expenseToEdit.accountId
            else getRecentAccountForCategory(selectedCategory, currentType)
        )
    }

    // Whenever category changes in Add mode, auto-switch default account to most recent for that category
    LaunchedEffect(selectedCategory, currentType) {
        if (expenseToEdit == null) {
            val autoAccId = getRecentAccountForCategory(selectedCategory, currentType)
            if (accounts.any { it.id == autoAccId }) {
                selectedAccountId = autoAccId
            }
        }
    }

    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()

    // 3. Timestamp state
    var selectedTimestamp by remember {
        mutableLongStateOf(expenseToEdit?.dateTimestamp ?: initialTimestamp)
    }

    // 4. Amount and Note states
    var amountInput by remember {
        mutableStateOf(if (expenseToEdit != null) expenseToEdit.amount.toString() else "")
    }
    var noteInput by remember {
        mutableStateOf(expenseToEdit?.note ?: "")
    }

    // Pickers modal dialog flags
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubCategoryDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            borderColor = Brush.linearGradient(
                listOf(
                    activeColor.copy(alpha = 0.45f),
                    Color.White.copy(alpha = 0.15f)
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (expenseToEdit == null) "记一笔新账目" else "编辑账目",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Segmented Switch (支出 / 收入)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))
                        .padding(4.dp)
                ) {
                    // Expense Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTypeIndex == 0) colorScheme.expenseColor else Color.Transparent)
                            .clickable {
                                selectedTypeIndex = 0
                                selectedCategory = "餐饮"
                                selectedSubCategory = CategoryManager.getDefaultSubcategory(context, "餐饮", "EXPENSE", true)
                                selectedAccountId = getRecentAccountForCategory("餐饮", "EXPENSE")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "支出 (-)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 0) Color.White else bgConfig.textSecondary
                        )
                    }

                    // Income Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTypeIndex == 1) colorScheme.incomeColor else Color.Transparent)
                            .clickable {
                                selectedTypeIndex = 1
                                val incCats = CategoryManager.getCategories(context, "INCOME")
                                val firstCat = incCats.firstOrNull()?.name ?: "工资"
                                selectedCategory = firstCat
                                selectedSubCategory = CategoryManager.getDefaultSubcategory(context, firstCat, "INCOME", false)
                                selectedAccountId = getRecentAccountForCategory(firstCat, "INCOME")
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "收入 (+)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 1) Color.White else bgConfig.textSecondary
                        )
                    }

                    // Transfer Tab（账户间资金划转：隐藏分类，双端一条记录）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTypeIndex == 2) transferColor else Color.Transparent)
                            .clickable {
                                selectedTypeIndex = 2
                                ensureTransferTargetValid(selectedAccountId)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "转账 (⇌)",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 2) Color.White else bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Display Field (Read only, driven by custom numpad)
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("记账金额", color = bgConfig.textSecondary) },
                    placeholder = { Text("0.00", color = bgConfig.textTertiary) },
                    leadingIcon = {
                        Text(
                            text = "¥",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = activeColor,
                            modifier = Modifier.padding(start = 14.dp, end = 4.dp)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = activeColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Structured 3 Options Row: (时间) (账户) (类别·细分类别)
                Text(
                    text = "账目要素与归属 (点击切换)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = bgConfig.textTertiary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option 1: 【时间】
                    val dateFormatted = remember(selectedTimestamp) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        val todayCal = Calendar.getInstance()
                        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                        val timePart = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(selectedTimestamp))
                        if (isToday) "今天 $timePart" else sdf.format(Date(selectedTimestamp))
                    }

                    SelectorOptionCard(
                        icon = Icons.Default.Event,
                        iconTint = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        label = "时间",
                        value = dateFormatted,
                        onClick = { showTimePickerSheet = true }
                    )

                    // Option 2: 【账户】 (Defaults to same category's most recent account!)
                    val accountDisplay = selectedAccount?.name ?: "选择账户"
                    val accTypeIcon = when (selectedAccount?.type) {
                        "WECHAT" -> Icons.Default.Payment
                        "ALIPAY" -> Icons.Default.CreditCard
                        "BANK" -> Icons.Default.AccountBalance
                        else -> Icons.Default.AccountBalanceWallet
                    }

                    SelectorOptionCard(
                        icon = accTypeIcon,
                        iconTint = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                        label = if (isTransfer) "转出账户" else "账户",
                        value = accountDisplay,
                        subValue = if (selectedAccount != null) "余额 ¥${String.format(Locale.CHINA, "%,.2f", selectedAccount.balance)}" else null,
                        onClick = { showAccountPickerSheet = true }
                    )

                    // 转账专属：转入账户横滑选择器（复用玻璃 Chip 样式）
                    if (isTransfer) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "转入账户",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary
                                )
                                if (!accounts.any { it.id == transferToAccountId && it.id != selectedAccountId }) {
                                    Text(
                                        text = if (accounts.size < 2) "需要至少两个不同账户" else "请选择收款账户",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(accounts.filter { it.id != selectedAccountId }) { acc ->
                                    val isTargetSelected = acc.id == transferToAccountId
                                    GlassChip(
                                        selected = isTargetSelected,
                                        onClick = { transferToAccountId = acc.id },
                                        selectedGlowColor = transferColor
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = when (acc.type) {
                                                    "WECHAT" -> Icons.Default.Payment
                                                    "ALIPAY" -> Icons.Default.CreditCard
                                                    "BANK_CARD", "BANK" -> Icons.Default.AccountBalance
                                                    else -> Icons.Default.AccountBalanceWallet
                                                },
                                                contentDescription = null,
                                                tint = if (isTargetSelected) {
                                                    (if (bgConfig.isLight) Color(0xFF6D28D9) else Color.White)
                                                } else bgConfig.textTertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isTargetSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isTargetSelected) {
                                                    (if (bgConfig.isLight) Color(0xFF6D28D9) else Color.White)
                                                } else bgConfig.textSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Option 3: 【类别 · 细分类别】（转账无分类语义，隐藏）
                    if (!isTransfer) {
                        val catGlow = CategoryManager.getCategoryGlowColor(selectedCategory)
                        val catCombinedDisplay = if (selectedSubCategory.isNotBlank()) {
                            "$selectedCategory · $selectedSubCategory"
                        } else {
                            selectedCategory
                        }

                        SelectorOptionCard(
                            icon = CategoryManager.getCategoryIcon(selectedCategory),
                            iconTint = catGlow,
                            label = "类别·细分",
                            value = catCombinedDisplay,
                            onClick = { showCategoryPickerSheet = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Note Input Field (Preserved below)
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { noteInput = it },
                    label = { Text("备注说明 (选填)", color = bgConfig.textSecondary) },
                    placeholder = { Text("如：朋友聚餐、买咖啡、乘地铁", color = bgConfig.textTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = activeColor
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_note_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Replaced Save Button with CustomNumpad
                val isAccountSelected = selectedAccount != null
                val transferTargetValid = !isTransfer ||
                    (transferToAccountId != selectedAccountId &&
                        accounts.any { it.id == transferToAccountId })

                if (!isAccountSelected && accounts.isNotEmpty()) {
                    Text(
                        text = "⚠️ 请先选择一个结算账户",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else if (isTransfer && !transferTargetValid) {
                    Text(
                        text = "⚠️ " + if (accounts.size < 2) "转账需要至少两个不同账户" else "请选择与转出不同的转入账户",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                CustomNumpad(
                    expression = amountInput,
                    onExpressionChange = { amountInput = it },
                    onConfirm = {
                        val amount = amountInput.toDoubleOrNull() ?: 0.0
                        if (amount > 0 && isAccountSelected && transferTargetValid) {
                            if (!isTransfer && selectedSubCategory.isNotBlank()) {
                                CategoryManager.saveLastSelectedSubcategory(context, selectedCategory, selectedSubCategory)
                            }
                            onConfirm(
                                when (selectedTypeIndex) {
                                    1 -> "INCOME"
                                    2 -> "TRANSFER"
                                    else -> "EXPENSE"
                                },
                                if (isTransfer) "" else selectedCategory,
                                if (isTransfer) "" else selectedSubCategory,
                                amount,
                                noteInput.trim(),
                                selectedAccount!!.id,
                                selectedAccount!!.name,
                                selectedTimestamp,
                                if (isTransfer) transferToAccountId else null
                            )
                        }
                    },
                    confirmColor = activeColor,
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                )
            }
        }
    }

    // Modal Sheet 1: Time / Date Picker
    if (showTimePickerSheet) {
        TimePickerSheet(
            currentTimestamp = selectedTimestamp,
            onDismiss = { showTimePickerSheet = false },
            onSelectTimestamp = {
                selectedTimestamp = it
                showTimePickerSheet = false
            }
        )
    }

    // Modal Sheet 2: Account Selection Picker
    if (showAccountPickerSheet) {
        AccountPickerSheet(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            onDismiss = { showAccountPickerSheet = false },
            onSelectAccount = { acc ->
                selectedAccountId = acc.id
                if (isTransfer) ensureTransferTargetValid(acc.id)
                showAccountPickerSheet = false
            }
        )
    }

    // Modal Sheet 3: Category & Subcategory Picker
    if (showCategoryPickerSheet) {
        CategoryPickerSheet(
            currentType = currentType,
            allCategories = allCategories,
            selectedCategory = selectedCategory,
            selectedSubCategory = selectedSubCategory,
            onDismiss = { showCategoryPickerSheet = false },
            onSelectCategoryAndSub = { cat, sub ->
                selectedCategory = cat
                selectedSubCategory = sub
                showCategoryPickerSheet = false
            },
            onAddCategory = { showAddCategoryDialog = true },
            onAddSubCategory = { showAddSubCategoryDialog = true }
        )
    }

    // Dialog: Add Custom Major Category
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newSubCatListText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = bgConfig.dialogBackground,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "添加自定义大类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("分类名称", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：宠物、数码、兼职", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = newSubCatListText,
                        onValueChange = { newSubCatListText = it },
                        label = { Text("细分子项 (以逗号分隔，选填)", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：狗粮, 玩具, 疫苗", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = bgConfig.textSecondary
                            )
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newCatName.trim()
                                if (trimmed.isNotBlank()) {
                                    val subList = newSubCatListText.split(",", "，", " ")
                                        .map { it.trim() }
                                        .filter { it.isNotBlank() }
                                    CategoryManager.addCustomCategory(
                                        context = context,
                                        name = trimmed,
                                        type = currentType,
                                        subcategories = subList
                                    )
                                    categoriesRefreshKey++
                                    selectedCategory = trimmed
                                    selectedSubCategory = subList.firstOrNull() ?: "其他"
                                    showAddCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("添加分类", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Add Custom Subcategory
    if (showAddSubCategoryDialog) {
        var newSubName by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddSubCategoryDialog = false }) {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
                backgroundColor = bgConfig.dialogBackground,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "为「$selectedCategory」添加细分项目",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("细分名称", color = bgConfig.textSecondary) },
                        placeholder = { Text("如：外卖、打车券、自制甜品", color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { showAddSubCategoryDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Transparent,
                                contentColor = bgConfig.textSecondary
                            )
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val trimmed = newSubName.trim()
                                if (trimmed.isNotBlank()) {
                                    CategoryManager.addCustomSubcategory(
                                        context = context,
                                        categoryName = selectedCategory,
                                        newSubcategory = trimmed
                                    )
                                    categoriesRefreshKey++
                                    selectedSubCategory = trimmed
                                    showAddSubCategoryDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("添加细分", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectorOptionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    label: String,
    value: String,
    subValue: String? = null,
    onClick: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (bgConfig.isLight) Color(0xFFF8FAFC) else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = 1.dp,
                color = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconTint.copy(alpha = 0.16f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (subValue != null) {
                    Text(
                        text = subValue,
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textSecondary,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = bgConfig.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Modal Sheet: Time / Date Picker
@Composable
private fun TimePickerSheet(
    currentTimestamp: Long,
    onDismiss: () -> Unit,
    onSelectTimestamp: (Long) -> Unit
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current

    val cal = remember {
        Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E293B),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择记账时间",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = bgConfig.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick preset date buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Today
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bgConfig.isLight) Color(0xFFEEF2FF) else Color(0xFF312E81).copy(alpha = 0.4f))
                            .clickable {
                                val now = Calendar.getInstance()
                                onSelectTimestamp(now.timeInMillis)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "现在 / 今天", fontWeight = FontWeight.Bold, color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan)
                    }

                    // Yesterday
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
                            .clickable {
                                val yCal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -1)
                                }
                                onSelectTimestamp(yCal.timeInMillis)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "昨天", fontWeight = FontWeight.Medium, color = bgConfig.textPrimary)
                    }

                    // Day before yesterday
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
                            .clickable {
                                val bCal = Calendar.getInstance().apply {
                                    add(Calendar.DAY_OF_YEAR, -2)
                                }
                                onSelectTimestamp(bCal.timeInMillis)
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "前天", fontWeight = FontWeight.Medium, color = bgConfig.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Date Picker Trigger
                Button(
                    onClick = {
                        val dpd = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                cal.set(Calendar.YEAR, year)
                                cal.set(Calendar.MONTH, month)
                                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                                // After picking date, open TimePicker for precise time
                                val tpd = TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        cal.set(Calendar.MINUTE, minute)
                                        onSelectTimestamp(cal.timeInMillis)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    true
                                )
                                tpd.show()
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                        dpd.show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bgConfig.isLight) Color(0xFF6366F1) else Color(0xFF4F46E5)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) {
                    Icon(imageVector = Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("选择指定年月日与时间", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

// Modal Sheet: Account Picker List
@Composable
private fun AccountPickerSheet(
    accounts: List<AccountEntity>,
    selectedAccountId: Long,
    onDismiss: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E293B),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择结算账户",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = bgConfig.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (accounts.isEmpty()) {
                    Text(
                        text = "暂无可用账户，请前往「账户」页添加",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(accounts) { acc ->
                            val isSelected = acc.id == selectedAccountId
                            val accIcon = when (acc.type) {
                                "WECHAT" -> Icons.Default.Payment
                                "ALIPAY" -> Icons.Default.CreditCard
                                "BANK" -> Icons.Default.AccountBalance
                                else -> Icons.Default.AccountBalanceWallet
                            }
                            val itemColor = if (isSelected) (if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan) else bgConfig.textPrimary

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) {
                                            if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.35f)
                                        } else {
                                            if (bgConfig.isLight) Color(0xFFF8FAFC) else Color.White.copy(alpha = 0.05f)
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.5.dp else 0.5.dp,
                                        color = if (isSelected) itemColor else (if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectAccount(acc) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(
                                                    if (isSelected) itemColor.copy(alpha = 0.2f) else (if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.1f)),
                                                    CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = accIcon,
                                                contentDescription = null,
                                                tint = itemColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = acc.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = bgConfig.textPrimary
                                            )
                                            if (acc.cardSuffix.isNotBlank()) {
                                                Text(
                                                    text = "尾号 ${acc.cardSuffix}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = bgConfig.textTertiary
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "¥${String.format(Locale.CHINA, "%,.2f", acc.balance)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = itemColor
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = itemColor,
                                                modifier = Modifier.size(18.dp)
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
    }
}

// Modal Sheet: Category & Subcategory Picker
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerSheet(
    currentType: String,
    allCategories: List<CategoryItem>,
    selectedCategory: String,
    selectedSubCategory: String,
    onDismiss: () -> Unit,
    onSelectCategoryAndSub: (String, String) -> Unit,
    onAddCategory: () -> Unit,
    onAddSubCategory: () -> Unit
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current

    var tempMajorCategory by remember { mutableStateOf(selectedCategory) }
    val currentSubcategories = remember(tempMajorCategory, currentType) {
        CategoryManager.getSubcategories(context, tempMajorCategory, currentType)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E293B),
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择消费类别与细分",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭", tint = bgConfig.textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 1. Major Category Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "主分类",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textSecondary
                    )
                    Text(
                        text = "+ 新增大类",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        modifier = Modifier.clickable { onAddCategory() }.padding(2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(allCategories) { cat ->
                        val isSelected = tempMajorCategory == cat.name
                        val catGlow = CategoryManager.getCategoryGlowColor(cat.name)
                        GlassChip(
                            selected = isSelected,
                            onClick = { tempMajorCategory = cat.name },
                            selectedGlowColor = catGlow
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = CategoryManager.getCategoryIcon(cat.name),
                                    contentDescription = null,
                                    tint = if (isSelected) catGlow else bgConfig.textTertiary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) catGlow else bgConfig.textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Subcategory Flow Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "「$tempMajorCategory」细分子项",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textSecondary
                    )
                    Text(
                        text = "+ 添加细分",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowCyan,
                        modifier = Modifier.clickable { onAddSubCategory() }.padding(2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp).verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currentSubcategories.forEach { sub ->
                        val isSelected = tempMajorCategory == selectedCategory && selectedSubCategory == sub
                        val catGlow = CategoryManager.getCategoryGlowColor(tempMajorCategory)

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) catGlow.copy(alpha = 0.2f)
                                    else (if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.White.copy(alpha = 0.08f))
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) catGlow else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onSelectCategoryAndSub(tempMajorCategory, sub)
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) catGlow else bgConfig.textPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
