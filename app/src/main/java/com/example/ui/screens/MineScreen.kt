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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.ExpenseEntity
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlowAmber
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.components.GlowPink
import com.example.ui.components.GlowViolet
import com.example.ui.theme.BackgroundConfig
import com.example.ui.theme.BackgroundOptionType
import com.example.ui.theme.ColorSchemeOption
import com.example.ui.theme.FontScaleOption
import com.example.ui.theme.LocalAppBackgroundConfig
import com.example.ui.theme.LocalAppColorScheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onSetCustomColor: (hex: String, name: String) -> Unit,
    onSetCustomImage: (uri: String, isLight: Boolean) -> Unit,
    onSetCardAlpha: (Float) -> Unit,
    onSetBlurRadius: (Float) -> Unit,
    onSetFrostAlpha: (Float) -> Unit,
    onSetIsLight: (Boolean) -> Unit,
    onGenerateCsv: () -> String,
    onImportCsv: (String) -> Pair<Int, String>,
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    // Phase 2 子页入口（MainScreen 注入；默认空实现以减小调用方改动面）
    onOpenBooks: () -> Unit = {},
    onOpenCategories: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current

    var showDetailsDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBgDialog by remember { mutableStateOf(false) }
    var showFontDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }

    GlassBackgroundWithGlow(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Title
            item {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(10.dp))
                Column {
                    Text(
                        text = "个人中心与设置",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = bgConfig.textPrimary
                    )
                    Text(
                        text = "账本流水明细、数据导出与个性化背景/配色",
                        style = MaterialTheme.typography.bodySmall,
                        color = bgConfig.textSecondary
                    )
                }
            }

            // Profile Summary Glass Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Avatar Bubble
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF6366F1), Color(0xFF06B6D4))
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "记",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "我的随身纯净账本",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "本地安全存储 · 极简灰白质感",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }
                }
            }

            // Ledger Quick Statistics (3 Equal Size Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Total Records
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(88.dp),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { showDetailsDialog = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "${expenses.size}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bgConfig.isLight) Color(0xFF0284C7) else GlowCyan
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "记账总笔数",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // 2. Total Accounts
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(88.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "$accountsCount",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "有效管理账户",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1
                            )
                        }
                    }

                    // 3. Custom Background (Unified Size & Alignment)
                    GlassCard(
                        modifier = Modifier
                            .weight(1f)
                            .height(88.dp),
                        shape = RoundedCornerShape(18.dp),
                        onClick = { showBgDialog = true }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(currentBackgroundConfig.solidColor)
                                        .border(1.dp, Color.Gray.copy(alpha = 0.5f), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = currentBackgroundConfig.title.substringBefore(" "),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowViolet,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "自定义背景",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Menu Section Header
            item {
                Text(
                    text = "功能与个性化偏好",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            // 1. 账单明细流水 (明细)
            item {
                MineMenuItemCard(
                    icon = Icons.Default.ReceiptLong,
                    iconColor = GlowCyan,
                    title = "账单明细流水",
                    subtitle = "按分类、账户快速搜索和管理全部账目",
                    onClick = { showDetailsDialog = true },
                    modifier = Modifier.testTag("mine_item_details")
                )
            }

            // 2. 导出账单 (到处账单)
            item {
                MineMenuItemCard(
                    icon = Icons.Default.FileDownload,
                    iconColor = GlowEmerald,
                    title = "导出账单数据",
                    subtitle = "生成 CSV / 文本格式账目流水并分享或复制",
                    onClick = { showExportDialog = true },
                    modifier = Modifier.testTag("mine_item_export")
                )
            }

            // 3. 导入账单明细
            item {
                MineMenuItemCard(
                    icon = Icons.Default.FileUpload,
                    iconColor = Color(0xFF3B82F6),
                    title = "明细导入",
                    subtitle = "支持粘贴文本或选取 CSV 文件导入历史账目流水",
                    onClick = { showImportDialog = true },
                    modifier = Modifier.testTag("mine_item_import")
                )
            }

            // 4. 自定义背景 (灰白纯色默认 + 自定义纯色拾色器 + 氛围风格)
            item {
                MineMenuItemCard(
                    icon = Icons.Default.Wallpaper,
                    iconColor = Color(0xFF6366F1),
                    title = "自定义背景风格",
                    subtitle = "当前：${currentBackgroundConfig.title} · 支持灰白纯色/色板/拾色器",
                    onClick = { showBgDialog = true },
                    modifier = Modifier.testTag("mine_item_background")
                )
            }

            // 5. 字体设置
            item {
                MineMenuItemCard(
                    icon = Icons.Default.FormatSize,
                    iconColor = GlowViolet,
                    title = "字体大小设置",
                    subtitle = "当前：${currentFontScale.title} (${currentFontScale.scale}x)",
                    onClick = { showFontDialog = true },
                    modifier = Modifier.testTag("mine_item_font")
                )
            }

            // 6. 收支配色
            item {
                MineMenuItemCard(
                    icon = Icons.Default.ColorLens,
                    iconColor = GlowPink,
                    title = "收支配色方案",
                    subtitle = "当前：${currentColorScheme.title} (${currentColorScheme.description})",
                    onClick = { showColorDialog = true },
                    modifier = Modifier.testTag("mine_item_color_scheme")
                )
            }

            // 7. 账本管理（Phase 2 多账本入口）
            item {
                MineMenuItemCard(
                    icon = Icons.Default.MenuBook,
                    iconColor = GlowEmerald,
                    title = "账本管理",
                    subtitle = "多场景账本切换 · 新建 / 设为默认 / 归档",
                    onClick = onOpenBooks,
                    modifier = Modifier.testTag("mine_item_books")
                )
            }

            // 8. 分类管理（Phase 2 分类树维护入口）
            item {
                MineMenuItemCard(
                    icon = Icons.Default.Category,
                    iconColor = Color(0xFF3B82F6),
                    title = "分类管理",
                    subtitle = "支出/收入两级分类树 · 新增与归档",
                    onClick = onOpenCategories,
                    modifier = Modifier.testTag("mine_item_categories")
                )
            }

            item {
                Spacer(modifier = Modifier.height(90.dp)) // padding for bottom nav
            }
        }
    }

    // Dialog 1: 账单流水明细
    if (showDetailsDialog) {
        AllTransactionsDetailDialog(
            expenses = expenses,
            onDismiss = { showDetailsDialog = false },
            onUpdateExpense = onUpdateExpense,
            onDeleteExpense = onDeleteExpense
        )
    }

    // Dialog 2: 导出账单
    if (showExportDialog) {
        ExportBillDialog(
            csvContent = onGenerateCsv(),
            totalCount = expenses.size,
            onDismiss = { showExportDialog = false },
            onCopyToClipboard = { content ->
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("账单导出数据", content)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已成功复制账单 CSV 数据到剪贴板！", Toast.LENGTH_SHORT).show()
            },
            onShare = { content ->
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "分享导出账单数据")
                context.startActivity(shareIntent)
            }
        )
    }

    // Dialog 2.5: 导入账单明细
    if (showImportDialog) {
        ImportBillDialog(
            onDismiss = { showImportDialog = false },
            onImport = onImportCsv
        )
    }

    // Dialog 3: 自定义背景设置
    if (showBgDialog) {
        BackgroundSettingsDialog(
            currentConfig = currentBackgroundConfig,
            onDismiss = { showBgDialog = false },
            onSelectPreset = { config ->
                onSelectBackgroundConfig(config)
                Toast.makeText(context, "已切换背景：${config.title}", Toast.LENGTH_SHORT).show()
            },
            onApplyCustomHex = { hex, name ->
                onSetCustomColor(hex, name)
                Toast.makeText(context, "已应用自定义颜色：$hex", Toast.LENGTH_SHORT).show()
            },
            onSetCustomImage = { uri, isLight ->
                onSetCustomImage(uri, isLight)
                Toast.makeText(context, "已设置自定义背景图片并启用毛玻璃效果", Toast.LENGTH_SHORT).show()
            },
            onSetCardAlpha = onSetCardAlpha,
            onSetBlurRadius = onSetBlurRadius,
            onSetFrostAlpha = onSetFrostAlpha,
            onSetIsLight = onSetIsLight
        )
    }

    // Dialog 4: 字体设置
    if (showFontDialog) {
        FontScaleSettingsDialog(
            currentScale = currentFontScale,
            onDismiss = { showFontDialog = false },
            onSelect = {
                onSelectFontScale(it)
                showFontDialog = false
                Toast.makeText(context, "已切换字体：${it.title}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog 5: 收支配色设置
    if (showColorDialog) {
        ColorSchemeSettingsDialog(
            currentScheme = currentColorScheme,
            onDismiss = { showColorDialog = false },
            onSelect = {
                onSelectColorScheme(it)
                showColorDialog = false
                Toast.makeText(context, "已应用配色：${it.title}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun MineMenuItemCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current

    GlassCard(
        shape = RoundedCornerShape(20.dp),
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = if (bgConfig.isLight) 0.12f else 0.18f))
                    .border(1.dp, iconColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = bgConfig.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = null,
                tint = bgConfig.textTertiary,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * 自定义背景风格、毛玻璃特效调节与自定义图片壁纸弹窗 (支持任意位置向下滑动关闭)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundSettingsDialog(
    currentConfig: BackgroundConfig,
    onDismiss: () -> Unit,
    onSelectPreset: (BackgroundConfig) -> Unit,
    onApplyCustomHex: (hex: String, name: String) -> Unit,
    onSetCustomImage: (uri: String, isLight: Boolean) -> Unit,
    onSetCardAlpha: (Float) -> Unit,
    onSetBlurRadius: (Float) -> Unit,
    onSetFrostAlpha: (Float) -> Unit,
    onSetIsLight: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current
    val colorScheme = LocalAppColorScheme.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customHexInput by remember { mutableStateOf(currentConfig.customHex) }
    var inputError by remember { mutableStateOf(false) }

    // System photo / image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onSetCustomImage(uri.toString(), currentConfig.isLight)
        }
    }

    // Solid Presets - Default is GRAY_WHITE
    val lightPresets = remember {
        listOf(
            BackgroundConfig(
                type = BackgroundOptionType.GRAY_WHITE,
                title = "灰白纯色 (默认)",
                subtitle = "极简高雅灰白，清晰纯净",
                solidColor = Color(0xFFF3F4F6),
                isLight = true,
                customHex = "#F3F4F6",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.WARM_IVORY,
                title = "暖阳米白",
                subtitle = "温润柔和象牙白，舒适护眼",
                solidColor = Color(0xFFF7F5F0),
                isLight = true,
                customHex = "#F7F5F0",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.PURE_WHITE,
                title = "极简纯白",
                subtitle = "现代明亮纯白，极致简洁",
                solidColor = Color(0xFFFAFAFA),
                isLight = true,
                customHex = "#FAFAFA",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.MINT_LIGHT,
                title = "淡雅薄荷",
                subtitle = "清新浅绿薄荷，舒缓宁静",
                solidColor = Color(0xFFF0FDF4),
                isLight = true,
                customHex = "#F0FDF4",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.LILAC_LIGHT,
                title = "暮光雾紫",
                subtitle = "素雅淡雾紫，高雅唯美",
                solidColor = Color(0xFFF5F3FF),
                isLight = true,
                customHex = "#F5F3FF",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.SKY_LIGHT,
                title = "晴空浅蓝",
                subtitle = "清透浅冰蓝，宁静开阔",
                solidColor = Color(0xFFF0F9FF),
                isLight = true,
                customHex = "#F0F9FF",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            )
        )
    }

    val darkPresets = remember {
        listOf(
            BackgroundConfig(
                type = BackgroundOptionType.DEEP_COSMIC,
                title = "深空微光 (暗色发光)",
                subtitle = "深邃宇宙星空，动态发光氛围",
                solidColor = Color(0xFF090D16),
                isLight = false,
                customHex = "#090D16",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.AURORA_NIGHT,
                title = "极光幻彩 (暗色渐变)",
                subtitle = "极光夜幕深蓝，流光溢彩",
                solidColor = Color(0xFF060B18),
                isLight = false,
                customHex = "#060B18",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            ),
            BackgroundConfig(
                type = BackgroundOptionType.SLATE_DARK,
                title = "玄武深岩 (暗色纯黑)",
                subtitle = "纯净板岩深黑，深邃沉静",
                solidColor = Color(0xFF0F172A),
                isLight = false,
                customHex = "#0F172A",
                cardAlpha = currentConfig.cardAlpha,
                blurRadius = currentConfig.blurRadius,
                frostAlpha = currentConfig.frostAlpha
            )
        )
    }

    // Quick Color Palette for solid customization
    val quickSwatches = remember {
        listOf(
            "#F3F4F6" to "经典灰白",
            "#F8FAFC" to "极淡冷灰",
            "#F1F5F9" to "岩石灰白",
            "#F7F5F0" to "暖象牙白",
            "#FFFBEB" to "暖杏奶茶",
            "#FEF2F2" to "淡粉樱花",
            "#F0FDF4" to "清香薄荷",
            "#ECFEFF" to "冰晶浅青",
            "#EFF6FF" to "天空浅蓝",
            "#F5F3FF" to "淡雅薰衣",
            "#FAF5FF" to "云雾淡紫",
            "#FFF7ED" to "朝阳暖橘",
            "#E2E8F0" to "素雅中灰",
            "#CBD5E1" to "水泥冷灰",
            "#334155" to "板岩深蓝",
            "#1E293B" to "暗夜深空",
            "#0F172A" to "玄武深黑",
            "#000000" to "极黑纯色"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgConfig.dialogBackground,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = bgConfig.textSecondary.copy(alpha = 0.5f))
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Title & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowViolet,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "背景与毛玻璃设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "关闭",
                        tint = bgConfig.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // SECTION 1: 实时毛玻璃与卡片效果预览
            Text(
                text = "实时毛玻璃与卡片预览",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
            )
            Spacer(modifier = Modifier.height(6.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = Color(0xFF6366F1),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "星巴克咖啡 (示例)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = bgConfig.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (bgConfig.isLight) Color(0xFFE0E7FF) else Color(0xFF312E81))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "微信钱包",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (bgConfig.isLight) Color(0xFF4338CA) else Color(0xFFA5B4FC),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "透明度 ${(currentConfig.cardAlpha * 100).toInt()}% · 模糊 ${currentConfig.blurRadius.toInt()}dp · 遮罩 ${(currentConfig.frostAlpha * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "-¥38.00",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.expenseColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 2: 毛玻璃与卡片透明度/模糊度自定义微调
            Text(
                text = "毛玻璃与卡片参数调节",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowViolet
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Slider 1: 卡片透明度 (Card Alpha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Opacity,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "卡片透明度 (不透明度)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bgConfig.textPrimary
                    )
                }
                Text(
                    text = "${(currentConfig.cardAlpha * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6366F1)
                )
            }
            Slider(
                value = currentConfig.cardAlpha,
                onValueChange = onSetCardAlpha,
                valueRange = 0.15f..0.98f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF6366F1),
                    activeTrackColor = Color(0xFF6366F1),
                    inactiveTrackColor = bgConfig.textSecondary.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Slider 2: 毛玻璃模糊度 (Blur Radius)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BlurOn,
                        contentDescription = null,
                        tint = Color(0xFF06B6D4),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "毛玻璃模糊度",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bgConfig.textPrimary
                    )
                }
                Text(
                    text = "${currentConfig.blurRadius.toInt()} dp",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06B6D4)
                )
            }
            Slider(
                value = currentConfig.blurRadius,
                onValueChange = onSetBlurRadius,
                valueRange = 0f..25f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF06B6D4),
                    activeTrackColor = Color(0xFF06B6D4),
                    inactiveTrackColor = bgConfig.textSecondary.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Slider 3: 背景遮罩浓度 (Frost Alpha)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "背景遮罩浓度 (增强对比度)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = bgConfig.textPrimary
                    )
                }
                Text(
                    text = "${(currentConfig.frostAlpha * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
            }
            Slider(
                value = currentConfig.frostAlpha,
                onValueChange = onSetFrostAlpha,
                valueRange = 0.0f..0.70f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF10B981),
                    activeTrackColor = Color(0xFF10B981),
                    inactiveTrackColor = bgConfig.textSecondary.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Toggle: 浅色/深色文字模式适配
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (bgConfig.isLight) Color.Black.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.BrightnessMedium,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFFF59E0B) else GlowAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "浅色卡片 / 深色字模式",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary
                        )
                        Text(
                            text = if (currentConfig.isLight) "开启: 适合浅色壁纸" else "关闭: 适合深色壁纸",
                            style = MaterialTheme.typography.labelSmall,
                            color = bgConfig.textSecondary
                        )
                    }
                }
                Switch(
                    checked = currentConfig.isLight,
                    onCheckedChange = onSetIsLight,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF6366F1)
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 3: 自定义图片壁纸与相册上传
            Text(
                text = "自定义相册壁纸",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowViolet
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Upload Picture Button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1)
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "从手机相册选择自定义图片",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // If currently using custom image, show reset button
            if (currentConfig.type == BackgroundOptionType.CUSTOM_IMAGE) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSelectPreset(lightPresets.first())
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.RestartAlt,
                        contentDescription = null,
                        tint = bgConfig.textPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "清除图片壁纸并恢复默认灰白纯色",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 4: 纯色极简风格 (灰白纯色默认)
            Text(
                text = "纯色极简风格 (推荐)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
            )
            Spacer(modifier = Modifier.height(6.dp))

            lightPresets.forEach { preset ->
                val isSelected = currentConfig.type == preset.type && currentConfig.customHex.equals(preset.customHex, ignoreCase = true)
                BackgroundOptionItemCard(
                    config = preset,
                    isSelected = isSelected,
                    onClick = { onSelectPreset(preset) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // SECTION 5: 暗色氛围风格
            Text(
                text = "暗色氛围风格",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF6366F1) else GlowViolet
            )
            Spacer(modifier = Modifier.height(6.dp))

            darkPresets.forEach { preset ->
                val isSelected = currentConfig.type == preset.type
                BackgroundOptionItemCard(
                    config = preset,
                    isSelected = isSelected,
                    onClick = { onSelectPreset(preset) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SECTION 6: 自定义纯色拾色板 & HEX
            Text(
                text = "自定义纯色拾色板",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Quick Palette Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val rows = quickSwatches.chunked(6)
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { (hex, name) ->
                            val parsedColor = remember(hex) {
                                try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color.Gray }
                            }
                            val isSelected = currentConfig.type == BackgroundOptionType.CUSTOM_SOLID && currentConfig.customHex.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(parsedColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF6366F1) else Color.Gray.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        customHexInput = hex
                                        onApplyCustomHex(hex, name)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (parsedColor.luminance() > 0.5f) Color.Black else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Custom HEX Input & Apply
            Text(
                text = "自定义 HEX 颜色代码",
                style = MaterialTheme.typography.bodySmall,
                color = bgConfig.textSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customHexInput,
                    onValueChange = {
                        customHexInput = it
                        inputError = false
                    },
                    placeholder = { Text("例: #F3F4F6", color = bgConfig.textTertiary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        val previewColor = remember(customHexInput) {
                            try {
                                val clean = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                                Color(android.graphics.Color.parseColor(clean))
                            } catch (e: Exception) {
                                Color.Transparent
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(previewColor)
                                .border(1.dp, Color.Gray.copy(alpha = 0.4f), CircleShape)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = if (inputError) Color.Red else Color(0xFF6366F1),
                        unfocusedBorderColor = if (inputError) Color.Red else bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        try {
                            val clean = if (customHexInput.startsWith("#")) customHexInput else "#$customHexInput"
                            android.graphics.Color.parseColor(clean)
                            onApplyCustomHex(clean, "自定义纯色 ($clean)")
                        } catch (e: Exception) {
                            inputError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(52.dp)
                ) {
                    Text("应用", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            if (inputError) {
                Text(
                    text = "请输入有效的 6 位或 8 位 HEX 颜色值 (如 #F3F4F6)",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * 导入账单明细弹窗
 */
@Composable
fun ImportBillDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Pair<Int, String>
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current

    var inputText by remember { mutableStateOf("") }
    var importStatus by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // File picker launcher for CSV/TXT
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    inputText = content
                    Toast.makeText(context, "已成功读取文件内容", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "读取文件失败：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val sampleTemplate = """
# === 资产账户记录 (ACCOUNTS) ===
v3_account,uuid,账户名称,类型,初始余额(元),颜色,备注
v3_account,,微信钱包,储蓄卡,3280.50,#3B82F6,日常零钱
v3_account,,招商银行,储蓄卡,24600.00,#8B5CF6,主卡

# === 收支明细记录 (TRANSACTIONS) ===
v2,uuid,日期时间,类型,一级分类,二级分类,账户,对方账户,金额(元),备注,状态
v2,,2026-08-25 12:30:00,支出,餐饮,午餐,微信钱包,,35.00,快餐午饭,有效
v2,,2026-08-25 09:00:00,收入,工资薪水,工资薪水,招商银行,,15000.00,8月份薪资,有效
v2,,2026-08-25 15:00:00,转账,转账,转账,招商银行,微信钱包,2000.00,日常周转,有效
    """.trimIndent()

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "导入账单明细",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "支持粘贴 CSV / 文本格式数据，或直接从手机选取 CSV 文件：",
                    style = MaterialTheme.typography.bodySmall,
                    color = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Actions: Pick File & Paste Sample
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("选择 CSV 文件", style = MaterialTheme.typography.labelMedium, color = Color.White)
                    }

                    Button(
                        onClick = {
                            inputText = sampleTemplate
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "填入示例模板",
                            style = MaterialTheme.typography.labelMedium,
                            color = bgConfig.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text Input Area
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        importStatus = null
                    },
                    label = { Text("CSV / 文本明细数据", color = bgConfig.textSecondary) },
                    placeholder = { Text("可在此直接粘贴由 Excel / 导出的 CSV 数据...", color = bgConfig.textTertiary) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = Color(0xFF3B82F6),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    )
                )

                if (importStatus != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = importStatus!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isError) Color(0xFFEF4444) else GlowEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Import Button
                Button(
                    onClick = {
                        if (inputText.isBlank()) {
                            importStatus = "⚠️ 请输入或选择 CSV 数据"
                            isError = true
                            return@Button
                        }
                        val (count, msg) = onImport(inputText)
                        if (count > 0) {
                            Toast.makeText(context, "成功导入 $count 笔账单流水！", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            importStatus = "⚠️ $msg"
                            isError = true
                        }
                    },
                    enabled = inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlowEmerald,
                        disabledContainerColor = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "确认导入到账本",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}

@Composable
private fun BackgroundOptionItemCard(
    config: BackgroundConfig,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    GlassCard(
        shape = RoundedCornerShape(14.dp),
        backgroundColor = if (isSelected) {
            if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.25f)
        } else {
            if (bgConfig.isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B).copy(alpha = 0.45f)
        },
        borderColor = Brush.linearGradient(
            if (isSelected) listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
            else listOf(bgConfig.dividerColor, Color.Transparent)
        ),
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color sample pill
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(config.solidColor)
                    .border(1.dp, Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = config.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF6366F1) else bgConfig.textPrimary
                )
                Text(
                    text = config.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = bgConfig.textSecondary
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "已选择",
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 账单明细流水弹窗
 */
@Composable
fun AllTransactionsDetailDialog(
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit,
    onUpdateExpense: (ExpenseEntity, ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") }

    val filtered = remember(expenses, searchQuery, filterType) {
        expenses.filter { item ->
            val matchesType = when (filterType) {
                "EXPENSE" -> item.type == "EXPENSE"
                "INCOME" -> item.type == "INCOME"
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                item.category.contains(searchQuery, ignoreCase = true) ||
                item.note.contains(searchQuery, ignoreCase = true) ||
                item.accountName.contains(searchQuery, ignoreCase = true) ||
                item.amount.toString().contains(searchQuery)
            }
            matchesType && matchesQuery
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .padding(vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "账单流水明细 (${filtered.size}笔)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("搜索分类、账户、备注...", color = bgConfig.textTertiary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = bgConfig.textSecondary)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = GlowCyan,
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Type Filter Chips
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlassChip(
                        selected = filterType == "ALL",
                        onClick = { filterType = "ALL" },
                        selectedGlowColor = GlowCyan
                    ) {
                        Text(
                            text = "全部",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filterType == "ALL") GlowCyan else bgConfig.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    GlassChip(
                        selected = filterType == "EXPENSE",
                        onClick = { filterType = "EXPENSE" },
                        selectedGlowColor = GlowPink
                    ) {
                        Text(
                            text = "支出",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filterType == "EXPENSE") GlowPink else bgConfig.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                    GlassChip(
                        selected = filterType == "INCOME",
                        onClick = { filterType = "INCOME" },
                        selectedGlowColor = GlowEmerald
                    ) {
                        Text(
                            text = "收入",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (filterType == "INCOME") GlowEmerald else bgConfig.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Detailed items list
                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("无匹配的明细记录", color = bgConfig.textSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { item ->
                            val isExpense = item.type == "EXPENSE"
                            val dateStr = remember(item.dateTimestamp) {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                                sdf.format(Date(item.dateTimestamp))
                            }
                            GlassCard(
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = item.category,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = bgConfig.textPrimary
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f)
                                            ) {
                                                Text(
                                                    text = item.accountName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (bgConfig.isLight) Color(0xFF0284C7) else Color(0xFF38BDF8),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        if (item.note.isNotBlank()) {
                                            Text(
                                                text = item.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = bgConfig.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = bgConfig.textTertiary,
                                            fontSize = 10.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (isExpense) "-" else "+"} ¥${String.format(Locale.CHINA, "%.2f", item.amount)}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isExpense) GlowPink else GlowEmerald
                                        )
                                        IconButton(
                                            onClick = { onDeleteExpense(item) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除",
                                                tint = bgConfig.textTertiary,
                                                modifier = Modifier.size(14.dp)
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

/**
 * 导出账单数据弹窗
 */
@Composable
fun ExportBillDialog(
    csvContent: String,
    totalCount: Int,
    onDismiss: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onShare: (String) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "导出账单数据 (CSV)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "已就绪 $totalCount 笔账目流水，格式标准兼容 Excel / 飞书 / WPS 电子表格。",
                    style = MaterialTheme.typography.bodySmall,
                    color = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Preview box
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(14.dp),
                    backgroundColor = if (bgConfig.isLight) Color(0xFFF1F5F9) else Color.Black.copy(alpha = 0.4f)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = csvContent.ifBlank { "暂无数据" },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            ),
                            color = bgConfig.textPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons: Share & Copy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onCopyToClipboard(csvContent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (bgConfig.isLight) Color(0xFF475569) else Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_copy_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制内容", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onShare(csvContent) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("export_share_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("系统分享", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 字体大小设置弹窗
 */
@Composable
fun FontScaleSettingsDialog(
    currentScale: FontScaleOption,
    onDismiss: () -> Unit,
    onSelect: (FontScaleOption) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "字体大小排版设置",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FontScaleOption.values().forEach { option ->
                    val isSelected = option == currentScale
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (isSelected) {
                            if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.25f)
                        } else {
                            if (bgConfig.isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B).copy(alpha = 0.4f)
                        },
                        borderColor = Brush.linearGradient(
                            if (isSelected) listOf(GlowViolet, Color(0xFF38BDF8))
                            else listOf(bgConfig.dividerColor, Color.Transparent)
                        ),
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${option.title} (${option.scale}x)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF6366F1) else bgConfig.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = option.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bgConfig.textSecondary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "已选择",
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
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
 * 收支配色方案设置弹窗
 */
@Composable
fun ColorSchemeSettingsDialog(
    currentScheme: ColorSchemeOption,
    onDismiss: () -> Unit,
    onSelect: (ColorSchemeOption) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "收支配色方案选择",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                ColorSchemeOption.values().forEach { scheme ->
                    val isSelected = scheme == currentScheme
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        backgroundColor = if (isSelected) {
                            if (bgConfig.isLight) Color(0xFF6366F1).copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.22f)
                        } else {
                            if (bgConfig.isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B).copy(alpha = 0.4f)
                        },
                        borderColor = Brush.linearGradient(
                            if (isSelected) listOf(Color(0xFF818CF8), Color(0xFF38BDF8))
                            else listOf(bgConfig.dividerColor, Color.Transparent)
                        ),
                        onClick = { onSelect(scheme) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = scheme.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF6366F1) else bgConfig.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = scheme.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = bgConfig.textSecondary
                                )
                            }

                            // Color swatches (Expense & Income demo capsules)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = scheme.expenseContainer
                                ) {
                                    Text(
                                        text = "支 -100",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.expenseText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = scheme.incomeContainer
                                ) {
                                    Text(
                                        text = "收 +100",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = scheme.incomeText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
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
