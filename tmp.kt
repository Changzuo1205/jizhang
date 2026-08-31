@Composable
private fun HomeButtonContent(onNavigateHome: () -> Unit) {
    NavItem(
        icon = Icons.Outlined.Home,
        title = "首页",
        selected = true,
        onClick = onNavigateHome,
        modifier = Modifier.fillMaxSize()
    )
}
