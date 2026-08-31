package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

enum class AppTab(val title: String) {
    ACCOUNTS("账户"),
    DESIGN("设计稿"),
    HOME("首页"),
    REPORTS("报表"),
    MINE("我的")
}

// 统一的轻量按压反馈替代 ripple
fun Modifier.pressScale(interactionSource: MutableInteractionSource): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "press_scale"
    )
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
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
    val backgroundColor = if (isLight) Color(0xFFF5F1E6) else Color(0xFF1E281E)
    // 顶部只有一条 0.5.dp 的分隔线，无阴影
    val dividerColor = bgConfig.dividerColor
    val navBarHeight = 60.dp
    
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. NavigationBar 作为第一层（主体），中间那个位置留空占位
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(backgroundColor)
                .navigationBarsPadding() // 通栏贴底，正常处理系统手势区
        ) {
            HorizontalDivider(thickness = 0.5.dp, color = dividerColor)
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight),
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
                
                Spacer(modifier = Modifier.weight(1f)) // 中间占位，不放实际内容
                
                NavItem(
                    icon = Icons.Outlined.PieChart,
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

        // 2. 中间的双态按钮作为 Box 的第二个子元素，与主体同级，悬浮定位
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .height(80.dp)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.weight(2f)) // 跳过前两个
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HomeOrQuickAddButton(
                        isHomeSelected = currentTab == AppTab.HOME,
                        onNavigateHome = { onTabSelected(AppTab.HOME) },
                        onQuickAdd = onOpenAddExpense
                    )
                }
                Spacer(modifier = Modifier.weight(2f)) // 跳过后两个
            }
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
    AnimatedContent(
        targetState = isHomeSelected,
        transitionSpec = {
            fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(250))
        },
        label = "centerButton",
        modifier = modifier.fillMaxSize()
    ) { selected ->
        if (selected) {
            QuickAddButtonContent(onQuickAdd = onQuickAdd)
        } else {
            HomeButtonContent(onNavigateHome = onNavigateHome)
        }
    }
}

@Composable
private fun QuickAddButtonContent(onQuickAdd: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp)
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 去除默认 ripple
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
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "记一笔",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFC4623D)
        )
    }
}

@Composable
private fun HomeButtonContent(onNavigateHome: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 10.dp)
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 去除默认 ripple
                onClick = onNavigateHome
            )
    ) {
        Icon(
            imageVector = Icons.Outlined.Home,
            contentDescription = "首页",
            tint = Color(0xFF8A8270),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "首页",
            fontSize = 10.sp,
            fontWeight = FontWeight.Normal,
            color = Color(0xFF8A8270)
        )
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
    val interactionSource = remember { MutableInteractionSource() }
    
    val iconColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "NavIconColor"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // 去除默认 ripple
                onClick = onClick
            )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = iconColor
        )
    }
}
