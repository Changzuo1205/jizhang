cat << 'INNER_EOF' > temp_import.txt
                ProfilePreferenceRow(
                    icon = Icons.Outlined.Upload,
                    title = "明细导入",
                    subtitle = "从其他平台导入账单",
                    iconColor = forestGreen,
                    textMain = textMain,
                    textMuted = textMuted,
                    dividerColor = dividerColor,
                    onClick = { filePickerLauncher.launch("text/*") }
                )
INNER_EOF
awk '
/icon = Icons.Outlined.Upload/ {
    system("cat temp_import.txt")
    skip = 1
    next
}
skip {
    if (/onClick = /) {
        skip = 0
    }
    next
}
{ print }
' app/src/main/java/com/example/ui/screens/MineScreen.kt > out.kt
mv out.kt app/src/main/java/com/example/ui/screens/MineScreen.kt
