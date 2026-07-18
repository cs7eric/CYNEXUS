from __future__ import annotations

from collections.abc import Mapping
from typing import Any

from cynexus_data.domain.market.etf import classify_etf


MappingRow = dict[str, Any]


ASSET_RULES = (
    ("bond", "债券", "债券"),
    ("bond", "国债", "国债"),
    ("bond", "信用债", "信用债"),
    ("currency", "货币", "货币"),
    ("currency", "现金管理", "现金管理"),
    ("currency", "添益", "现金管理"),
    ("currency", "日日鑫", "现金管理"),
    ("currency", "快钱", "现金管理"),
    ("commodity", "黄金", "黄金"),
    ("commodity", "金ETF", "黄金"),
    ("commodity", "原油", "原油"),
    ("commodity", "油气", "油气"),
    ("commodity", "大宗商品", "大宗商品"),
    ("commodity", "豆粕", "豆粕"),
)

REGION_RULES = (
    ("hong_kong", ("港股", "恒生", "港股通", "沪港深", "沪深港"), "中国香港"),
    ("overseas", ("纳斯达克", "纳指", "标普", "日经", "日本", "德国", "法国", "巴西", "沙特", "美国", "道琼斯", "东南亚", "亚太", "全球", "海外"), "海外"),
)

INDEX_RULES = (
    ("csi_300", "沪深300", "沪深300"),
    ("csi_500", "中证500", "中证500"),
    ("csi_1000", "中证1000", "中证1000"),
    ("csi_2000", "中证2000", "中证2000"),
    ("csi_800", "中证800", "中证800"),
    ("csi_a50", "中证A50", "中证A50"),
    ("csi_a500", "A500", "中证A500"),
    ("csi_a100", "A100", "中证A100"),
    ("sse_50", "上证50", "上证50"),
    ("sse_180", "上证180", "上证180"),
    ("sse_380", "上证380", "上证380"),
    ("sse_580", "上证580", "上证580"),
    ("szse_100", "深证100", "深证100"),
    ("szse_100", "深100", "深证100"),
    ("szse_component", "深成", "深证成指"),
    ("szse_50", "深证50", "深证50"),
    ("szse_main_50", "深证主板50", "深证主板50"),
    ("chinext", "创业板", "创业板"),
    ("chinext", "创业综指", "创业板综合"),
    ("star_50", "科创50", "科创50"),
    ("star_100", "科创100", "科创100"),
    ("star_200", "科创200", "科创200"),
    ("star_composite", "科创综指", "科创综指"),
    ("star_growth", "科创成长", "科创成长"),
    ("star_chinext", "科创创业", "科创创业"),
    ("sme_100", "中小100", "中小100"),
    ("csi_2000", "国证2000", "国证2000"),
    ("csi_2000", "中创400", "中创400"),
    ("large_cap", "超大盘", "超大盘"),
    ("large_cap", "核心50", "核心50"),
    ("large_cap", "漂亮50", "漂亮50"),
)

STYLE_RULES = (
    ("dividend", ("红利", "高股息"), "红利"),
    ("low_volatility", ("低波", "低波动"), "低波"),
    ("value", ("价值",), "价值"),
    ("growth", ("成长",), "成长"),
    ("quality", ("质量",), "质量"),
    ("enhanced", ("增强",), "增强"),
    ("esg", ("ESG", "责任", "可持续"), "ESG/责任投资"),
    ("cash_flow", ("现金流", "自由现金流"), "现金流"),
    ("fundamental", ("基本面",), "基本面"),
    ("state_owned", ("国企", "央企", "民企"), "国企/央企/民企"),
    ("carbon_neutral", ("低碳", "碳中和"), "低碳/碳中和"),
)

THEME_RULES = (
    ("5g", ("5G",), "5G"),
    ("semiconductor", ("半导体", "芯片", "集成电路"), "半导体/芯片"),
    ("ai", ("人工智能", "AI", "科创信息"), "人工智能"),
    ("technology", ("科技", "数字经济", "信创", "信息技术", "信息科技"), "科技/数字经济"),
    ("cloud_computing", ("云计算",), "云计算"),
    ("internet", ("互联网",), "互联网"),
    ("communication", ("通信", "电信"), "通信"),
    ("robotics", ("机器人",), "机器人"),
    ("new_energy", ("新能源", "光伏", "电池", "绿电"), "新能源"),
    ("carbon_neutral", ("低碳", "碳中和"), "低碳/碳中和"),
    ("automobile", ("汽车", "智能车", "智能驾驶"), "汽车"),
    ("military", ("军工", "国防", "航空", "航天", "船舶", "通用航空"), "军工/航空航天"),
    ("medicine", ("医药", "医疗", "中药", "创新药", "生物科技", "生物医药", "疫苗", "药ETF"), "医药医疗"),
    ("consumer", ("消费", "食品", "酒", "家电", "国货"), "消费"),
    ("brokerage", ("证券", "券商"), "证券"),
    ("banking", ("银行",), "银行"),
    ("insurance", ("保险",), "保险"),
    ("non_bank_finance", ("非银", "金融"), "非银金融"),
    ("metals", ("有色", "稀土", "稀有金属", "资源", "矿业"), "有色/资源"),
    ("coal", ("煤炭",), "煤炭"),
    ("steel", ("钢铁",), "钢铁"),
    ("chemical", ("化工", "石化"), "化工"),
    ("oil_gas", ("石油", "原油", "油气"), "石油/油气"),
    ("real_estate", ("房地产", "地产"), "房地产"),
    ("construction", ("基建", "建筑", "建材", "工程机械"), "建筑/基建"),
    ("power_grid", ("电力", "电网设备", "电力设备"), "电力/电网"),
    ("environment", ("环保",), "环保"),
    ("media", ("传媒", "影视", "动漫", "游戏"), "传媒"),
    ("education", ("教育",), "教育"),
    ("agriculture", ("农业", "农牧", "养殖", "粮食"), "农业"),
    ("transportation", ("交通运输", "交运", "物流"), "交通运输"),
    ("machinery", ("机械", "智能制造", "高端制造", "高端装备", "工业母机"), "机械/高端制造"),
    ("materials", ("材料", "新材料"), "新材料"),
    ("regional_development", ("一带一路", "大湾区", "长三角", "成渝", "湾创"), "区域发展"),
)


def build_etf_mapping_rows(
    code: str,
    name: str | None,
    *,
    category: str | None = None,
    classification_source: str | None = None,
    mapping_source: str = "analysis.name_rule_v3",
) -> list[MappingRow]:
    text = str(name or "").strip().upper()
    inferred_category, _ = classify_etf(name)
    universe = category or inferred_category
    source = classification_source if classification_source and not classification_source.startswith("fallback.") else mapping_source
    confidence = 1.0 if classification_source and not classification_source.startswith("fallback.") else 0.72
    rows: list[MappingRow] = []

    def add(group: str, value: str, label: str, *, primary: bool = False, row_confidence: float = confidence, row_source: str = source) -> None:
        rows.append({
            "etf_code": code,
            "mapping_group": group,
            "mapping_code": value,
            "mapping_name": label,
            "is_primary": primary,
            "confidence": row_confidence,
            "source": row_source,
        })

    add("universe", universe, {"broad": "宽基", "sector": "行业/主题", "other": "其他"}.get(universe, universe), primary=True)

    asset = "equity"
    asset_label = "权益"
    for value, keyword, label in ASSET_RULES:
        if keyword.upper() in text:
            asset, asset_label = value, label
            break
    add("asset_class", asset, asset_label, primary=True)

    region = "china_a"
    region_label = "中国A股"
    for value, keywords, label in REGION_RULES:
        if any(keyword.upper() in text for keyword in keywords):
            region, region_label = value, label
            break
    add("region", region, region_label, primary=True)

    for value, keyword, label in INDEX_RULES:
        if keyword.upper() in text:
            add("index_scope", value, label, primary=True)

    for value, keywords, label in STYLE_RULES:
        if any(keyword.upper() in text for keyword in keywords):
            add("style", value, label)

    for value, keywords, label in THEME_RULES:
        if any(keyword.upper() in text for keyword in keywords):
            add("theme", value, label)

    return rows


def build_etf_mapping_rows_for_catalog(rows: list[Mapping[str, Any]]) -> list[MappingRow]:
    mappings: list[MappingRow] = []
    for row in rows:
        mappings.extend(build_etf_mapping_rows(
            str(row.get("code") or ""),
            row.get("name"),
            category=row.get("category"),
            classification_source=row.get("classification_source"),
        ))
    return mappings
