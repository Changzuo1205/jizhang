import re

with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'r', encoding='utf-8') as f:
    db_content = f.read()

db_content = db_content.replace('version = 4', 'version = 5')
db_content = db_content.replace('version = 5', 'version = 6')
db_content = db_content.replace('version = 6', 'version = 7')
db_content = db_content.replace('daily_expense_db_v4', 'daily_expense_db_v7')
db_content = db_content.replace('daily_expense_db_v5', 'daily_expense_db_v7')

with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'w', encoding='utf-8') as f:
    f.write(db_content)

print("Bumped DB version")
