import re

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'r') as f:
    content = f.read()

# 1. Update Top Bar Signature
content = content.replace("onToggleLightDark: () -> Unit", "onOpenSettings: () -> Unit")
content = content.replace("onToggleLightDark = { forceDarkPreview = !(isLight) }", "onOpenSettings = { showSettingsDialog = true }")
content = content.replace(
"""        // 昼夜预览微调
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
        }""",
"""        // 设置
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.size(34.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "键盘布局设置",
                tint = inkSecondary,
                modifier = Modifier.size(17.dp)
            )
        }"""
)

# 2. Add Settings State in EditorialExpenseAddEditScreen
prefs_injection = """    var forceDarkPreview by remember { mutableStateOf(false) }

    val prefs = context.getSharedPreferences("expense_settings", android.content.Context.MODE_PRIVATE)
    var keyboardLayout by remember { mutableIntStateOf(prefs.getInt("keyboard_layout", 0)) }
    var showSettingsDialog by remember { mutableStateOf(false) }"""

content = content.replace("    var forceDarkPreview by remember { mutableStateOf(false) }", prefs_injection)

# 3. Pass keyboardLayout down
numpad_call = """            TactileFiveColumnNumpad(
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
            )"""
new_numpad_call = """            TactileFiveColumnNumpad(
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
                isLight = isLight,
                keyboardLayout = keyboardLayout
            )"""
content = content.replace(numpad_call, new_numpad_call)

# 4. Modify TactileFiveColumnNumpad
old_numpad_def = """private fun TactileFiveColumnNumpad(
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
) {"""
new_numpad_def = """private fun TactileFiveColumnNumpad(
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
    isLight: Boolean,
    keyboardLayout: Int
) {"""
content = content.replace(old_numpad_def, new_numpad_def)

old_row1 = """        // 第一行：7, 8, 9, ÷, ⌫ (退格)
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
            }"""
new_row1 = """        val r1 = if (keyboardLayout == 1) listOf("1", "2", "3") else listOf("7", "8", "9")
        val r3 = if (keyboardLayout == 1) listOf("7", "8", "9") else listOf("1", "2", "3")

        // 第一行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TactileNumpadBtn(r1[0], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput(r1[0], expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn(r1[1], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput(r1[1], expression, onExpressionChange, onConfirm)
            }
            TactileNumpadBtn(r1[2], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                handleInput(r1[2], expression, onExpressionChange, onConfirm)
            }"""
content = content.replace(old_row1, new_row1)

old_row3 = """                // Row 3 (1, 2, 3, -)
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
                    }"""
new_row3 = """                // Row 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TactileNumpadBtn(r3[0], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput(r3[0], expression, onExpressionChange, onConfirm)
                    }
                    TactileNumpadBtn(r3[1], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput(r3[1], expression, onExpressionChange, onConfirm)
                    }
                    TactileNumpadBtn(r3[2], isLight, chipSurface, inkPrimary, borderSubtle, Modifier.weight(1f)) {
                        handleInput(r3[2], expression, onExpressionChange, onConfirm)
                    }"""
content = content.replace(old_row3, new_row3)


# 5. Add Settings Dialog UI in the main Screen
settings_dialog_code = """
            // ── 5. 设置键盘布局对话框 ───────────────────────────────────
            if (showSettingsDialog) {
                Dialog(onDismissRequest = { showSettingsDialog = false }) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(paperBg)
                            .padding(24.dp)
                    ) {
                        Column {
                            Text("键盘布局", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = inkPrimary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // 布局 0
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        keyboardLayout = 0
                                        prefs.edit().putInt("keyboard_layout", 0).apply()
                                        showSettingsDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = keyboardLayout == 0,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = activeAccentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("计算器标准布局", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = inkPrimary)
                                    Text("7-8-9 在上，1-2-3 在下", fontSize = 12.sp, color = inkSecondary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 布局 1
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        keyboardLayout = 1
                                        prefs.edit().putInt("keyboard_layout", 1).apply()
                                        showSettingsDialog = false
                                    }
                                    .padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = keyboardLayout == 1,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(selectedColor = activeAccentColor)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("拨号盘布局", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = inkPrimary)
                                    Text("1-2-3 在上，7-8-9 在下", fontSize = 12.sp, color = inkSecondary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { showSettingsDialog = false }) {
                                    Text("关闭", color = inkSecondary)
                                }
                            }
                        }
                    }
                }
            }"""

content = content.replace("        }\n    }\n}", "        }\n" + settings_dialog_code + "\n    }\n}")

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'w') as f:
    f.write(content)

