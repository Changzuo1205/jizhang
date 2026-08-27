import re

with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace('version = 7', 'version = 8')

with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'w', encoding='utf-8') as f:
    f.write(content)

print("Bumped DB version to 8")
