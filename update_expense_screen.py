import re

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'r') as f:
    content = f.read()

# 1. Color swap
content = content.replace(
"""    val activeAccentColor = when (selectedTypeIndex) {
        1 -> forestSage
        2 -> royalIndigo
        else -> clayAccent
    }""",
"""    val activeAccentColor = when (selectedTypeIndex) {
        1 -> clayAccent
        2 -> royalIndigo
        else -> forestSage
    }"""
)

# 2. Add isCategoryDrillDown state
content = content.replace(
"""    val currentSubcategories = remember(selectedCategory, currentType, categoriesRefreshKey) {""",
"""    var isCategoryDrillDown by remember { mutableStateOf(expenseToEdit != null) }
    LaunchedEffect(currentType) {
        if (expenseToEdit == null) {
            isCategoryDrillDown = false
        }
    }

    val currentSubcategories = remember(selectedCategory, currentType, categoriesRefreshKey) {"""
)

# 3. Update Categories layout and usages
old_categories_call = """                        // 一级大类：杂志风排版单行 (Magazine Primary Stream)
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
                        }"""

new_categories_call = """                        // 一级大类与二级联动动画 (Category Drill-down)
                        TactileMagazineCategoryRow(
                            categories = allCategories,
                            selectedCategory = selectedCategory,
                            isCategoryDrillDown = isCategoryDrillDown,
                            activeColor = activeAccentColor,
                            inkPrimary = inkPrimary,
                            inkSecondary = inkSecondary,
                            onSelectCategory = { catName ->
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                if (isCategoryDrillDown && selectedCategory == catName) {
                                    // Reselecting primary
                                    isCategoryDrillDown = false
                                } else {
                                    selectedCategory = catName
                                    isCategoryDrillDown = true
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
                                }
                            },
                            onAddCategory = { showAddCategoryDialog = true }
                        )

                        // 二级细分：纸张微浮雕实体印章胶囊 (Tactile Paper Chips)
                        AnimatedVisibility(
                            visible = isCategoryDrillDown && isExpense && currentSubcategories.isNotEmpty(),
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
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
                        }"""
content = content.replace(old_categories_call, new_categories_call)

# 4. Remove quick notes array
quick_notes_pattern = re.compile(r'    // 灵感便签标签池\s*val quickNotes = remember.*?    }\s*// 保存逻辑', re.DOTALL)
content = quick_notes_pattern.sub('    // 保存逻辑', content)

# 5. Move MetaSlip to bottom of container
old_layout = """                    // ── 3. 手帐便签纸条元数据行 (Dot-Grid Metadata Slip) ──────
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
            }"""

new_layout = """                }

                Spacer(modifier = Modifier.weight(1f))

                // ── 3. 手帐便签纸条元数据行 (Dot-Grid Metadata Slip) 紧贴键盘 ──────
                TactileDotGridMetaSlip(
                    selectedTimestamp = selectedTimestamp,
                    accountName = selectedAccount?.name ?: "选择账户",
                    isTransfer = isTransfer,
                    note = noteInput,
                    onNoteChange = { noteInput = it },
                    inkPrimary = inkPrimary,
                    inkSecondary = inkSecondary,
                    inkMuted = inkMuted,
                    paperSlipBg = paperSlipBg,
                    borderSubtle = borderSubtle,
                    activeColor = activeAccentColor,
                    onOpenTimePicker = { showTimePickerSheet = true },
                    onOpenAccountPicker = { showAccountPickerSheet = true }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }"""
content = content.replace(old_layout, new_layout)

# 6. Update TacticalDotGridMetaSlip component
content = content.replace("    quickNotes: List<String>,", "")

old_metaslip_body_pattern = re.compile(r'    var showQuickTagPool by remember \{ mutableStateOf\(false\) \}.*?AnimatedVisibility.*?\}', re.DOTALL)
old_metaslip_content = """    var showQuickTagPool by remember { mutableStateOf(false) }

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
    }"""

new_metaslip_content = """    // 主便签条 (Dot-grid style slip)
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
            modifier = Modifier.weight(1f).padding(start = 2.dp),
            decorationBox = { innerTextField ->
                if (note.isEmpty()) {
                    Text("添写备注...", fontSize = 12.sp, color = inkMuted)
                }
                innerTextField()
            }
        )
    }"""
content = content.replace(old_metaslip_content, new_metaslip_content)


# 7. Modify TacticalAmbientAmountSection (Move amount right, keep ¥ left)
old_amount = """        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            // 算式即时求值微弱提示
            Box(
                modifier = Modifier.height(18.dp),
                contentAlignment = Alignment.CenterStart
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
                horizontalArrangement = Arrangement.Start,
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

                AnimatedContent(
                    targetState = if (expression.isEmpty()) "0.00" else expression,
                    transitionSpec = {
                        if (targetState.length > initialState.length) {
                            (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                        } else {
                            (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                        }
                    },
                    label = "amount_animation"
                ) { targetText ->
                    Text(
                        text = targetText,
                        fontSize = if (targetText.length > 9) 34.sp else 44.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        color = if (targetText == "0.00") inkMuted.copy(alpha = 0.5f) else inkPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

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
            }"""

new_amount = """        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            // 算式即时求值微弱提示
            Box(
                modifier = Modifier.height(18.dp).fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
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
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¥",
                    fontSize = 28.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontFamily = FontFamily.Serif,
                    color = activeColor,
                    modifier = Modifier.padding(bottom = 2.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    AnimatedContent(
                        targetState = if (expression.isEmpty()) "0.00" else expression,
                        transitionSpec = {
                            if (targetState.length > initialState.length) {
                                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
                            } else {
                                (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
                            }
                        },
                        label = "amount_animation"
                    ) { targetText ->
                        Text(
                            text = targetText,
                            fontSize = if (targetText.length > 9) 34.sp else 44.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            color = if (targetText == "0.00") inkMuted.copy(alpha = 0.5f) else inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

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
            }"""
content = content.replace(old_amount, new_amount)

# 8. Update TactileMagazineCategoryRow
old_mag = """@Composable
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
}"""

new_mag = """import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

@Composable
private fun TactileMagazineCategoryRow(
    categories: List<CategoryItem>,
    selectedCategory: String,
    isCategoryDrillDown: Boolean,
    activeColor: Color,
    inkPrimary: Color,
    inkSecondary: Color,
    onSelectCategory: (String) -> Unit,
    onAddCategory: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        categories.forEach { cat ->
            val isSelected = selectedCategory == cat.name
            val icon = CategoryManager.getCategoryIcon(cat.name)

            AnimatedVisibility(
                visible = !isCategoryDrillDown || isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            ) {
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
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) activeColor else inkSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )

                    Text(
                        text = cat.name,
                        fontSize = if (isSelected) 15.sp else 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) inkPrimary else inkSecondary.copy(alpha = 0.65f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = !isCategoryDrillDown,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        ) {
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
}"""
content = content.replace(old_mag, new_mag)


with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'w') as f:
    f.write(content)

