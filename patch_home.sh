cat << 'INNER_EOF' > tmp.kt
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
INNER_EOF
awk '/@Composable/{if(flag==1) {print; flag=0; next}} /private fun HomeButtonContent/{flag=1; system("cat tmp.kt"); next} {if(flag==0) print}' app/src/main/java/com/example/ui/components/BottomNavBar.kt > out.kt
mv out.kt app/src/main/java/com/example/ui/components/BottomNavBar.kt
