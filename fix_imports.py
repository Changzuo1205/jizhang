with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
imports_to_add = [
    "import androidx.compose.foundation.horizontalScroll\n",
    "import androidx.compose.foundation.rememberScrollState\n"
]
added = False

for line in lines:
    if line.strip() == "import androidx.compose.foundation.horizontalScroll":
        continue
    if line.strip() == "import androidx.compose.foundation.rememberScrollState":
        continue
    
    if line.startswith("import ") and not added:
        new_lines.extend(imports_to_add)
        added = True
        
    new_lines.append(line)

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'w') as f:
    f.writelines(new_lines)
