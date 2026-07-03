from __future__ import annotations

from typing import Final


INDUSTRY_INDEX_CODES: Final[list[tuple[str, str]]] = [
    ("sh880301", "农副食品"),
    ("sh880302", "纺织服饰"),
    ("sh880303", "轻工制造"),
    ("sh880304", "医药生物"),
    ("sh880305", "公用事业"),
    ("sh880306", "交通运输"),
    ("sh880307", "房地产"),
    ("sh880308", "商贸零售"),
    ("sh880309", "社会服务"),
    ("sh880310", "综合"),
    ("sh880311", "建筑材料"),
    ("sh880312", "建筑装饰"),
    ("sh880313", "电力设备"),
    ("sh880314", "机械设备"),
    ("sh880315", "国防军工"),
    ("sh880316", "汽车"),
    ("sh880317", "美容护理"),
    ("sh880318", "家用电器"),
    ("sh880319", "食品饮料"),
    ("sh880320", "煤炭"),
    ("sh880321", "石油石化"),
    ("sh880322", "环保"),
    ("sh880323", "银行"),
    ("sh880324", "非银金融"),
    ("sh880325", "钢铁"),
    ("sh880326", "有色金属"),
    ("sh880327", "电子"),
    ("sh880328", "计算机"),
    ("sh880329", "传媒"),
    ("sh880330", "通信"),
    ("sh880331", "农林牧渔"),
    ("sh880332", "基础化工"),
]


CONCEPT_INDEX_CODES: Final[list[tuple[str, str]]] = [
    ("sh880401", "沪深300概念"),
    ("sh880402", "中证500概念"),
    ("sh880403", "中证1000概念"),
    ("sh880404", "中证2000概念"),
    ("sh880405", "红利概念"),
    ("sh880406", "高股息概念"),
    ("sh880407", "国企改革"),
    ("sh880408", "央企改革"),
    ("sh880409", "一带一路"),
    ("sh880410", "粤港澳大湾区"),
    ("sh880411", "长三角一体化"),
    ("sh880412", "雄安新区"),
    ("sh880413", "海南自贸港"),
    ("sh880414", "上海自贸"),
    ("sh880415", "数字货币"),
    ("sh880416", "区块链"),
    ("sh880417", "人工智能"),
    ("sh880418", "大数据"),
    ("sh880419", "云计算"),
    ("sh880420", "5G"),
    ("sh880421", "芯片"),
    ("sh880422", "国产芯片"),
    ("sh880423", "存储芯片"),
    ("sh880424", "半导体"),
    ("sh880425", "集成电路"),
    ("sh880426", "工业互联网"),
    ("sh880427", "智能制造"),
    ("sh880428", "卫星互联网"),
    ("sh880429", "低空经济"),
    ("sh880430", "人形机器人"),
    ("sh880431", "CPO"),
    ("sh880432", "数字经济"),
    ("sh880433", "新能源"),
    ("sh880434", "锂电池"),
    ("sh880435", "固态电池"),
    ("sh880436", "储能"),
    ("sh880437", "充电桩"),
    ("sh880438", "光伏"),
    ("sh880439", "风电"),
    ("sh880440", "核电"),
    ("sh880441", "氢能源"),
    ("sh880442", "特高压"),
    ("sh880443", "碳中和"),
    ("sh880444", "军工"),
    ("sh880445", "国产替代"),
    ("sh880446", "高端装备"),
    ("sh880447", "智慧城市"),
    ("sh880448", "乡村振兴"),
    ("sh880449", "医美"),
    ("sh880450", "创新药"),
]

TDX_INDUSTRY_56: Final[dict[str, str]] = {
    "880301": "煤炭",
    "880305": "电力",
    "880310": "石油",
    "880318": "钢铁",
    "880324": "有色",
    "880330": "化纤",
    "880335": "化工",
    "880344": "建材",
    "880350": "造纸",
    "880351": "矿物制品",
    "880355": "日用化工",
    "880360": "农林牧渔",
    "880367": "纺织服饰",
    "880372": "食品饮料",
    "880380": "酿酒",
    "880387": "家用电器",
    "880390": "汽车类",
    "880398": "医疗保健",
    "880399": "家居用品",
    "880400": "医药",
    "880406": "商业连锁",
    "880414": "商贸代理",
    "880418": "传媒娱乐",
    "880421": "广告包装",
    "880422": "文教休闲",
    "880423": "酒店餐饮",
    "880424": "旅游",
    "880430": "航空",
    "880431": "船舶",
    "880432": "运输设备",
    "880437": "通用机械",
    "880440": "工业机械",
    "880446": "电气设备",
    "880447": "工程机械",
    "880448": "电器仪表",
    "880452": "电信运营",
    "880453": "公共交通",
    "880454": "水务",
    "880455": "供气供热",
    "880456": "环境保护",
    "880459": "运输服务",
    "880464": "仓储物流",
    "880465": "交通设施",
    "880471": "银行",
    "880472": "证券",
    "880473": "保险",
    "880474": "多元金融",
    "880476": "建筑",
    "880482": "房地产",
    "880489": "IT设备",
    "880490": "通信设备",
    "880491": "半导体",
    "880492": "元器件",
    "880493": "软件服务",
    "880494": "互联网",
    "880497": "综合类",
}


TDX_INDUSTRY_SEEDS: Final[dict[str, list[str]]] = {
    "880301": ["sh601088", "sh601225"],
    "880305": ["sh600886", "sh600025"],
    "880310": ["sh601857", "sh600028"],
    "880318": ["sh600019", "sh600010"],
    "880324": ["sh601600", "sh601899"],
    "880330": ["sh600346", "sz000301"],
    "880335": ["sh600309", "sz000792"],
    "880344": ["sh600585", "sz000877"],
    "880350": ["sh600567", "sz000488"],
    "880351": ["sz002080", "sh603650"],
    "880355": ["sh603605", "sz300740"],
    "880360": ["sh600598", "sz000876"],
    "880367": ["sh600400", "sz002154"],
    "880372": ["sh600887", "sz000895"],
    "880380": ["sh600519", "sz000858"],
    "880387": ["sz000333", "sh600690"],
    "880390": ["sh600104", "sz000625"],
    "880398": ["sh600196", "sz000538"],
    "880399": ["sh603833", "sz002572"],
    "880400": ["sh600276", "sz000538"],
    "880406": ["sh600415", "sz002024"],
    "880414": ["sh600058", "sz000906"],
    "880418": ["sh600977", "sz300133"],
    "880421": ["sz002701", "sz002117"],
    "880422": ["sh603899", "sz300077"],
    "880423": ["sh600754", "sz000428"],
    "880424": ["sh600138", "sz300144"],
    "880430": ["sh600029", "sh601111"],
    "880431": ["sh600150", "sh601728"],
    "880432": ["sh600031", "sz000425"],
    "880437": ["sh600031", "sz000425"],
    "880440": ["sz000425", "sh600031"],
    "880446": ["sh601727", "sh600416"],
    "880447": ["sz000528", "sh600031"],
    "880448": ["sh600071", "sz002415"],
    "880452": ["sh600050", "sh600776"],
    "880453": ["sh600611", "sh600834"],
    "880454": ["sh600008", "sh601158"],
    "880455": ["sh600635", "sh601139"],
    "880456": ["sh600323", "sz300070"],
    "880459": ["sh601111", "sh600029"],
    "880464": ["sh600026", "sh601598"],
    "880465": ["sh600018", "sh600548"],
    "880471": ["sh601398", "sh600036"],
    "880472": ["sh600030", "sh601066"],
    "880473": ["sh601628", "sh601336"],
    "880474": ["sz000563", "sh600816"],
    "880476": ["sh601668", "sh601800"],
    "880482": ["sh600048", "sz001979"],
    "880489": ["sz000997", "sh603019"],
    "880490": ["sh600487", "sz000063"],
    "880491": ["sh688981", "sh688256"],
    "880492": ["sh603501", "sh603160"],
    "880493": ["sh600588", "sh600845"],
    "880494": ["sh603000", "sz300017"],
    "880497": ["sh600621", "sh601992"],
}


def normalize_index_code(code: str) -> str:
    value = str(code or "").strip().lower()
    if not value:
        return ""
    if value.startswith(("sh", "sz", "bj")):
        return value
    if value.isdigit():
        return f"sh{value}"
    return value


def infer_index_kind(code: str) -> str:
    normalized = normalize_index_code(code)
    if normalized.startswith("sh8803"):
        return "industry"
    if normalized.startswith("sh8804"):
        return "concept"
    if normalized.startswith("sh881"):
        return "sector"
    return "unknown"


def get_industry_codes() -> list[tuple[str, str]]:
    return list(INDUSTRY_INDEX_CODES)


def get_concept_codes() -> list[tuple[str, str]]:
    return list(CONCEPT_INDEX_CODES)


def all_index_codes(extra_items: list[dict[str, str]] | None = None) -> list[tuple[str, str, str]]:
    out: list[tuple[str, str, str]] = [(code, name, "industry") for code, name in INDUSTRY_INDEX_CODES]
    out.extend((code, name, "concept") for code, name in CONCEPT_INDEX_CODES)
    seen = {code for code, _, _ in out}
    for item in extra_items or []:
        code = normalize_index_code(item.get("code") or "")
        name = str(item.get("name") or "").strip()
        kind = str(item.get("kind") or infer_index_kind(code)).strip().lower() or "unknown"
        if code and name and code not in seen:
            out.append((code, name, kind))
            seen.add(code)
    return out


def lookup_index_name(code: str, extra_items: list[dict[str, str]] | None = None) -> str | None:
    normalized = normalize_index_code(code)
    if not normalized:
        return None
    for current_code, name, _kind in all_index_codes(extra_items=extra_items):
        if current_code == normalized:
            return name
    return None


def get_tdx_industry_56() -> dict[str, str]:
    return dict(TDX_INDUSTRY_56)


def get_tdx_industry_56_pairs() -> list[tuple[str, str]]:
    return [(f"sh{code6}", name) for code6, name in TDX_INDUSTRY_56.items()]


def get_tdx_industry_name(code6_or_full: str) -> str | None:
    raw = normalize_index_code(code6_or_full)
    code6 = raw[2:] if raw[:2] in {"sh", "sz", "bj"} else raw
    return TDX_INDUSTRY_56.get(code6)


def tdx_industry_seed_for(code6_or_full: str) -> list[str]:
    raw = normalize_index_code(code6_or_full)
    code6 = raw[2:] if raw[:2] in {"sh", "sz", "bj"} else raw
    return list(TDX_INDUSTRY_SEEDS.get(code6, []))
