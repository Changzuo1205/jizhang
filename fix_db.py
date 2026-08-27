import re

# 1. Fix Database version
with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'r', encoding='utf-8') as f:
    db_content = f.read()

db_content = db_content.replace('version = 4', 'version = 5')
db_content = db_content.replace('version = 5', 'version = 6') # Just to be safe, increment to 6
db_content = db_content.replace('daily_expense_db_v4', 'daily_expense_db_v6')

with open('app/src/main/java/com/example/data/local/DailyToolboxDatabase.kt', 'w', encoding='utf-8') as f:
    f.write(db_content)

# 2. Fix CategoryManager for "漏记款" as EXPENSE
with open('app/src/main/java/com/example/data/repository/CategoryManager.kt', 'r', encoding='utf-8') as f:
    cat_content = f.read()

# Make sure we have 漏记款 under EXPENSE
# Let's see what is there
