import json

with open("app/src/main/assets/initial_expenses.json", "r", encoding="utf-8") as f:
    data = json.load(f)

for item in data:
    # Any item that was previously transferred and is now INCOME, or has specific note
    if item["note"] == "余额调整产生的差额":
        item["type"] = "INCOME"
        item["category"] = "转账"
    elif item["category"] == "其他" and item["type"] == "INCOME" and item["note"] == "":
        # For those that were plain transfer without note
        item["category"] = "转账"

with open("app/src/main/assets/initial_expenses.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Fixed transfers to category 转账")
