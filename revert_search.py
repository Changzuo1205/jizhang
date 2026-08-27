import re

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# I want to remove the Circular Search Icon Button and Expandable Search Box
# Let's extract the part from // Circular Search Icon Button to the end of the Expandable Search Box
pattern = r'(// Circular Search Icon Button.*?)(?=\n\s*\}\n\s*\}\n\s*// Empty State)'
match = re.search(pattern, content, flags=re.DOTALL)
if match:
    print("Found search box!")
    content = content.replace(match.group(1), '')

with open('app/src/main/java/com/example/ui/screens/ExpenseScreen.kt', 'w', encoding='utf-8') as f:
    f.write(content)
