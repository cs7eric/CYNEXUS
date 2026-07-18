from __future__ import annotations

from collections.abc import Mapping
from typing import Any


BROAD_KEYWORDS = (
    "沪深300", "中证500", "中证1000", "中证2000", "上证50", "创业板",
    "科创50", "科创100", "中证A50", "A500", "A100", "A50", "国证2000", "国证A50", "红利",
    "上证指数", "上证180", "上证380", "上证580", "深证成指", "中证全指",
    "中证800", "中证A100", "中小100", "中创400", "创100", "创50", "创新100", "创业大盘",
    "创业综指", "创中盘", "上证综指", "上证中盘", "A股",
    "MSCI中国",
)

SECTOR_KEYWORDS = (
    "半导体", "芯片", "人工智能", "AI", "软件", "计算机", "通信", "机器人",
    "新能源", "光伏", "电池", "汽车", "军工", "医药", "医疗", "证券", "银行",
    "保险", "消费", "食品", "酒", "有色", "煤炭", "钢铁", "稀土", "房地产",
    "基建", "电力", "环保", "游戏", "传媒", "旅游", "农业", "养殖", "港股",
    "中药", "云计算", "互联网", "信创", "信息安全", "信息技术", "信息科技", "TMT",
    "数据", "软件", "专精特新", "交运", "交通运输", "公用事业", "农牧", "养殖",
    "创新药", "医药生物", "生物科技", "生物医药", "医疗器械", "疫苗", "消费电子",
    "电子", "芯片", "家电", "建材", "化工", "石化", "石油", "油气", "有色金属",
    "稀土", "旅游", "酒店", "传媒", "影视", "动漫", "游戏", "教育", "机械",
    "工程机械", "工业母机", "智能制造", "智能驾驶", "汽车零部件", "电网设备",
    "电力设备", "通信设备", "物联网", "卫星", "航空航天", "国防", "军工", "券商",
    "证券", "保险", "银行", "金融", "物流", "电信", "材料", "新材料", "农业",
    "食品饮料", "消费", "汽车", "地产", "房地产", "建筑", "基建", "环保", "能源",
    "一带一路", "大湾区", "产业升级", "创科技",
)

NON_SECTOR_KEYWORDS = (
    "纳斯达克", "标普", "恒生", "港股", "港股通", "黄金", "原油", "商品",
    "债券", "国债", "信用债", "货币", "现金", "美元", "日经", "德国", "法国",
    "越南", "东南亚", "亚太", "全球", "海外", "ESG", "低碳", "价值", "增强",
    "质量", "治理", "自由现金流", "现金流", "多因子", "红利低波",
)


def classify_etf(name: str | None) -> tuple[str, str | None]:
    """Classify an ETF name conservatively when authoritative metadata is unavailable."""
    text = str(name or "").strip().upper()
    for keyword in BROAD_KEYWORDS:
        if keyword.upper() in text:
            return "broad", keyword
    for keyword in SECTOR_KEYWORDS:
        if keyword.upper() in text:
            return "sector", keyword
    for keyword in NON_SECTOR_KEYWORDS:
        if keyword.upper() in text:
            return "other", None
    return "other", None


def normalize_etf_code(value: Any) -> str:
    text = str(value or "").strip().upper()
    if "." in text:
        text = text.split(".", 1)[0]
    return text.zfill(6) if text.isdigit() else text


def exchange_for_code(code: str) -> str:
    return "SSE" if str(code).startswith(("5", "6")) else "SZSE"


def numeric(value: Any) -> float | None:
    if value is None or value == "":
        return None
    try:
        result = float(value)
    except (TypeError, ValueError):
        return None
    return None if result != result else result


def integer(value: Any) -> int | None:
    result = numeric(value)
    return int(result) if result is not None else None


def normalize_spot_row(
    row: dict[str, Any],
    *,
    captured_at: str,
    classification_map: Mapping[str, Mapping[str, Any]] | None = None,
) -> dict[str, Any]:
    code = normalize_etf_code(row.get("代码") or row.get("code"))
    name = str(row.get("名称") or row.get("name") or code)
    fallback_category, fallback_theme = classify_etf(name)
    official = (classification_map or {}).get(code) or {}
    category = str(official.get("category") or fallback_category)
    theme = official.get("theme") or fallback_theme
    classification_source = str(official.get("source") or "fallback.name_keyword")
    return {
        "code": code,
        "exchange": exchange_for_code(code),
        "name": name,
        "category": category,
        "theme": theme,
        "classification_source": classification_source,
        "price": numeric(row.get("最新价")),
        "prev_close": numeric(row.get("昨收")),
        "change_pct": numeric(row.get("涨跌幅")),
        "volume": numeric(row.get("成交量")),
        "amount": numeric(row.get("成交额")),
        "iopv": numeric(row.get("IOPV实时估值")),
        "premium_pct": numeric(row.get("基金折价率")),
        "turnover_pct": numeric(row.get("换手率")),
        "inside_volume": numeric(row.get("内盘")),
        "outside_volume": numeric(row.get("外盘")),
        "latest_shares": numeric(row.get("最新份额")),
        "super_large_net_inflow": numeric(row.get("超大单净流入-净额")),
        "super_large_net_ratio": numeric(row.get("超大单净流入-净占比")),
        "main_net_inflow": numeric(row.get("主力净流入-净额")),
        "main_net_ratio": numeric(row.get("主力净流入-净占比")),
        "large_net_inflow": numeric(row.get("大单净流入-净额")),
        "large_net_ratio": numeric(row.get("大单净流入-净占比")),
        "medium_net_inflow": numeric(row.get("中单净流入-净额")),
        "medium_net_ratio": numeric(row.get("中单净流入-净占比")),
        "small_net_inflow": numeric(row.get("小单净流入-净额")),
        "small_net_ratio": numeric(row.get("小单净流入-净占比")),
        "captured_at": captured_at,
        "raw_payload": row,
    }
