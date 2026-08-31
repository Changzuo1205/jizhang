package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.data.local.ExpenseEntity
import com.example.ui.components.EditorialPageHeader
import com.example.ui.theme.BackgroundConfig
import com.example.ui.theme.BackgroundOptionType
import com.example.ui.theme.ColorSchemeOption
import com.example.ui.theme.FontScaleOption
import com.example.ui.theme.LocalAppBackgroundConfig
import java.text.DecimalFormat
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

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
    onGenerateCsv: () -> Unit,
    onImportCsv: (String) -> Unit,
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onOpenBooks: () -> Unit,
    onOpenCategories: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val backgroundColor = if (bgConfig.isLight) Color(0xFFF4F1E8) else bgConfig.solidColor
    val textMain = bgConfig.textPrimary
    val textMuted = if (bgConfig.isLight) Color(0xFF8A8270) else Color(0xFFB0B0B0)
    val forestGreen = Color(0xFF2D6A4F)
    val dividerColor = bgConfig.dividerColor

    val totalRecords = expenses.size
    val continuousDays = 3 // Mocked for now

    var showImportDialog by remember { mutableStateOf(false) }
    var csvContent by remember { mutableStateOf("") }
    
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
                    icon = Icons.Outlined.ReceiptLong,
                    title = "账单明细流水",
                    subtitle = "按时间线查看所有收支记录",
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
                    onClick = onGenerateCsv
                )
                ProfilePreferenceRow(
                    icon = Icons.Outlined.Upload,
                    title = "明细导入",
                    subtitle = "从其他平台导入账单",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = { showImportDialog = true }
                )
                ProfilePreferenceRow(
                    icon = Icons.Outlined.ColorLens,
                    title = "自定义背景风格",
                    subtitle = "切换主题模式与视觉配色",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = { showThemeDialog = true }
                )
            }
        }
    }
    
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("导入 CSV 数据") },
            text = {
                OutlinedTextField(
                    value = csvContent,
                    onValueChange = { csvContent = it },
                    label = { Text("粘贴 CSV 内容") },
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    onImportCsv(csvContent)
                    showImportDialog = false
                    csvContent = ""
                }) {
                    Text("确认导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
    
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("切换背景风格") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    BackgroundOptionType.values().forEach { option ->
                        Text(
                            text = option.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onSelectBackgroundConfig(BackgroundConfig(type = option))
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text("关闭")
                }
            }
        )
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
