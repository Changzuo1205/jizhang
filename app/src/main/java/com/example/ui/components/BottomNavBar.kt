package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalAppBackgroundConfig

enum class AppTab(val title: String) {
    HOME("首页"),
    ACCOUNTS("账户"),
    REPORTS("报表"),
    MINE("我的")
}

@Composable
fun GlassBottomNavBar(
    currentTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    onOpenAddExpense: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgConfig = LocalAppBackgroundConfig.current
    val infiniteTransition = rememberInfiniteTransition(label = "NavGlow")
    
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HomePlusPulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = if (bgConfig.isLight) 0.35f else 0.5f,
        targetValue = if (bgConfig.isLight) 0.70f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HomePlusAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Frosted Bar
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(26.dp),
            backgroundColor = bgConfig.navBarBackground,
            borderColor = bgConfig.navBarBorder,
            borderWidth = 1.2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tab 1: Home / Add Expense Highlight
                val isHome = currentTab == AppTab.HOME
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(
                                bounded = false, 
                                color = if (bgConfig.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.2f)
                            ),
                            onClick = {
                                if (isHome) {
                                    onOpenAddExpense()
                                } else {
                                    onTabSelected(AppTab.HOME)
                                }
                            }
                        )
                        .testTag("nav_tab_home"),
                    contentAlignment = Alignment.Center
                ) {
                    if (isHome) {
                        // Highlighted Glowing Add button state when on Home
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                // Glowing background halo
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .scale(pulseScale)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF6366F1).copy(alpha = pulseAlpha),
                                                    Color(0xFF06B6D4).copy(alpha = pulseAlpha * 0.5f),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                    )

                                // Illuminated Plus button
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFF06B6D4)
                                                )
                                            )
                                        )
                                        .border(1.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "记一笔",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "记一笔",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = if (bgConfig.isLight) Color(0xFF0284C7) else Color(0xFF38BDF8)
                            )
                        }
                    } else {
                        // Standard Home tab when not active
                        NavItem(
                            icon = Icons.Default.Home,
                            title = "首页",
                            selected = false,
                            onClick = { onTabSelected(AppTab.HOME) }
                        )
                    }
                }

                // Tab 2: Accounts
                NavItem(
                    icon = Icons.Default.AccountBalance,
                    title = "账户",
                    selected = currentTab == AppTab.ACCOUNTS,
                    onClick = { onTabSelected(AppTab.ACCOUNTS) },
                    modifier = Modifier.weight(1f).testTag("nav_tab_accounts")
                )

                // Tab 3: Reports
                NavItem(
                    icon = Icons.Default.PieChart,
                    title = "报表",
                    selected = currentTab == AppTab.REPORTS,
                    onClick = { onTabSelected(AppTab.REPORTS) },
                    modifier = Modifier.weight(1f).testTag("nav_tab_reports")
                )

                // Tab 4: Mine
                NavItem(
                    icon = Icons.Default.Person,
                    title = "我的",
                    selected = currentTab == AppTab.MINE,
                    onClick = { onTabSelected(AppTab.MINE) },
                    modifier = Modifier.weight(1f).testTag("nav_tab_mine")
                )
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
    val bgConfig = LocalAppBackgroundConfig.current
    val activeColor = if (bgConfig.isLight) Color(0xFF4F46E5) else Color(0xFF818CF8)
    val inactiveColor = if (bgConfig.isLight) Color(0xFF64748B) else Color.White.copy(alpha = 0.5f)

    val iconColor by animateColorAsState(
        targetValue = if (selected) activeColor else inactiveColor,
        label = "NavIconColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    bounded = false, 
                    color = if (bgConfig.isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
                ),
                onClick = onClick
            )
            .padding(vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier.size(26.dp),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                // Glow indicator under selected icon
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    activeColor.copy(alpha = if (bgConfig.isLight) 0.25f else 0.45f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            ),
            color = iconColor
        )
    }
}
