cat << 'INNER_EOF' > temp_click.txt
                    onClick = {
                        val csv = onGenerateCsv()
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, csv)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "导出 CSV")
                        context.startActivity(shareIntent)
                    }
INNER_EOF
awk '
/onClick = onGenerateCsv/ {
    system("cat temp_click.txt")
    next
}
{ print }
' app/src/main/java/com/example/ui/screens/MineScreen.kt > out.kt
mv out.kt app/src/main/java/com/example/ui/screens/MineScreen.kt
