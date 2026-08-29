package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.AccountEntity
import com.example.data.local.CategoryManager
import com.example.data.local.ExpenseEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
        transferToAccountId: Long?
    ) -> Unit
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current

    // Segmented Type (0 = 支出, 1 = 收入, 2 = 转账)
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

    val expenseGreen = Color(0xFF10B981)
    val incomeRed = Color(0xFFEF4444)
    val transferViolet = Color(0xFF8B5CF6)
    val activeThemeColor = when (selectedTypeIndex) {
        1 -> incomeRed
        2 -> transferViolet
        else -> expenseGreen
    }

    var transferToAccountId by remember {
        mutableLongStateOf(
            expenseToEdit?.transferToAccountId?.takeIf { it != 0L }
                ?: accounts.firstOrNull { it.id != (expenseToEdit?.accountId ?: accounts.firstOrNull()?.id) }?.id
                ?: accounts.firstOrNull()?.id
                ?: 1L
        )
    }

    fun ensureTransferTargetValid(fromId: Long) {
        if (!accounts.any { it.id == transferToAccountId } || transferToAccountId == fromId) {
            transferToAccountId = accounts.firstOrNull { it.id != fromId }?.id ?: fromId
        }
    }

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

    fun getRecentAccountForCategory(cat: String, type: String): Long {
        if (expenseToEdit != null) {
            return expenseToEdit.accountId
        }
        val recentExpense = allExpenses
            .filter { it.type == type && it.category == cat }
            .maxByOrNull { it.dateTimestamp }
        if (recentExpense != null && accounts.any { it.id == recentExpense.accountId }) {
            return recentExpense.accountId
        }
        return accounts.firstOrNull()?.id ?: 1L
    }

    var selectedAccountId by remember {
        mutableLongStateOf(
            if (expenseToEdit != null) expenseToEdit.accountId
            else getRecentAccountForCategory(selectedCategory, currentType)
        )
    }

    LaunchedEffect(selectedCategory, currentType) {
        if (expenseToEdit == null && !isTransfer) {
            val autoAccId = getRecentAccountForCategory(selectedCategory, currentType)
            if (accounts.any { it.id == autoAccId }) {
                selectedAccountId = autoAccId
            }
        }
    }

    val selectedAccount = accounts.find { it.id == selectedAccountId } ?: accounts.firstOrNull()
    val targetTransferAccount = accounts.find { it.id == transferToAccountId }

    var selectedTimestamp by remember {
        mutableLongStateOf(expenseToEdit?.dateTimestamp ?: initialTimestamp)
    }

    var amountInput by remember {
        mutableStateOf(if (expenseToEdit != null) expenseToEdit.amount.toString() else "")
    }
    var noteInput by remember {
        mutableStateOf(expenseToEdit?.note ?: "")
    }

    // Modal Sheet states
    var showTimePickerSheet by remember { mutableStateOf(false) }
    var showAccountPickerSheet by remember { mutableStateOf(false) }
    var showTransferTargetPickerSheet by remember { mutableStateOf(false) }
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubCategoryDialog by remember { mutableStateOf(false) }

    fun doSave(closeOnFinish: Boolean) {
        val calculatedAmount = if (amountInput.isNotEmpty()) {
            evaluateExpression(amountInput).toDoubleOrNull() ?: 0.0
        } else 0.0

        val isAccountSelected = selectedAccount != null
        val transferTargetValid = !isTransfer ||
            (transferToAccountId != selectedAccountId && accounts.any { it.id == transferToAccountId })

        if (calculatedAmount > 0 && isAccountSelected && transferTargetValid) {
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
            if (!closeOnFinish) {
                // "再记": reset amount and note, keep category/account/time
                amountInput = ""
                noteInput = ""
            }
        }
    }

    BackHandler(onBack = onDismiss)

    GlassBackgroundWithGlow(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // 1. Top Navigation Bar with Expense/Income/Transfer Tabs inline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = bgConfig.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Centered Type Tabs
                Row(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tabs = listOf("支出", "收入", "转账")
                    tabs.forEachIndexed { index, title ->
                        val isSelected = selectedTypeIndex == index
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    selectedTypeIndex = index
                                    if (index == 0) {
                                        selectedCategory = "餐饮"
                                        selectedSubCategory = CategoryManager.getDefaultSubcategory(context, "餐饮", "EXPENSE", true)
                                    } else if (index == 1) {
                                        val lastInc = CategoryManager.getLastIncomeCategory(context)
                                        val incCats = CategoryManager.getCategories(context, "INCOME")
                                        val defaultCat = if (lastInc.isNotBlank() && incCats.any { it.name == lastInc }) lastInc
                                            else incCats.firstOrNull()?.name ?: "工资薪水"
                                        selectedCategory = defaultCat
                                        selectedSubCategory = defaultCat
                                    } else if (index == 2) {
                                        ensureTransferTargetValid(selectedAccountId)
                                    }
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) bgConfig.textPrimary else bgConfig.textTertiary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isSelected) activeThemeColor else Color.Transparent)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Floating Hero Amount Card (Frosted Glass Card)
            val catIcon = if (isTransfer) Icons.Default.SwapHoriz else if (isIncome) CategoryManager.getCategoryIcon(selectedCategory) else getSubcategoryIcon(selectedSubCategory, selectedCategory)
            val catGlow = if (isTransfer) transferViolet else CategoryManager.getCategoryGlowColor(selectedCategory)
            val heroDisplayName = if (isTransfer) "转账" else if (isIncome) selectedCategory else if (selectedSubCategory.isNotBlank() && selectedSubCategory != "其他") selectedSubCategory else selectedCategory
            val heroSubtitle = if (isTransfer) "账户间资金划转" else if (isIncome) "" else if (selectedSubCategory.isNotBlank() && selectedSubCategory != "其他") selectedCategory else ""

            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Icon + Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clickable {
                                if (isExpense) showCategoryPickerSheet = true
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(catGlow.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, catGlow.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = catIcon,
                                contentDescription = heroDisplayName,
                                tint = catGlow,
                                modifier = Modifier.size(24.dp)
                            )
                            if (isExpense) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(12.dp)
                                        .background(activeThemeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = heroDisplayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary,
                                maxLines = 1
                            )
                            if (heroSubtitle.isNotBlank()) {
                                Text(
                                    text = heroSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = bgConfig.textTertiary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Right: Large Amount Display
                    val displayAmount = if (amountInput.isEmpty()) "0.00" else amountInput
                    Text(
                        text = displayAmount,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = if (displayAmount.length > 8) 28.sp else 34.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = bgConfig.textPrimary,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("expense_amount_display")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 4. Middle Content: 4-Column Category Grid or Transfer View
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                if (isTransfer) {
                    // Transfer View: From Account -> To Account (Glass Card Style)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "资金调拨与转账",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textSecondary
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // From Card
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { showAccountPickerSheet = true }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text("转出账户", style = MaterialTheme.typography.labelSmall, color = bgConfig.textTertiary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = selectedAccount?.name ?: "选择账户",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "转入",
                                tint = activeThemeColor,
                                modifier = Modifier.padding(horizontal = 8.dp).size(24.dp)
                            )

                            // To Card
                            GlassCard(
                                modifier = Modifier
                                    .weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { showTransferTargetPickerSheet = true }
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    Text("转入账户", style = MaterialTheme.typography.labelSmall, color = bgConfig.textTertiary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = targetTransferAccount?.name ?: "选择目标账户",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = bgConfig.textPrimary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // 4-Column Category Grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        items(allCategories, key = { it.name }) { cat ->
                            val isSelected = selectedCategory == cat.name
                            val itemGlow = CategoryManager.getCategoryGlowColor(cat.name)
                            val itemIcon = CategoryManager.getCategoryIcon(cat.name)
                            val currentSub = if (isSelected) selectedSubCategory else ""

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple()
                                    ) {
                                        selectedCategory = cat.name
                                        if (isIncome) {
                                            selectedSubCategory = cat.name
                                        } else {
                                            selectedSubCategory = CategoryManager.getDefaultSubcategory(
                                                context = context,
                                                categoryName = cat.name,
                                                type = currentType,
                                                isFreshCreation = false
                                            )
                                            // Open subcategory picker drawer
                                            showCategoryPickerSheet = true
                                        }
                                    }
                                    .padding(vertical = 6.dp, horizontal = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) itemGlow
                                            else if (bgConfig.isLight) Color(0xFFEDF2F7).copy(alpha = 0.75f)
                                            else Color(0xFF232630).copy(alpha = 0.65f)
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.85f)
                                            else (if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = itemIcon,
                                        contentDescription = cat.name,
                                        tint = if (isSelected) Color.White else (if (bgConfig.isLight) Color(0xFF4A5568) else Color(0xFFA0AEC0)),
                                        modifier = Modifier.size(26.dp)
                                    )
                                    if (isExpense) {
                                        // Small down tag on bottom right
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(3.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White.copy(alpha = 0.9f) else bgConfig.textTertiary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) itemGlow else bgConfig.textPrimary,
                                    maxLines = 1
                                )

                                if (isExpense && isSelected && currentSub.isNotBlank() && currentSub != "其他" && currentSub != cat.name) {
                                    Text(
                                        text = currentSub,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = bgConfig.textTertiary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val focusManager = LocalFocusManager.current
            val density = LocalDensity.current
            val isImeOpen = WindowInsets.ime.getBottom(density) > 0

            // 5. Note Input Field & Metadata Capsules Row (Frosted Glass Style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                // Note input line with glass card container
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = bgConfig.textTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = bgConfig.textPrimary,
                                fontSize = 14.sp
                            ),
                            cursorBrush = SolidColor(activeThemeColor),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (noteInput.isEmpty()) {
                                        Text(
                                            text = "输入备注...",
                                            fontSize = 14.sp,
                                            color = bgConfig.textTertiary
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("expense_note_input")
                        )
                        if (noteInput.isNotEmpty()) {
                            IconButton(
                                onClick = { noteInput = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "清除",
                                    tint = bgConfig.textTertiary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Metadata Capsules (Time capsule + Account capsule) - Clean, Camera icon removed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Time Capsule: e.g. "今天 16:59"
                    val timeFormatted = remember(selectedTimestamp) {
                        val cal = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
                        val todayCal = Calendar.getInstance()
                        val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
                        val timePart = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(selectedTimestamp))
                        if (isToday) "今天 $timePart" else SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(selectedTimestamp))
                    }

                    CapsuleChip(
                        text = timeFormatted,
                        icon = Icons.Default.AccessTime,
                        onClick = { showTimePickerSheet = true }
                    )

                    // Account Capsule: e.g. "微信钱包"
                    val accName = selectedAccount?.name ?: "选择账户"
                    CapsuleChip(
                        text = accName,
                        icon = Icons.Default.AccountBalanceWallet,
                        onClick = { showAccountPickerSheet = true }
                    )
                }
            }

            // 6. Professional Integrated Keypad - Smooth animated transition to eliminate jitter
            AnimatedVisibility(
                visible = !isImeOpen,
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                    expandFrom = Alignment.Bottom
                ),
                exit = fadeOut(animationSpec = tween(120)) + shrinkVertically(
                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                    shrinkTowards = Alignment.Bottom
                )
            ) {
                AccountingNumpad(
                    expression = amountInput,
                    onExpressionChange = { amountInput = it },
                    onConfirm = { doSave(closeOnFinish = true) },
                    onSaveAndNext = { doSave(closeOnFinish = false) },
                    confirmColor = activeThemeColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(216.dp)
                        .padding(bottom = 2.dp)
                )
            }
        }
    }

    // Modal Sheet 1: Wheel Time Picker
    if (showTimePickerSheet) {
        WheelTimePickerSheet(
            initialTimestamp = selectedTimestamp,
            onDismiss = { showTimePickerSheet = false },
            onConfirm = {
                selectedTimestamp = it
                showTimePickerSheet = false
            },
            accentColor = activeThemeColor
        )
    }

    // Modal Sheet 2: Account Picker Sheet
    if (showAccountPickerSheet) {
        val recentAccountIds = remember(allExpenses) {
            allExpenses
                .sortedByDescending { it.dateTimestamp }
                .map { it.accountId }
                .distinct()
                .take(2)
        }
        AccountPickerSheet(
            accounts = accounts,
            selectedAccountId = selectedAccountId,
            recentAccountIds = recentAccountIds,
            onDismiss = { showAccountPickerSheet = false },
            onSelectAccount = { acc ->
                selectedAccountId = acc.id
                if (isTransfer) ensureTransferTargetValid(acc.id)
                showAccountPickerSheet = false
            },
            accentColor = activeThemeColor
        )
    }

    // Modal Sheet 2.1: Transfer Target Account Picker Sheet
    if (showTransferTargetPickerSheet) {
        AccountPickerSheet(
            accounts = accounts.filter { it.id != selectedAccountId },
            selectedAccountId = transferToAccountId,
            onDismiss = { showTransferTargetPickerSheet = false },
            onSelectAccount = { acc ->
                transferToAccountId = acc.id
                showTransferTargetPickerSheet = false
            },
            accentColor = activeThemeColor
        )
    }

    // Modal Sheet 3: Category & Subcategory Picker
    if (showCategoryPickerSheet && !isTransfer) {
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
            onAddSubCategory = { showAddSubCategoryDialog = true },
            accentColor = activeThemeColor
        )
    }

    // Dialog: Add Custom Major Category
    if (showAddCategoryDialog) {
        var newCatName by remember { mutableStateOf("") }
        var newSubCatListText by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showAddCategoryDialog = false }) {
            GlassCard(
                shape = RoundedCornerShape(22.dp),
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
                        TextButton(onClick = { showAddCategoryDialog = false }) {
                            Text("取消", color = bgConfig.textSecondary)
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
                            colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
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
                        TextButton(onClick = { showAddSubCategoryDialog = false }) {
                            Text("取消", color = bgConfig.textSecondary)
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
                            colors = ButtonDefaults.buttonColors(containerColor = activeThemeColor),
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
private fun CapsuleChip(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    GlassCard(
        modifier = Modifier,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = bgConfig.textTertiary,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = bgConfig.textPrimary
            )
        }
    }
}
