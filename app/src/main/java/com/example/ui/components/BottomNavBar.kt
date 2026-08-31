package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke

enum class AppTab(val title: String) {
    ACCOUNTS("账户"),
    DESIGN("设计稿"),
    HOME("首页"),
    REPORTS("报表"),
    MINE("我的")
}

@Composable
fun BottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onOpenAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val isLight = bgConfig.isLight
    
    // 暖纸白背景 #F5F1E6
    val backgroundColor = if (isLight) Color(0xFFF5F1E6) else Color(0xFF1E281E) // Fallback for dark if needed, but per instruction, background same as page (warm paper white). Assuming dark mode has its own bg, but instruction specifically said "#F5F1E6".
    // 顶部只有一条 0.5.dp 的分隔线，无阴影
    val dividerColor = Color(0xFFDCD5C0)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .navigationBarsPadding() // 通栏贴底，正常处理系统手势区
    ) {
        HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                icon = Icons.Outlined.AccountBalance,
                title = AppTab.ACCOUNTS.title,
                selected = currentTab == AppTab.ACCOUNTS,
                onClick = { onTabSelected(AppTab.ACCOUNTS) },
                modifier = Modifier.weight(1f)
            )
            
            NavItem(
                icon = Icons.Outlined.Edit,
                title = AppTab.DESIGN.title,
                selected = currentTab == AppTab.DESIGN,
                onClick = { onTabSelected(AppTab.DESIGN) },
                modifier = Modifier.weight(1f)
            )
            
            HomeOrQuickAddButton(
                isHomeSelected = currentTab == AppTab.HOME,
                onNavigateHome = { onTabSelected(AppTab.HOME) },
                onQuickAdd = onOpenAddExpense,
                modifier = Modifier.weight(1f)
            )
            
            NavItem(
                icon = Icons.Outlined.PieChart, // outline pie chart
                title = AppTab.REPORTS.title,
                selected = currentTab == AppTab.REPORTS,
                onClick = { onTabSelected(AppTab.REPORTS) },
                modifier = Modifier.weight(1f)
            )
            
            NavItem(
                icon = Icons.Outlined.Person,
                title = AppTab.MINE.title,
                selected = currentTab == AppTab.MINE,
                onClick = { onTabSelected(AppTab.MINE) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun HomeOrQuickAddButton(
    isHomeSelected: Boolean,
    onNavigateHome: () -> Unit,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = isHomeSelected,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
            },
            label = "home_btn_transition"
        ) { isHome ->
            if (isHome) {
                // 状态 B: 圆形印章造型按钮
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .offset(y = (-14).dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onQuickAdd
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Color(0xFFC4623D),
                                    radius = size.minDimension / 2f,
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(Color(0xFFC4623D), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "记一笔",
                                tint = Color(0xFFF5F1E6),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "记一笔",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFC4623D)
                    )
                }
            } else {
                // 状态 A: 朴素的首页图标
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onNavigateHome
                        )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Home,
                        contentDescription = "首页",
                        tint = Color(0xFF8A8270),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "首页",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF8A8270)
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = Color(0xFF2D6A4F)
    val inactiveColor = Color(0xFF8A8270)
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "NavIconColor"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = iconColor
        )
    }
}
