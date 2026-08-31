package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ExpenseEntity
import com.example.ui.components.EditorialPageHeader
import com.example.ui.theme.BackgroundConfig
import com.example.ui.theme.BackgroundOptionType
import com.example.ui.theme.ColorSchemeOption
import com.example.ui.theme.FontScaleOption
import com.example.ui.theme.LocalAppBackgroundConfig
import java.text.DecimalFormat

@Composable
fun MineScreen(
    expenses: List<ExpenseEntity>,
    accountsCount: Int,
    currentColorScheme: ColorSchemeOption,
    currentFontScale: FontScaleOption,
    currentBackgroundConfig: BackgroundConfig,
    onSelectColorScheme: (ColorSchemeOption) -> Unit,
    onSelectFontScale: (FontScaleOption) -> Unit,
    onSelectBackgroundConfig: (BackgroundConfig) -> Unit,
    onSetCustomColor: (String, String) -> Unit,
    onSetCustomImage: (String, Boolean) -> Unit,
    onSetCardAlpha: (Float) -> Unit,
    onSetBlurRadius: (Float) -> Unit,
    onSetFrostAlpha: (Float) -> Unit,
    onSetIsLight: (Boolean) -> Unit,
    onGenerateCsv: () -> String,
    onImportCsv: (String) -> Unit,
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onOpenBooks: () -> Unit,
    onOpenCategories: () -> Unit
) {
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val csv = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                onImportCsv(csv)
                Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val bgConfig = LocalAppBackgroundConfig.current
    val backgroundColor = if (bgConfig.isLight) Color(0xFFFAFAF7) else bgConfig.solidColor
    val textMain = bgConfig.textPrimary
    val textMuted = if (bgConfig.isLight) Color(0xFF8A8270) else Color(0xFFB0B0B0)
    val forestGreen = Color(0xFF2D6A4F)
    val dividerColor = bgConfig.dividerColor

    val totalRecords = expenses.size
    val continuousDays = remember(expenses) {
        if (expenses.isEmpty()) 0
        else {
            val earliest = expenses.minOf { it.dateTimestamp }
            val startCal = java.util.Calendar.getInstance().apply {
                timeInMillis = earliest
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val nowCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMs = nowCal.timeInMillis - startCal.timeInMillis
            val days = (diffMs / (1000L * 60 * 60 * 24)).toInt() + 1
            days.coerceAtLeast(1)
        }
    }

    var showThemeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        EditorialPageHeader(
            title = "Profile",
            subtitle = "PERSONAL CENTER",
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 10.dp, bottom = 24.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 8.dp)
        ) {
            item {
                ProfileInfoRow(
                    textMain = textMain,
                    textMuted = textMuted,
                    forestGreen = forestGreen,
                    dividerColor = dividerColor
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(24.dp))
                ProfileStatsRow(
                    totalRecords = totalRecords,
                    totalAccounts = accountsCount,
                    continuousDays = continuousDays,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor
                )
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            item {
                Text(
                    text = "PREFERENCES / 功能与偏好",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = textMuted,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
                
                ProfilePreferenceRow(
                    icon = Icons.Default.MenuBook,
                    title = "账本管理",
                    subtitle = "多账本切换、独立核算与数据管理",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = onOpenBooks
                )
                ProfilePreferenceRow(
                    icon = Icons.Outlined.Download,
                    title = "导出账单数据",
                    subtitle = "将账单数据导出为 CSV 文件",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = {
                        val csv = onGenerateCsv()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "导出 CSV")
                        context.startActivity(shareIntent)
                    }
                )
                ProfilePreferenceRow(
                    icon = Icons.Outlined.Upload,
                    title = "明细导入",
                    subtitle = "从其他平台导入账单",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = { filePickerLauncher.launch("text/*") }
                )
                ProfilePreferenceRow(
                    icon = Icons.Outlined.ColorLens,
                    title = "自定义背景风格",
                    subtitle = "保留亮色、暗色与自定义背景三大选项",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = { showThemeDialog = true }
                )
            }
        }
    }
    
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentConfig = currentBackgroundConfig,
            onSelectLight = {
                onSelectBackgroundConfig(
                    BackgroundConfig(
                        type = BackgroundOptionType.PURE_WHITE,
                        title = "亮色风格",
                        subtitle = "经典纸感纯白，清爽通透",
                        isLight = true,
                        solidColor = Color(0xFFFAFAF7)
                    )
                )
                showThemeDialog = false
            },
            onSelectDark = {
                onSelectBackgroundConfig(
                    BackgroundConfig(
                        type = BackgroundOptionType.SLATE_DARK,
                        title = "暗色风格",
                        subtitle = "极简玄武深岩，夜间护眼",
                        isLight = false,
                        solidColor = Color(0xFF242E24)
                    )
                )
                showThemeDialog = false
            },
            onSelectCustomColor = { hex: String, isLight: Boolean ->
                onSetCustomColor(hex, "自定义背景")
                onSetIsLight(isLight)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}

@Composable
fun ThemeSelectionDialog(
    currentConfig: BackgroundConfig,
    onSelectLight: () -> Unit,
    onSelectDark: () -> Unit,
    onSelectCustomColor: (hex: String, isLight: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedOption by remember {
        mutableStateOf(
            when (currentConfig.type) {
                BackgroundOptionType.PURE_WHITE, BackgroundOptionType.GRAY_WHITE, BackgroundOptionType.WARM_IVORY -> "LIGHT"
                BackgroundOptionType.SLATE_DARK, BackgroundOptionType.DEEP_COSMIC, BackgroundOptionType.AURORA_NIGHT -> "DARK"
                else -> if (currentConfig.type == BackgroundOptionType.CUSTOM_SOLID || currentConfig.type == BackgroundOptionType.CUSTOM_IMAGE) "CUSTOM"
                else if (currentConfig.isLight) "LIGHT" else "DARK"
            }
        )
    }

    var customHexInput by remember { mutableStateOf(currentConfig.customHex.ifEmpty { if (currentConfig.isLight) "#FAF6EE" else "#1F2922" }) }
    var customIsLight by remember { mutableStateOf(currentConfig.isLight) }

    val presetCustomColors = listOf(
        "#FAF6EE" to true,
        "#F0F4F8" to true,
        "#FDF2F4" to true,
        "#E8F5E9" to true,
        "#1F2922" to false,
        "#182030" to false,
        "#2B1824" to false,
        "#1A1A1A" to false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "THEME & BACKGROUND",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    color = Color(0xFF2D6A4F),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "背景风格设置",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 亮色风格
                ThemeOptionCard(
                    title = "亮色风格",
                    subtitle = "经典米白与纸感明亮色调，清爽自然",
                    icon = Icons.Default.LightMode,
                    accentColor = Color(0xFFC4623D),
                    isSelected = selectedOption == "LIGHT",
                    onClick = {
                        selectedOption = "LIGHT"
                        onSelectLight()
                    }
                )

                // 2. 暗色风格
                ThemeOptionCard(
                    title = "暗色风格",
                    subtitle = "极简玄武墨绿，夜间沉浸护眼，对比温和",
                    icon = Icons.Default.DarkMode,
                    accentColor = Color(0xFF52B788),
                    isSelected = selectedOption == "DARK",
                    onClick = {
                        selectedOption = "DARK"
                        onSelectDark()
                    }
                )

                // 3. 自定义背景
                ThemeOptionCard(
                    title = "自定义背景",
                    subtitle = "自定义 HEX 颜色与预设色盘，自由搭配",
                    icon = Icons.Default.Palette,
                    accentColor = Color(0xFF3B82F6),
                    isSelected = selectedOption == "CUSTOM",
                    onClick = {
                        selectedOption = "CUSTOM"
                    }
                )

                if (selectedOption == "CUSTOM") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF8A8270).copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "选择色盘或输入 HEX 颜色代码：",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // 预设色板
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presetCustomColors.forEach { (hex, isLt) ->
                                val parsedColor = try {
                                    Color(android.graphics.Color.parseColor(hex))
                                } catch (e: Exception) {
                                    Color.Gray
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(parsedColor)
                                        .border(
                                            width = if (customHexInput.equals(hex, ignoreCase = true)) 2.dp else 0.5.dp,
                                            color = if (customHexInput.equals(hex, ignoreCase = true)) Color(0xFF2D6A4F) else Color.Gray.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            customHexInput = hex
                                            customIsLight = isLt
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (customHexInput.equals(hex, ignoreCase = true)) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (isLt) Color.Black else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = customHexInput,
                            onValueChange = { input ->
                                customHexInput = input
                            },
                            label = { Text("HEX 颜色 (例如 #FAF6EE)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (customIsLight) "文字模式：深色文字 (浅底)" else "文字模式：浅色文字 (深底)",
                                fontSize = 11.5.sp
                            )
                            Switch(
                                checked = customIsLight,
                                onCheckedChange = { customIsLight = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color(0xFF2D6A4F),
                                    checkedTrackColor = Color(0xFF2D6A4F).copy(alpha = 0.3f)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                val validHex = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                                onSelectCustomColor(validHex, customIsLight)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
                        ) {
                            Text("应用自定义背景", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("关闭", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun ThemeOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isSelected) accentColor else Color.Gray.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accentColor else Color.Gray.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) Color.White else accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选",
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileInfoRow(
    textMain: Color,
    textMuted: Color,
    forestGreen: Color,
    dividerColor: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(forestGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "记",
                    fontFamily = FontFamily.Serif,
                    fontSize = 22.sp,
                    color = Color(0xFFF4F1E8)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "我的随身纯净账本",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "本地安全存储 · 极简纸感",
                    fontSize = 11.sp,
                    color = textMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
    }
}

@Composable
fun ProfileStatsRow(
    totalRecords: Int,
    totalAccounts: Int,
    continuousDays: Int,
    textMain: Color,
    textMuted: Color,
    dividerColor: Color
) {
    val formatter = DecimalFormat("#,###")
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            value = formatter.format(totalRecords),
            label = "记账总笔数",
            textMain = textMain,
            textMuted = textMuted,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(
            thickness = 0.5.dp,
            color = dividerColor,
            modifier = Modifier.height(30.dp)
        )
        StatItem(
            value = formatter.format(totalAccounts),
            label = "有效管理账户",
            textMain = textMain,
            textMuted = textMuted,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(
            thickness = 0.5.dp,
            color = dividerColor,
            modifier = Modifier.height(30.dp)
        )
        StatItem(
            value = formatter.format(continuousDays),
            label = "连续记账天数",
            textMain = textMain,
            textMuted = textMuted,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    textMain: Color,
    textMuted: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textMain
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.5.sp,
            color = textMuted
        )
    }
}

@Composable
fun ProfilePreferenceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    textMain: Color,
    textMuted: Color,
    dividerColor: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = textMain
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = textMuted
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "进入",
                tint = textMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
    }
}

