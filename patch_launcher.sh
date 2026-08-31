cat << 'INNER_EOF' > temp_launcher.txt
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val csv = inputStream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                onImportCsv(csv)
                Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
INNER_EOF
awk '
/val bgConfig = LocalAppBackgroundConfig.current/ {
    system("cat temp_launcher.txt")
    print
    next
}
{ print }
' app/src/main/java/com/example/ui/screens/MineScreen.kt > out.kt
mv out.kt app/src/main/java/com/example/ui/screens/MineScreen.kt
