import json

with open("app/src/main/assets/initial_expenses.json", "r", encoding="utf-8") as f:
    data = json.load(f)

for item in data:
    # If the original category was "转账" or the note was a transfer, the user said it's income.
    # Wait, the user specifically said: "不知道你是否已经弄明白“转账”类型其实是收入"
    # So any item that was originally a "转账" in the CSV was parsed.
    # In my previous script, I did:
    # type_str = "EXPENSE" if parts[0] == "支出" else ("INCOME" if parts[0] == "收入" else "EXPENSE")
    # if parts[0] == "转账": cat = "转账"
    # This means all "转账" items have type="EXPENSE" and category="转账", except those where I didn't set category to "转账" if it was already set?
    # Let's just find anything with category "转账", or account transfer, and set type to INCOME.
    if item["category"] == "转账":
        item["type"] = "INCOME"
        item["category"] = "其他" # Or "漏记款"? Let's set it to "其他" or keep it as "转账" and add "转账" to income categories.
    elif item["note"] == "余额调整产生的差额":
        # Maybe these are also income if they are positive?
        # In the app, amount is positive.
        pass

with open("app/src/main/assets/initial_expenses.json", "w", encoding="utf-8") as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Updated transfers to INCOME")
