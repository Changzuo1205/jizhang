import re
with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

if 'import androidx.compose.foundation.layout.requiredWidth' not in content:
    content = content.replace('import androidx.compose.foundation.layout.fillMaxWidth', 'import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.requiredWidth')

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
