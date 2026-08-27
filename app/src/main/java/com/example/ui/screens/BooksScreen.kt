package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.BookEntity
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.components.GlowCyan
import com.example.ui.components.GlowEmerald
import com.example.ui.theme.LocalAppBackgroundConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 账本管理页（Phase 2 子页）。
 *
 * 复用 [BudgetSettingsScreen] 的页面骨架：返回头 + 玻璃容器 + 统一边距。
 * 支持新建（弹窗输入名称）、设为默认、重命名与归档；默认账本由 Repository 层
 * 保护不可归档，UI 端仅隐藏入口。
 */
@Composable
fun BooksScreen(
    books: List<BookEntity>,
    onCreateBook: (name: String) -> Unit = {},
    onRenameBook: (bookId: Long, name: String) -> Unit = { _, _ -> },
    onSetDefaultBook: (bookId: Long) -> Unit = {},
    onArchiveBook: (bookId: Long) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 拦截系统返回手势回到上级
    BackHandler(enabled = true) { onBack() }

    val bgConfig = LocalAppBackgroundConfig.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var renamingBook by remember { mutableStateOf<BookEntity?>(null) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.CHINA) }

    GlassBackgroundWithGlow(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.statusBarsPadding())
                Spacer(modifier = Modifier.height(8.dp))

                // 返回头（与预算设置页同构）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (bgConfig.isLight) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.1f))
                                .testTag("books_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = bgConfig.textPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "账本管理",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = bgConfig.textPrimary
                            )
                            Text(
                                text = "多场景账本 · 设默认 / 归档互不影响",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (bgConfig.isLight) Color(0xFF059669).copy(alpha = 0.12f) else Color(0xFF059669).copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 新建账本入口卡片
                GlassCard(
                    shape = RoundedCornerShape(20.dp),
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth().testTag("create_book_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF6366F1).copy(alpha = if (bgConfig.isLight) 0.12f else 0.25f))
                                .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "新建账本",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                            Text(
                                text = "如：日常账本、家庭账本、旅行账本…",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary,
                                maxLines = 1
                            )
                        }
                        Text(
                            text = "＋ 新建",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "现有账本 (${books.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                books.forEach { book ->
                    BookRowCard(
                        book = book,
                        createdDateText = sdf.format(Date(book.createdAt)),
                        onSetDefault = { onSetDefaultBook(book.id) },
                        onRename = { renamingBook = book },
                        onArchive = { onArchiveBook(book.id) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (books.isEmpty()) {
                    GlassCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "暂无账本，点击上方「新建账本」开始创建",
                            style = MaterialTheme.typography.bodySmall,
                            color = bgConfig.textSecondary,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(90.dp)) // 底部导航占位
            }
        }
    }

    // 新建账本对话框
    if (showCreateDialog) {
        BookNameEditDialog(
            title = "新建账本",
            initialName = "",
            confirmText = "创建",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateBook(name)
                showCreateDialog = false
            }
        )
    }

    // 重命名对话框
    if (renamingBook != null) {
        BookNameEditDialog(
            title = "重命名账本",
            initialName = renamingBook!!.name,
            confirmText = "保存",
            onDismiss = { renamingBook = null },
            onConfirm = { name ->
                onRenameBook(renamingBook!!.id, name)
                renamingBook = null
            }
        )
    }
}

/** 单个账本条目玻璃卡：名称 + 默认徽标 + 设默认/重命名/归档操作 */
@Composable
private fun BookRowCard(
    book: BookEntity,
    createdDateText: String,
    onSetDefault: () -> Unit,
    onRename: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current

    GlassCard(
        shape = RoundedCornerShape(20.dp),
        backgroundColor = if (book.isDefault) {
            if (bgConfig.isLight) Color(0xFFEEF2FF).copy(alpha = 0.95f) else Color(0xFF312E81).copy(alpha = 0.45f)
        } else null,
        borderColor = if (book.isDefault) Brush.linearGradient(
            listOf(
                Color(0xFF6366F1).copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.12f)
            )
        ) else null,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF6366F1).copy(alpha = if (bgConfig.isLight) 0.12f else 0.25f))
                        .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.35f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = book.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary,
                            maxLines = 1
                        )
                        if (book.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF6366F1).copy(alpha = if (bgConfig.isLight) 0.15f else 0.35f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "默认",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${book.currency} · 建于 $createdDateText",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "重命名",
                        tint = bgConfig.textTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 操作行：设为默认 / 归档（默认账本禁止归档）
            if (!book.isDefault) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(selected = true, onClick = onSetDefault, selectedGlowColor = Color(0xFF6366F1)) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "设为默认",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (bgConfig.isLight) Color(0xFF4F46E5) else GlowCyan
                            )
                        }
                    }
                    GlassChip(selected = false, onClick = onArchive) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = null,
                                tint = bgConfig.textTertiary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "归档",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (bgConfig.isLight) Color(0xFF059669) else GlowEmerald,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "当前默认账本 · 记账与导入均挂靠于此",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textSecondary
                    )
                }
            }
        }
    }
}

/** 账本名称输入对话框（新建/重命名共用） */
@Composable
private fun BookNameEditDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var nameInput by remember { mutableStateOf(initialName) }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("账本名称", color = bgConfig.textSecondary) },
                    placeholder = { Text("如：家庭账本、旅行账本", color = bgConfig.textTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = bgConfig.textPrimary,
                        unfocusedTextColor = bgConfig.textPrimary,
                        focusedContainerColor = bgConfig.inputFieldBg,
                        unfocusedContainerColor = bgConfig.inputFieldBg,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = bgConfig.inputFieldBorder,
                        cursorColor = Color(0xFF6366F1)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("book_name_input")
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = bgConfig.textSecondary
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = { if (nameInput.trim().isNotBlank()) onConfirm(nameInput.trim()) },
                        enabled = nameInput.trim().isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(confirmText, color = Color.White)
                    }
                }
            }
        }
    }
}
