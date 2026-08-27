import json

with open("app/src/main/res/raw/initial_expenses_part1.json") as f: d1 = json.load(f)
with open("app/src/main/res/raw/initial_expenses_part2.json") as f: d2 = json.load(f)

# Combine and deduplicate
seen = set()
merged = []
for item in d1 + d2:
    key = (item["date_str"], item["amount"], item["note"])
    if key not in seen:
        seen.add(key)
        merged.append(item)

# Update account mapping
# 1 币安, 2 现金, 3 农业银行储蓄卡, 4 微信钱包, 5 黄金, 6 招商银行储蓄卡, 7 基金, 8 支付宝
acc_map = {
    '币安': 1,
    '现金': 2,
    '农业': 3,
    '微信': 4,
    '黄金': 5,
    '招商': 6,
    '基金': 7,
    '支付宝': 8
}

def get_acc_id(name):
    for k, v in acc_map.items():
        if k in name:
            return v
    return 4 # Default to 微信

for item in merged:
    item['accountId'] = get_acc_id(item['accountName'])
    
merged.sort(key=lambda x: x["date_str"], reverse=True)

with open("app/src/main/assets/initial_expenses.json", "w", encoding="utf-8") as f: 
    json.dump(merged, f, ensure_ascii=False, indent=2)

print(f"Total unique items updated: {len(merged)}")
