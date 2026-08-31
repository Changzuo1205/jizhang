package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entity.BookEntity
import com.example.ui.components.EditorialPageHeader
import com.example.ui.theme.LocalAppBackgroundConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 手账本专属封面配色主题
private data class BookCoverTheme(
    val name: String,
    val primaryColor: Color,
    val spineColor: Color,
    val accentColor: Color,
    val tagBg: Color
)

private val BookCoverThemes = listOf(
    BookCoverTheme("墨绿手账", Color(0xFF2D6A4F), Color(0xFF1B4332), Color(0xFF52B788), Color(0xFFE8F5E9)),
    BookCoverTheme("琥珀陶土", Color(0xFFC05621), Color(0xFF7B2CBF), Color(0xFFDD6B20), Color(0xFFFEEBC8)),
    BookCoverTheme("深海蔚蓝", Color(0xFF1E3A8A), Color(0xFF1E1B4B), Color(0xFF3B82F6), Color(0xFFE0F2FE)),
    BookCoverTheme("复古暮紫", Color(0xFF6B21A8), Color(0xFF3B0764), Color(0xFFA855F7), Color(0xFFF3E8FF)),
    BookCoverTheme("暖炭木黑", Color(0xFF262626), Color(0xFF171717), Color(0xFFA3A3A3), Color(0xFFF5F5F5))
)

/**
 * 账本管理页（全新手账风升级版）。
 *
 * 特性：
 * 1. 拟物手账本封面设计（书脊、防滑针线缝边、金属封角质感）
 * 2. 多账本数据完全隔离：切换账本时同步修改默认/激活账本，更新全应用收支流水
 * 3. 支持快捷新建、重命名、选择主题风格与归档
 */
@Composable
fun BooksScreen(
    books: List<BookEntity>,
    currentBookId: Long? = null,
    onCreateBook: (name: String) -> Unit = {},
    onRenameBook: (bookId: Long, name: String) -> Unit = { _, _ -> },
    onSetDefaultBook: (bookId: Long) -> Unit = {},
    onArchiveBook: (bookId: Long) -> Unit = {},
    onClearBookData: (bookId: Long) -> Unit = {},
    onDeleteBook: (bookId: Long) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = true) { onBack() }

    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current
    val backgroundColor = if (bgConfig.isLight) Color(0xFFFAFAF7) else Color(0xFF121418)
    val textPrimary = bgConfig.textPrimary
    val textMuted = bgConfig.textSecondary
    val dividerColor = bgConfig.dividerColor

    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingBook by remember { mutableStateOf<BookEntity?>(null) }
    var archiveConfirmBook by remember { mutableStateOf<BookEntity?>(null) }
    var clearDataConfirmBook by remember { mutableStateOf<BookEntity?>(null) }
    var deleteBookConfirmBook by remember { mutableStateOf<BookEntity?>(null) }

    val activeBook = remember(books, currentBookId) {
        if (currentBookId != null) {
            books.find { it.id == currentBookId } ?: books.find { it.isDefault } ?: books.firstOrNull()
        } else {
            books.find { it.isDefault } ?: books.firstOrNull()
        }
    }

    val sdf = remember { SimpleDateFormat("yyyy.MM.dd", Locale.CHINA) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        // 返回标头
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (bgConfig.isLight) Color(0xFFEFECE6) else Color(0xFF22262E))
                    .testTag("books_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回",
                    tint = textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "LEDGER MANAGEMENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = Color(0xFF2D6A4F),
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "账本管理",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
        ) {
            // Header Banner: 当前使用中的手账本信息
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (bgConfig.isLight) Color(0xFFF3EFE6) else Color(0xFF1E222A),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF2D6A4F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Book,
                                    contentDescription = null,
                                    tint = Color(0xFFF4F1E8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "当前激活手账本",
                                    fontSize = 11.sp,
                                    color = textMuted,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = activeBook?.name ?: "默认账本",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF2D6A4F).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "共 ${books.size} 个账本",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2D6A4F),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // 新建入口 Banner
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (bgConfig.isLight) Color(0xFF2D6A4F).copy(alpha = 0.4f) else Color(0xFF52B788).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showCreateDialog = true }
                        .testTag("create_book_card")
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
                                .background(Color(0xFF2D6A4F).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF2D6A4F),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "新建独立手账本",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "为旅行、家庭、项目建立独立算账明细",
                                fontSize = 12.sp,
                                color = textMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = "新建",
                            tint = Color(0xFF2D6A4F),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 账本列表 Heading
            item {
                Text(
                    text = "ALL HANDBOOK LEDGERS / 账本画廊",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = textMuted,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 账本卡片
            items(books, key = { it.id }) { book ->
                val isActive = activeBook?.id == book.id
                val isDefault = book.isDefault
                val theme = remember(book.id) {
                    BookCoverThemes[(book.id % BookCoverThemes.size).toInt()]
                }
                val createdDateStr = remember(book.createdAt) {
                    if (book.createdAt > 0) sdf.format(Date(book.createdAt)) else "系统内置"
                }

                JournalBookCard(
                    book = book,
                    isActive = isActive,
                    isDefault = isDefault,
                    canDelete = books.size > 1,
                    theme = theme,
                    createdDateStr = createdDateStr,
                    onSelect = {
                        if (!isActive) {
                            onSetDefaultBook(book.id)
                            Toast.makeText(context, "已切换至【${book.name}】，明细随之更新", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRename = { renamingBook = book },
                    onClearData = { clearDataConfirmBook = book },
                    onDelete = { deleteBookConfirmBook = book },
                    onArchive = { archiveConfirmBook = book }
                )
            }
        }
    }

    // 新建账本对话框
    if (showCreateDialog) {
        CreateBookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                onCreateBook(name)
                showCreateDialog = false
                Toast.makeText(context, "已创建并自动切换至【$name】", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 重命名对话框
    if (renamingBook != null) {
        RenameBookDialog(
            initialName = renamingBook!!.name,
            onDismiss = { renamingBook = null },
            onConfirm = { newName ->
                onRenameBook(renamingBook!!.id, newName)
                renamingBook = null
                Toast.makeText(context, "账本重命名成功", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 清空数据确认对话框
    if (clearDataConfirmBook != null) {
        val target = clearDataConfirmBook!!
        AlertDialog(
            onDismissRequest = { clearDataConfirmBook = null },
            title = { Text("清空账本数据？", fontWeight = FontWeight.Bold) },
            text = {
                Text("确定要清空【${target.name}】的所有数据吗？\n\n清空后该账本内的所有记账流水与转账明细将被彻底清除，账户余额将归零。此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onClearBookData(target.id)
                        clearDataConfirmBook = null
                        Toast.makeText(context, "【${target.name}】数据已清空", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认清空数据", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { clearDataConfirmBook = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 删除账本确认对话框
    if (deleteBookConfirmBook != null) {
        val target = deleteBookConfirmBook!!
        AlertDialog(
            onDismissRequest = { deleteBookConfirmBook = null },
            title = { Text("删除账本及数据？", fontWeight = FontWeight.Bold) },
            text = {
                Text("确定要删除【${target.name}】吗？\n\n删除后该账本及其包含的所有账户、明细数据将被彻底移除，此操作不可撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteBook(target.id)
                        deleteBookConfirmBook = null
                        Toast.makeText(context, "【${target.name}】已成功删除", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("确认删除账本", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteBookConfirmBook = null }) {
                    Text("取消")
                }
            }
        )
    }

    // 归档确认对话框
    if (archiveConfirmBook != null) {
        AlertDialog(
            onDismissRequest = { archiveConfirmBook = null },
            title = { Text("确认归档账本？", fontWeight = FontWeight.Bold) },
            text = { Text("归档后【${archiveConfirmBook!!.name}】将移出常用账本画廊，但历史数据安全保留。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onArchiveBook(archiveConfirmBook!!.id)
                        archiveConfirmBook = null
                        Toast.makeText(context, "账本已成功归档", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("确认归档", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { archiveConfirmBook = null }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 手账风精装书卡片组件 (Journal Cover Card)
 */
@Composable
private fun JournalBookCard(
    book: BookEntity,
    isActive: Boolean,
    isDefault: Boolean,
    canDelete: Boolean,
    theme: BookCoverTheme,
    createdDateStr: String,
    onSelect: () -> Unit,
    onRename: () -> Unit,
    onClearData: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
        shadowElevation = if (isActive) 8.dp else 2.dp,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isActive) 2.dp else 0.8.dp,
            color = if (isActive) theme.primaryColor else if (bgConfig.isLight) Color(0xFFE5E2D9) else Color.White.copy(alpha = 0.08f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // 拟物书脊 (Spine) Accent
            Box(
                modifier = Modifier
                    .width(16.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(theme.spineColor, theme.primaryColor)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 书脊线纹理
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .width(6.dp)
                                .height(2.dp)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                    }
                }
            }

            // 封面主要区域
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                // Top Tag & Title Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = theme.tagBg
                        ) {
                            Text(
                                text = "LEDGER NO.${book.id}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        if (isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text(
                                    text = "主账本",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // 编辑/操作菜单
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "重命名",
                                tint = bgConfig.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(onClick = onClearData, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "清空账本数据",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        if (canDelete) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = "删除账本",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (!isDefault) {
                            IconButton(onClick = onArchive, modifier = Modifier.size(28.dp)) {
                                Icon(
                                    imageVector = Icons.Outlined.Archive,
                                    contentDescription = "归档",
                                    tint = bgConfig.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 手账本标题
                Text(
                    text = book.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "币种: ${book.currency} · 建立于 $createdDateStr",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = bgConfig.textSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 底部切换 / 激活状态 Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isActive) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = theme.primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "正在使用中",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primaryColor
                            )
                        }
                    } else {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (bgConfig.isLight) Color(0xFFF1F5F9) else Color(0xFF2D3748),
                                contentColor = bgConfig.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "切换至此账本", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 新建手账本对话框
 */
@Composable
private fun CreateBookDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String) -> Unit
) {
    var bookName by remember { mutableStateOf("") }
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "新建手账本",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Text(
                    text = "建立后，系统将为此账本开启独立的收支流水隔离",
                    fontSize = 12.sp,
                    color = bgConfig.textSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text("手账本名称") },
                    placeholder = { Text("如：家庭开销、日本旅行账...") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D6A4F),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("book_name_input")
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = bgConfig.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (bookName.isNotBlank()) {
                                onCreate(bookName.trim())
                            }
                        },
                        enabled = bookName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("确认创建", color = Color(0xFFF4F1E8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * 重命名对话框
 */
@Composable
private fun RenameBookDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (newName: String) -> Unit
) {
    var bookName by remember { mutableStateOf(initialName) }
    val bgConfig = LocalAppBackgroundConfig.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
            shadowElevation = 16.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "修改账本名称",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bookName,
                    onValueChange = { bookName = it },
                    label = { Text("账本名称") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D6A4F),
                        unfocusedBorderColor = bgConfig.inputFieldBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("取消", color = bgConfig.textSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (bookName.isNotBlank()) {
                                onConfirm(bookName.trim())
                            }
                        },
                        enabled = bookName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("保存修改", color = Color(0xFFF4F1E8), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
