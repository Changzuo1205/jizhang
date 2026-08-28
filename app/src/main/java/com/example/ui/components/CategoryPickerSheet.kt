package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.CategoryItem
import com.example.data.local.CategoryManager
import com.example.ui.theme.LocalAppBackgroundConfig

@Composable
fun CategoryPickerSheet(
    currentType: String,
    allCategories: List<CategoryItem>,
    selectedCategory: String,
    selectedSubCategory: String,
    onDismiss: () -> Unit,
    onSelectCategoryAndSub: (category: String, subCategory: String) -> Unit,
    onAddCategory: () -> Unit,
    onAddSubCategory: () -> Unit,
    onManageCategories: (() -> Unit)? = null,
    accentColor: Color = Color(0xFFF97316)
) {
    val context = LocalContext.current
    val bgConfig = LocalAppBackgroundConfig.current

    var currentCategoryName by remember { mutableStateOf(selectedCategory) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    val subcategories = remember(currentCategoryName, currentType) {
        CategoryManager.getSubcategories(context, currentCategoryName, currentType)
    }

    val filteredSubcategories = remember(subcategories, searchQuery) {
        if (searchQuery.isBlank()) subcategories
        else subcategories.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    val catGlow = CategoryManager.getCategoryGlowColor(currentCategoryName)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (bgConfig.isLight) Color.White else Color(0xFF1E222B),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header: Down arrow, Title "选择类别", Search icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "收起",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "选择类别",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = bgConfig.textPrimary
                    )

                    IconButton(
                        onClick = { isSearching = !isSearching },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isSearching) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "搜索细分",
                            tint = bgConfig.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (isSearching) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索细分类别", fontSize = 14.sp, color = bgConfig.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = bgConfig.inputFieldBg,
                            unfocusedContainerColor = bgConfig.inputFieldBg,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = bgConfig.inputFieldBorder
                        ),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Major Category Active Header (e.g. 餐饮 ^)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (bgConfig.isLight) Color(0xFFF8FAFC) else Color(0xFF282C37))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(catGlow.copy(alpha = 0.22f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryManager.getCategoryIcon(currentCategoryName),
                                contentDescription = null,
                                tint = catGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = currentCategoryName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = bgConfig.textPrimary
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "已展开",
                        tint = bgConfig.textTertiary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Vertical Subcategories List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Default/Self row
                    item {
                        val isSelfSelected = currentCategoryName == selectedCategory &&
                            (selectedSubCategory == currentCategoryName || selectedSubCategory.isBlank() || selectedSubCategory == "其他")
                        SubcategoryRowItem(
                            title = currentCategoryName,
                            icon = CategoryManager.getCategoryIcon(currentCategoryName),
                            iconColor = catGlow,
                            isSelected = isSelfSelected,
                            accentColor = accentColor,
                            onClick = {
                                onSelectCategoryAndSub(currentCategoryName, "其他")
                            }
                        )
                    }

                    items(filteredSubcategories.filter { it != "其他" && it != currentCategoryName }) { sub ->
                        val isSelected = currentCategoryName == selectedCategory && selectedSubCategory == sub
                        val subIcon = getSubcategoryIcon(sub, currentCategoryName)

                        SubcategoryRowItem(
                            title = sub,
                            icon = subIcon,
                            iconColor = catGlow,
                            isSelected = isSelected,
                            accentColor = accentColor,
                            onClick = {
                                onSelectCategoryAndSub(currentCategoryName, sub)
                            }
                        )
                    }

                    // "+ 添加细分" entry inside list
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onAddSubCategory() }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "添加细分",
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "添加「$currentCategoryName」细分",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = accentColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Action Bar: "+ 添加类别" & "管理"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { onAddCategory() }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = "添加类别",
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "添加类别",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }

                    Text(
                        text = "管理",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (bgConfig.isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier
                            .clickable {
                                onManageCategories?.invoke() ?: onDismiss()
                            }
                            .padding(vertical = 6.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubcategoryRowItem(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val bgConfig = LocalAppBackgroundConfig.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(iconColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) accentColor else bgConfig.textPrimary
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

fun getSubcategoryIcon(sub: String, parentCategory: String): ImageVector {
    return when {
        sub.contains("早餐") || sub.contains("早点") -> Icons.Default.Restaurant
        sub.contains("午餐") || sub.contains("中餐") -> Icons.Default.Restaurant
        sub.contains("晚餐") || sub.contains("晚饭") -> Icons.Default.Restaurant
        sub.contains("夜宵") || sub.contains("宵夜") || sub.contains("烧烤") -> Icons.Default.LocalBar
        sub.contains("零食") || sub.contains("点心") || sub.contains("水果") -> Icons.Default.ShoppingBag
        sub.contains("饮料") || sub.contains("奶茶") || sub.contains("咖啡") -> Icons.Default.LocalCafe
        sub.contains("打车") || sub.contains("出租") || sub.contains("网约车") -> Icons.Default.DirectionsCar
        sub.contains("公交") || sub.contains("巴士") -> Icons.Default.DirectionsBus
        sub.contains("地铁") || sub.contains("轨道") -> Icons.Default.Train
        sub.contains("火车") || sub.contains("高铁") -> Icons.Default.Train
        sub.contains("飞机") || sub.contains("机票") -> Icons.Default.Flight
        sub.contains("加油") || sub.contains("燃油") -> Icons.Default.LocalGasStation
        sub.contains("停车") -> Icons.Default.LocalParking
        sub.contains("服饰") || sub.contains("衣服") || sub.contains("鞋") -> Icons.Default.Checkroom
        sub.contains("数码") || sub.contains("手机") || sub.contains("电脑") -> Icons.Default.PhoneAndroid
        sub.contains("电影") || sub.contains("影院") -> Icons.Default.Movie
        sub.contains("游戏") || sub.contains("电玩") -> Icons.Default.SportsEsports
        sub.contains("宠物") || sub.contains("猫") || sub.contains("狗") -> Icons.Default.Pets
        sub.contains("健身") || sub.contains("运动") -> Icons.Default.FitnessCenter
        sub.contains("水电") || sub.contains("电费") || sub.contains("水费") || sub.contains("燃气") -> Icons.Default.Bolt
        sub.contains("房租") || sub.contains("房贷") -> Icons.Default.Home
        sub.contains("宽带") || sub.contains("网络") -> Icons.Default.Wifi
        sub.contains("快递") || sub.contains("邮政") -> Icons.Default.LocalShipping
        sub.contains("漏记") -> Icons.Default.Bookmark
        sub.contains("药品") || sub.contains("挂号") || sub.contains("医疗") -> Icons.Default.MedicalServices
        sub.contains("学费") || sub.contains("培训") -> Icons.Default.School
        sub.contains("基金") || sub.contains("股票") || sub.contains("理财") -> Icons.Default.TrendingUp
        sub.contains("红包") || sub.contains("礼金") -> Icons.Default.CardGiftcard
        sub.contains("工资") || sub.contains("薪水") -> Icons.Default.Work
        sub.contains("兼职") -> Icons.Default.Assignment
        sub.contains("转账") -> Icons.Default.SwapHoriz
        else -> CategoryManager.getCategoryIcon(parentCategory)
    }
}
