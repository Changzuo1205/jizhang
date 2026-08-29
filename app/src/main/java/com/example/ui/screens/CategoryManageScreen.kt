package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import com.example.data.local.CategoryManager
import com.example.data.local.entity.CategoryEntity
import com.example.ui.components.GlassBackgroundWithGlow
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassChip
import com.example.ui.theme.LocalAppBackgroundConfig

/**
 * 分类管理页（Phase 2 子页）。
 *
 * 支出/收入两个 Tab 各自展示「一级 → 二级」树：点击一级展开/收起二级，
 * 支持新增一级、在某一级下新增二级与归档任意层级。
 * 数据来自 ViewModel 暴露的 category 表原始平面行，本页在内存中组树。
 */
@Composable
fun CategoryManageScreen(
    categories: List<CategoryEntity>,
    onCreateCategory: (parentName: String?, name: String, type: String) -> Unit = { _, _, _ -> },
    onArchiveCategory: (categoryId: Long) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 拦截系统返回手势回到上级
    BackHandler(enabled = true) { onBack() }

    val bgConfig = LocalAppBackgroundConfig.current

    // 0 = 支出分类，1 = 收入分类
    var selectedTypeIndex by remember { mutableIntStateOf(0) }
    val currentDbType = if (selectedTypeIndex == 1) "income" else "expense"

    // 展开状态：每个分类 Tab 独立维护的展开集合
    val expandedIds = remember(currentDbType) { mutableStateListOf<Long>() }

    // 新建对话框状态：非空 parent 表示正在为该一级添加二级
    var showCreateDialog by remember { mutableStateOf(false) }
    var createParent by remember { mutableStateOf<CategoryEntity?>(null) }

    val parents = categories.filter { it.parentId == null && it.type == currentDbType }
    val childrenByParent = categories
        .filter { it.parentId != null && it.type == currentDbType }
        .groupBy { it.parentId!! }

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
                                .testTag("categories_back_button")
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
                                text = "分类管理",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = bgConfig.textPrimary
                            )
                            Text(
                                text = "两级分类树维护 · 归档不影响历史账目",
                                style = MaterialTheme.typography.bodySmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 支出 / 收入 分段切换
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (bgConfig.isLight) Color(0xFFE2E8F0) else Color.White.copy(alpha = 0.08f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTypeIndex == 0) Color(0xFFF43F5E).copy(alpha = 0.85f) else Color.Transparent)
                            .clickable { selectedTypeIndex = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "支出分类",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 0) Color.White else bgConfig.textSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selectedTypeIndex == 1) Color(0xFF10B981).copy(alpha = 0.85f) else Color.Transparent)
                            .clickable { selectedTypeIndex = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "收入分类",
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTypeIndex == 1) Color.White else bgConfig.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 新增一级入口
                GlassCard(
                    shape = RoundedCornerShape(18.dp),
                    onClick = {
                        createParent = null
                        showCreateDialog = true
                    },
                    modifier = Modifier.fillMaxWidth().testTag("create_category_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = if (bgConfig.isLight) 0.12f else 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "新增一级分类",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "＋",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (bgConfig.isLight) Color(0xFF4F46E5) else Color(0xFF818CF8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 树形列表
                parents.forEach { parent ->
                    val isExpanded = expandedIds.contains(parent.id)
                    val subs = childrenByParent[parent.id].orEmpty()
                    CategoryParentRow(
                        parent = parent,
                        children = subs,
                        isExpanded = isExpanded,
                        onToggleExpand = {
                            if (isExpanded) expandedIds.remove(parent.id) else expandedIds.add(parent.id)
                        },
                        onAddChild = {
                            createParent = parent
                            showCreateDialog = true
                        },
                        onArchiveChild = onArchiveCategory,
                        onArchiveSelf = { onArchiveCategory(parent.id) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                if (parents.isEmpty()) {
                    GlassCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "暂无分类，点击上方「新增一级分类」创建",
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

    // 新建对话框（parent 为空即新增一级，否则为该一级添加二级）
    if (showCreateDialog) {
        CategoryCreateDialog(
            parentName = createParent?.name,
            typeLabel = if (selectedTypeIndex == 1) "收入" else "支出",
            onDismiss = { showCreateDialog = false },
            onConfirm = { name ->
                onCreateCategory(createParent?.name, name, currentDbType)
                showCreateDialog = false
            }
        )
    }
}

/** 一级分类卡：可展开二级，支持在该级下新增二级与自我归档 */
@Composable
private fun CategoryParentRow(
    parent: CategoryEntity,
    children: List<CategoryEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddChild: () -> Unit,
    onArchiveChild: (Long) -> Unit,
    onArchiveSelf: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val glowColor = CategoryManager.getCategoryGlowColor(parent.name)

    GlassCard(shape = RoundedCornerShape(18.dp), modifier = modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(glowColor.copy(alpha = if (bgConfig.isLight) 0.14f else 0.28f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = CategoryManager.getCategoryIcon(parent.name),
                        contentDescription = null,
                        tint = glowColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = parent.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    text = "${children.size} 项",
                    style = MaterialTheme.typography.labelSmall,
                    color = bgConfig.textTertiary
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "收起" else "展开",
                    tint = bgConfig.textTertiary,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                children.forEach { child ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (bgConfig.isLight) Color(0xFFF8FAFC) else Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(glowColor.copy(alpha = 0.85f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = child.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = bgConfig.textSecondary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1
                        )
                        Text(
                            text = "归档",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = bgConfig.textTertiary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onArchiveChild(child.id) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassChip(selected = false, onClick = onAddChild) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = if (bgConfig.isLight) Color(0xFF4F46E5) else Color(0xFF818CF8),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "新增二级",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = bgConfig.textPrimary
                            )
                        }
                    }
                    GlassChip(selected = false, onClick = onArchiveSelf) {
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
                                text = "归档此类",
                                style = MaterialTheme.typography.labelSmall,
                                color = bgConfig.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 新建分类输入对话框：[parentName] 为空即新增一级 */
@Composable
private fun CategoryCreateDialog(
    parentName: String?,
    typeLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current
    var nameInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            shape = RoundedCornerShape(22.dp),
            backgroundColor = bgConfig.dialogBackground,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = if (parentName == null) "新增${typeLabel}一级分类" else "为「$parentName」新增二级细分",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = bgConfig.textPrimary
                )
                if (parentName != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "挂在「$parentName」之下 · ${typeLabel}类型",
                        style = MaterialTheme.typography.labelSmall,
                        color = bgConfig.textTertiary
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("分类名称", color = bgConfig.textSecondary) },
                    placeholder = { Text("如：宠物、数码（或细分名）", color = bgConfig.textTertiary) },
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
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
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
                        Text("确认添加", color = Color.White)
                    }
                }
            }
        }
    }
}
