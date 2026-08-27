import json
from datetime import datetime

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

# Categories
breakfast = 0
lunch = 0
dinner = 0
snack = 0

for d in data:
    if d['amount'] > 0 and d['category'] == '其他' and d['note'] == '':
        dt = datetime.strptime(d['date_str'], '%Y-%m-%d %H:%M:%S')
        hour = dt.hour
        if 5 <= hour < 10:
            breakfast += 1
        elif 10 <= hour < 15:
            lunch += 1
        elif 15 <= hour < 21:
            dinner += 1
        else:
            snack += 1

print(f"Breakfast: {breakfast}, Lunch: {lunch}, Dinner: {dinner}, Snack: {snack}")
