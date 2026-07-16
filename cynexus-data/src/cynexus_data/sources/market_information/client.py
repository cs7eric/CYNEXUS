from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime
import hashlib
import html
import re
from typing import Any, Iterable
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup


UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/149.0.0.0 Safari/537.36"


@dataclass(frozen=True)
class MarketInformationSource:
    key: str
    url: str
    category: str
    name: str
    referer: str | None = None
    max_items: int = 40


DEFAULT_SOURCES: tuple[MarketInformationSource, ...] = (
    MarketInformationSource("gov.cn.policy", "https://www.gov.cn/zhengce/index.htm", "policy", "中国政府网"),
    MarketInformationSource("pboc.news", "http://www.pbc.gov.cn/goutongjiaoliu/113456/113469/index.html", "macro", "中国人民银行"),
    MarketInformationSource("stats.gov.cn.release", "https://www.stats.gov.cn/sj/zxfb/", "macro", "国家统计局"),
    MarketInformationSource("mof.policy", "https://www.mof.gov.cn/zhengwuxinxi/zhengcefabu/", "policy", "财政部"),
    MarketInformationSource("ndrc.news", "https://www.ndrc.gov.cn/xwdt/", "industry", "国家发展改革委"),
    MarketInformationSource("csrc.news", "https://www.csrc.gov.cn/csrc/c100028/common_list.shtml", "regulation", "证监会"),
    MarketInformationSource("szse.disclosure", "https://www.szse.cn/disclosure/", "regulation", "深交所"),
    MarketInformationSource("xinhua.finance", "https://www.news.cn/fortune/", "macro", "新华社财经"),
    MarketInformationSource("cctv.finance", "https://news.cctv.com/finance/", "macro", "央视财经"),
    MarketInformationSource("caam.industry", "http://www.caam.org.cn/chn/8/cate_82/list_1.html", "industry", "中国汽车工业协会"),
    MarketInformationSource("caam.policy", "http://www.caam.org.cn/chn/9/cate_97/list_1.html", "policy", "中国汽车工业协会"),
    MarketInformationSource("chinania.news", "https://www.chinania.org.cn/html/hangyexinwen/guoneixinwen/", "industry", "中国有色金属工业协会"),
    MarketInformationSource("chinania.stats", "https://www.chinania.org.cn/html/hangyetongji/jqzs/", "industry", "中国有色金属工业协会"),
    MarketInformationSource("chinania.policy", "https://www.chinania.org.cn/html/zcfg/zhengcefagui/", "policy", "中国有色金属工业协会"),
)

_DATE_PATTERN = re.compile(r"(?:19|20)\d{2}[-/]?\d{2}[-/]?\d{2}")
_NAV_WORDS = {
    "首页", "网站地图", "联系我们", "English", "English Version", "登录", "注册", "搜索", "更多", "上一页", "下一页",
    "返回顶部", "关闭", "打印", "分享", "字体", "无障碍浏览", "简体中文", "繁体中文",
}


@dataclass
class OfficialMarketInformationClient:
    session: requests.Session = field(default_factory=requests.Session)
    timeout: float = 20.0
    sources: tuple[MarketInformationSource, ...] = DEFAULT_SOURCES

    def __post_init__(self) -> None:
        self.session.headers.setdefault("User-Agent", UA)
        self.session.headers.setdefault("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        self.session.headers.setdefault("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")

    def fetch(self, *, source_keys: Iterable[str] | None = None, page_size: int = 20) -> list[dict[str, Any]]:
        wanted = {str(item).strip() for item in source_keys or () if str(item).strip()}
        selected = [item for item in self.sources if not wanted or item.key in wanted]
        rows: list[dict[str, Any]] = []
        for source in selected:
            try:
                rows.extend(self._fetch_source(source, page_size=min(max(page_size, 1), source.max_items)))
            except requests.RequestException:
                # A single official site being down must not block the other sources.
                continue
            except (UnicodeError, ValueError):
                continue
        dedup: dict[str, dict[str, Any]] = {}
        for row in rows:
            dedup.setdefault(str(row["id"]), row)
        return sorted(dedup.values(), key=lambda item: item.get("time") or "", reverse=True)

    def _fetch_source(self, source: MarketInformationSource, *, page_size: int) -> list[dict[str, Any]]:
        response = self.session.get(source.url, headers={"Referer": source.referer or source.url}, timeout=self.timeout)
        response.raise_for_status()
        text = _decode_html(response)
        soup = BeautifulSoup(text, "html.parser")
        rows: list[dict[str, Any]] = []
        seen: set[str] = set()
        for anchor in _candidate_links(soup):
            title = _clean_text(anchor.get_text(" ", strip=True))
            href = str(anchor.get("href") or "").strip()
            if not _is_candidate_title(title) or not href:
                continue
            url = urljoin(response.url, href)
            if not url.startswith(("http://", "https://")):
                continue
            item_id = hashlib.sha1(f"{source.key}|{url}|{title}".encode("utf-8")).hexdigest()
            if item_id in seen:
                continue
            seen.add(item_id)
            rows.append(
                {
                    "id": item_id,
                    "title": title,
                    "summary": "",
                    "time": _extract_date(anchor, url),
                    "source": source.key,
                    "media_name": source.name,
                    "url": url,
                    "category": source.category,
                    "raw_payload": {"source": source.key, "category": source.category, "publisher": source.name},
                }
            )
            if len(rows) >= page_size:
                break
        return rows


def _candidate_links(soup: BeautifulSoup) -> list[Any]:
    selectors = ("main li a[href]", "article a[href]", ".list li a[href]", ".news_list li a[href]", "ul li a[href]")
    links: list[Any] = []
    seen: set[int] = set()
    for selector in selectors:
        for anchor in soup.select(selector):
            marker = id(anchor)
            if marker not in seen:
                seen.add(marker)
                links.append(anchor)
    if not links:
        links = list(soup.select("a[href]"))
    return links


def _decode_html(response: requests.Response) -> str:
    raw = response.content
    candidates = ["utf-8", response.apparent_encoding, "gb18030"]
    for encoding in candidates:
        if not encoding:
            continue
        try:
            value = raw.decode(encoding, errors="strict")
        except (LookupError, UnicodeDecodeError):
            continue
        if "�" not in value:
            return value
    return raw.decode("utf-8", errors="replace")


def _clean_text(value: str) -> str:
    return " ".join(html.unescape(value or "").replace("\u00a0", " ").split())


def _is_candidate_title(value: str) -> bool:
    if not value or len(value) < 8 or len(value) > 180 or value in _NAV_WORDS:
        return False
    if value.startswith(("http://", "https://", "www.")):
        return False
    return any("\u4e00" <= char <= "\u9fff" for char in value)


def _extract_date(anchor: Any, url: str) -> str:
    current = anchor
    for _ in range(4):
        if current is None:
            break
        text = _clean_text(current.get_text(" ", strip=True))
        match = _DATE_PATTERN.search(text) or _DATE_PATTERN.search(url)
        if match:
            raw = match.group(0).replace("/", "-")
            if len(raw) == 8 and raw.isdigit():
                raw = f"{raw[:4]}-{raw[4:6]}-{raw[6:]}"
            return raw[:10]
        current = getattr(current, "parent", None)
    return datetime.now().date().isoformat()
