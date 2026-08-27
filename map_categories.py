import json
from datetime import datetime

with open('app/src/main/assets/initial_expenses.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for d in data:
    amt = d['amount']
    note = d['note']
    dt = datetime.strptime(d['date_str'], '%Y-%m-%d %H:%M:%S')
    hour = dt.hour
    cat = "其他"
    subcat = "其他"
    
    is_income = d['type'] == 'INCOME'

    # Known notes mapping
    if note == "Gemini": cat, subcat = "购物", "电子数码"
    elif note == "高铁": cat, subcat = "交通", "火车"
    elif note == "矿泉水": cat, subcat = "餐饮", "晚餐" # Usually
    elif note == "余额调整产生的差额":
        if is_income:
            cat, subcat = "转账", "转账"
        else:
            cat, subcat = "居家", "漏记款"
    elif note == "专业注册费 住宿费": cat, subcat = "医教", "学费"
    elif note == "课本": cat, subcat = "购物", "报刊书籍"
    elif note == "media plus" or note == "MiniMax plus": cat, subcat = "餐饮", "午餐" # From prompt
    elif note == "路由器": cat, subcat = "购物", "电子数码"
    elif note == "BBQ": cat, subcat = "餐饮", "晚餐"
    elif note == "001423": cat, subcat = "投资", "基金"
    elif note == "众包保证金": cat, subcat = "交通", "交通"
    elif note == "寄件": cat, subcat = "购物", "其他"
    elif note == "VPN": cat, subcat = "购物", "电子数码"
    elif note == "DeepSeek": cat, subcat = "购物", "电子数码"
    elif note == "qwen": cat, subcat = "购物", "电子数码"
    elif note == "基金卖出": cat, subcat = "基金", "其他"
    elif note == "ghelper": cat, subcat = "餐饮", "晚餐"
    elif note == "seedream5pro": cat, subcat = "购物", "电子数码"
    elif note == "支付宝转入": cat, subcat = "娱乐", "娱乐" # From prompt
    elif note == "转到微信": cat, subcat = "餐饮", "晚餐"
    elif note == "生活费": cat, subcat = "娱乐", "娱乐"
    elif note == "烧烤": cat, subcat = "餐饮", "晚餐"
    elif note == "无畏契约": cat, subcat = "娱乐", "网游电玩" # Or "购物" > "电子数码" in one case
    elif "车票" in note: cat, subcat = "交通", "交通"
    elif note == "水卡": cat, subcat = "餐饮", "夜宵"
    elif "洗衣机" in note: cat, subcat = "购物", "电子数码"
    elif note == "瑞幸": cat, subcat = "餐饮", "午餐"
    elif "电费" in note: cat, subcat = "购物", "电子数码"
    elif note == "蜜雪冰城": cat, subcat = "餐饮", "晚餐"
    elif note == "黄焖鸡": cat, subcat = "餐饮", "晚餐"
    elif note == "腊肉拌面": cat, subcat = "餐饮", "午餐"
    elif note == "鸡柳饼": cat, subcat = "餐饮", "夜宵"
    elif note == "塔斯汀": cat, subcat = "餐饮", "午餐"
    elif note == "贵烟": cat, subcat = "餐饮", "午餐"
    elif note == "甜筒": cat, subcat = "餐饮", "晚餐"
    elif note == "馄饨面": cat, subcat = "餐饮", "早餐"
    elif note == "泡面" or note == "方便面": cat, subcat = "餐饮", "午餐"
    elif note == "地铁" or note == "公交": cat, subcat = "交通", "交通"
    elif note == "币": cat, subcat = "餐饮", "夜宵"
    elif note == "父亲节眼罩": cat, subcat = "人情", "礼金红包"
    elif note == "流浪泡泡": cat, subcat = "餐饮", "午餐"
    elif note == "手抓饼": cat, subcat = "餐饮", "夜宵"
    elif note == "张雪峰": cat, subcat = "餐饮", "午餐"
    elif note == "学术部零食蛋糕": cat, subcat = "餐饮", "晚餐"
    elif note == "综述": cat, subcat = "医教", "医教"
    elif note == "颜悦": cat, subcat = "餐饮", "夜宵"
    elif note == "东方树叶": cat, subcat = "餐饮", "晚餐"
    elif note == "烤冷面": cat, subcat = "餐饮", "夜宵"
    elif note == "学术部干事笔": cat, subcat = "人情", "礼金红包"
    elif note == "巧乐兹": cat, subcat = "餐饮", "晚餐"
    elif note == "麻辣拌": cat, subcat = "餐饮", "晚餐"
    elif note == "面包": cat, subcat = "餐饮", "早餐"
    elif note == "冰淇淋": cat, subcat = "餐饮", "午餐"
    elif note == "空调电费": cat, subcat = "购物", "电子数码"
    elif note == "团费": cat, subcat = "医教", "医教"
    elif note == "母亲节": cat, subcat = "人情", "礼金红包"
    elif note == "话费": cat, subcat = "购物", "电子数码"
    elif note == "多又佳": cat, subcat = "医教", "医教"
    elif note == "视听说": cat, subcat = "医教", "医教"
    elif note == "冰露": cat, subcat = "餐饮", "午餐"
    elif note == "细白将": cat, subcat = "餐饮", "午餐"
    elif note == "网吧": cat, subcat = "餐饮", "晚餐"
    elif note == "排骨米饭": cat, subcat = "餐饮", "午餐"
    elif note == "报告册": cat, subcat = "医教", "医教"
    elif note == "吃饭": cat, subcat = "餐饮", "夜宵"
    elif note == "煊赫门": cat, subcat = "餐饮", "晚餐"
    elif note == "电动车": cat, subcat = "交通", "交通"
    elif note == "饮料": cat, subcat = "餐饮", "晚餐"
    elif note == "兰州拉面": cat, subcat = "餐饮", "晚餐"
    elif note == "洗衣服": cat, subcat = "餐饮", "晚餐"
    elif note == "尖叫": cat, subcat = "餐饮", "晚餐"
    elif note == "炸鸡": cat, subcat = "餐饮", "晚餐"
    elif note == "炒河粉": cat, subcat = "餐饮", "午餐"
    elif note == "笼王焖面": cat, subcat = "餐饮", "晚餐"
    elif note == "姐": cat, subcat = "人情", "人情"
    elif note == "打车": cat, subcat = "交通", "交通"
    elif note == "KTV" or note == "卡拉OK": cat, subcat = "娱乐", "卡拉OK"
    elif note == "夸克网盘续费": cat, subcat = "购物", "电子数码"
    elif note == "羽毛球": cat, subcat = "娱乐", "娱乐"
    elif note == "娘": cat, subcat = "人情", "礼金红包"
    elif note == "大叹号": cat, subcat = "餐饮", "晚餐"
    elif note == "烟水": cat, subcat = "餐饮", "晚餐"
    elif note == "压岁": cat, subcat = "人情", "礼金红包"
    elif note == "还钱": cat, subcat = "人情", "人情"
    elif note == "pbl报告": cat, subcat = "医教", "医教"
    elif note == "讲义": cat, subcat = "医教", "医教"
    elif "矿泉水" in note: cat, subcat = "餐饮", "晚餐"
    elif not is_income and note == "":
        # Time-based heuristics
        if amt in [1.94, 0.96, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00, 7.00, 8.00] and (d['accountId'] == 4 or d['accountId'] == 1):
            if amt in [0.96, 1.94]:
                if hour < 10: cat, subcat = "交通", "交通"
                elif hour < 15: cat, subcat = "餐饮", "午餐"
                elif hour < 21: cat, subcat = "餐饮", "晚餐"
                else: cat, subcat = "餐饮", "夜宵"
            else:
                if 5 <= hour < 10: cat, subcat = "餐饮", "早餐"
                elif 10 <= hour < 15: cat, subcat = "餐饮", "午餐"
                elif 15 <= hour < 21: cat, subcat = "餐饮", "晚餐"
                else: cat, subcat = "餐饮", "夜宵"
        else:
            if 5 <= hour < 10: cat, subcat = "餐饮", "早餐"
            elif 10 <= hour < 15: cat, subcat = "餐饮", "午餐"
            elif 15 <= hour < 21: cat, subcat = "餐饮", "晚餐"
            else: cat, subcat = "餐饮", "夜宵"
    elif is_income:
        if d['category'] == '兼职外快': cat, subcat = '兼职外快', '兼职外快'
        elif d['category'] == '红包': cat, subcat = '红包', '红包'
        elif amt == 240.0: cat, subcat = "兼职外快", "兼职外快"
        elif amt == 200.0 and note == "": cat, subcat = "兼职外快", "兼职外快"
        elif note == "余额调整产生的差额": cat, subcat = "转账", "转账"
        else:
            cat, subcat = d['category'], d['category']

    # Try to map back some specific exact amounts from the prompt
    if amt == 3025.0: cat, subcat = "医教", "学费"
    elif amt == 4386.0: cat, subcat = "医教", "学费"
    elif amt == 1998.36 or amt == 1998.41: cat, subcat = "居家", "漏记款"
    elif amt == 1538.55 and d['accountId'] == 3: cat, subcat = "基金", "转账"
    elif amt == 1538.55 and d['accountId'] == 2: cat, subcat = "投资", "出资"
    elif amt == 888.0: cat, subcat = "人情", "礼金红包"
    
    # Capital Flow
    if cat == "交通" and note == "": cat, subcat = "交通", "交通"
    if note == "车票": cat, subcat = "交通", "交通"

    # From prompt JSON
    if d['date_str'] == "2026-04-20 16:45:13": cat, subcat = "报销款", "报销款"
    if d['date_str'] == "2026-04-18 13:11:58": cat, subcat = "应收款", "应收款"
    
    d['category'] = cat
    d['subCategory'] = subcat
    
with open('app/src/main/assets/initial_expenses.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Finished heuristic mapping")
