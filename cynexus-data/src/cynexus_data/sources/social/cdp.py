from __future__ import annotations

import hashlib
import json
import os
import re
import threading
from contextlib import contextmanager
from dataclasses import replace
from datetime import date, datetime, time, timedelta
from typing import Any, Iterator
from urllib.parse import parse_qs, urlencode, urlsplit, urlunsplit
from zoneinfo import ZoneInfo

from cynexus_data.connections.models import Connection
from cynexus_data.sources.social.browser import SocialAccessError
from cynexus_data.sources.social.models import SocialComment, SocialCreator, SocialPost
from cynexus_data.sources.social.parser import extract_post_key, parse_comments, parse_datetime_value


PLATFORM_HOME_URLS = {
    "xiaohongshu": "https://www.xiaohongshu.com/",
    "douyin": "https://www.douyin.com/",
}
DEFAULT_POST_SELECTORS = {
    "xiaohongshu": "a[href*='/explore/'], a[href*='/discovery/item/']",
    "douyin": "a[href*='/video/'], a[href*='/note/']",
}
DEFAULT_COMMENT_SELECTORS = {
    # Xiaohongshu exposes a stable leaf node for each comment. Selecting the
    # generic `comment` class also matches the list and reply containers, whose
    # innerText contains every descendant comment.
    "xiaohongshu": ".comment-item",
    # Douyin's current desktop DOM exposes each comment, including replies, as
    # a `data-e2e="comment-item"` node. Avoid broad class selectors here:
    # `comment` also matches the list, input, author badge, and stats wrappers.
    "douyin": "[data-e2e='comment-item']",
}
SHANGHAI_TZ = ZoneInfo("Asia/Shanghai")


class CdpSocialClient:
    """Collect from a user-owned Chrome instance through its CDP endpoint."""

    def __init__(self, platform: str, connection: Connection, profile_name: str = "default") -> None:
        normalized = str(platform or "").strip().lower()
        if normalized not in PLATFORM_HOME_URLS:
            raise ValueError(f"unsupported social platform: {platform}")
        self.platform = normalized
        self.connection = connection
        self.profile_name = profile_name
        configured = connection.extra.get("extra") if isinstance(connection.extra.get("extra"), dict) else connection.extra
        self._configured = configured
        self.timeout_ms = _timeout_ms(connection.timeout, 30_000)
        self.wait_ms = _bounded_int(configured.get("wait_ms"), 2_000, 0, 30_000)
        self.scroll_rounds = _bounded_int(configured.get("scroll_rounds"), 12, 1, 60)
        self.comment_scroll_rounds = _optional_bounded_int(
            configured.get("cdp_comment_scroll_rounds") or os.getenv("CYNEXUS_SOCIAL_CDP_COMMENT_SCROLL_ROUNDS"),
            1,
            100_000,
        )
        self.scroll_wait_ms = _bounded_int(configured.get("scroll_wait_ms"), 800, 100, 10_000)
        self.post_limit = _bounded_int(configured.get("cdp_post_limit"), 20, 1, 100)
        self.comment_limit = _optional_bounded_int(
            configured.get("cdp_comment_limit") or os.getenv("CYNEXUS_SOCIAL_CDP_COMMENT_LIMIT"),
            1,
            1_000_000,
        )
        self.reply_expand_limit = _optional_bounded_int(
            configured.get("douyin_reply_expand_limit") or os.getenv("CYNEXUS_SOCIAL_DOUYIN_REPLY_EXPAND_LIMIT"),
            1,
            1_000_000,
        )
        self.cdp_host = str(configured.get("cdp_host") or os.getenv("CYNEXUS_SOCIAL_CDP_HOST") or "127.0.0.1")
        self.cdp_port = self._resolve_port(configured)
        self._lock = threading.Lock()

    @contextmanager
    def connected(self) -> Iterator[tuple[Any, Any, Any]]:
        try:
            from playwright.sync_api import sync_playwright
        except ImportError as exc:
            raise SocialAccessError("PLAYWRIGHT_UNAVAILABLE", "CDP 采集需要安装 Playwright") from exc

        playwright = sync_playwright().start()
        try:
            try:
                browser = playwright.chromium.connect_over_cdp(self.endpoint, timeout=self.timeout_ms)
                context = browser.contexts[0] if browser.contexts else browser.new_context(locale="zh-CN")
            except Exception as exc:
                raise SocialAccessError(
                    "CDP_BROWSER_NOT_CONNECTED",
                    f"无法连接 {self.platform} 专用 Chrome（{self.endpoint}）。请先启动本机社媒采集器。",
                ) from exc
            yield playwright, browser, context
        finally:
            # Disconnect Playwright only. The Chrome process and its profile stay alive.
            playwright.stop()

    @property
    def endpoint(self) -> str:
        return f"http://{self.cdp_host}:{self.cdp_port}"

    def open_login_page(self) -> tuple[Any, Any, Any, Any]:
        """Connect and return a live page; caller owns playwright.stop()."""
        try:
            from playwright.sync_api import sync_playwright
        except ImportError as exc:
            raise SocialAccessError("PLAYWRIGHT_UNAVAILABLE", "CDP 登录需要安装 Playwright") from exc
        playwright = sync_playwright().start()
        try:
            browser = playwright.chromium.connect_over_cdp(self.endpoint, timeout=self.timeout_ms)
            context = browser.contexts[0] if browser.contexts else browser.new_context(locale="zh-CN")
            page = context.pages[0] if context.pages else context.new_page()
            page.goto(PLATFORM_HOME_URLS[self.platform], wait_until="domcontentloaded", timeout=self.timeout_ms)
            page.wait_for_timeout(self.wait_ms)
            return playwright, browser, context, page
        except Exception as exc:
            playwright.stop()
            if isinstance(exc, SocialAccessError):
                raise
            raise SocialAccessError(
                "CDP_BROWSER_NOT_CONNECTED",
                f"无法打开 {self.platform} 专用 Chrome 页面，请确认 CDP 端口 {self.cdp_port} 已启动。",
            ) from exc

    def discover_posts(self, creator: SocialCreator) -> list[SocialPost]:
        with self.connected() as (_playwright, _browser, context):
            page = context.new_page()
            try:
                self._goto(page, creator.profile_url)
                rows = self._post_rows(page)
                if self.platform == "douyin":
                    return self._discover_douyin(creator, page, rows)
                return self._discover_xiaohongshu(creator, page, rows)
            finally:
                page.close()

    def fetch_comments(self, post: SocialPost) -> list[SocialComment]:
        with self.connected() as (_playwright, _browser, context):
            page = context.new_page()
            try:
                self._goto(page, post.url)
                if self.platform == "douyin":
                    self._verify_douyin_post(page, post)
                    self._open_douyin_comments(page)
                selector = self._selector("comment_selectors", DEFAULT_COMMENT_SELECTORS[self.platform])
                rows: list[dict[str, Any]] = []
                stable_rounds = 0
                extract_script = _XHS_COMMENT_ROWS_SCRIPT if self.platform == "xiaohongshu" else _DOUYIN_COMMENT_ROWS_SCRIPT
                max_rounds = self.comment_scroll_rounds
                rounds = 0
                while max_rounds is None or rounds < max_rounds:
                    rounds += 1
                    if self.platform == "xiaohongshu":
                        self._expand_xhs_replies(page)
                    elif self.platform == "douyin":
                        self._load_more_douyin_comments(page)
                        self._expand_douyin_replies(page)
                    current = page.locator(selector).evaluate_all(extract_script)
                    before = len(rows)
                    if isinstance(current, list):
                        rows = _merge_comment_rows(rows, current)
                    if len(rows) == before:
                        stable_rounds += 1
                    else:
                        stable_rounds = 0
                    if stable_rounds >= 3 or (self.comment_limit is not None and len(rows) >= self.comment_limit):
                        break
                    if self.platform == "douyin":
                        self._scroll_douyin(page)
                    else:
                        page.mouse.wheel(0, 1_200)
                    page.wait_for_timeout(self.scroll_wait_ms)
                limited_rows = _with_comment_keys(rows)
                if self.comment_limit is not None:
                    limited_rows = limited_rows[: self.comment_limit]
                return parse_comments(self.platform, post.post_key, limited_rows)
            finally:
                page.close()

    def _verify_douyin_post(self, page: Any, post: SocialPost) -> None:
        """Do not collect a recommended video after Douyin redirects a stale URL."""
        current_url = str(getattr(page, "url", "") or "")
        current_key = extract_post_key("douyin", current_url)
        if not current_key:
            current_key = str(parse_qs(urlsplit(current_url).query).get("modal_id", [""])[0] or "").strip()
        if current_key and current_key != post.post_key:
            raise SocialAccessError(
                "POST_REDIRECTED",
                f"抖音帖子 {post.post_key} 已跳转到 {current_key}，未采集跳转后的其他视频评论。",
            )
        if not current_key:
            raise SocialAccessError("POST_NOT_AVAILABLE", f"抖音帖子 {post.post_key} 未打开可识别的视频页面。")

    def _open_douyin_comments(self, page: Any) -> None:
        """Open the comment drawer when the page has not opened it by default."""
        selector = self._selector("comment_selectors", DEFAULT_COMMENT_SELECTORS["douyin"])
        clicked = False
        attempts = max(1, min(20, self.timeout_ms // 500))
        for _ in range(attempts):
            if page.locator(selector).count() > 0:
                return
            controls = page.locator("[data-e2e='feed-comment-icon']")
            for index in range(controls.count()):
                control = controls.nth(index)
                try:
                    if control.is_visible():
                        if not clicked:
                            control.click(timeout=1_500)
                            clicked = True
                        break
                except Exception:
                    continue
            page.wait_for_timeout(500)
        raise SocialAccessError("COMMENTS_NOT_AVAILABLE", "抖音视频页面未找到评论入口。")

    def _load_more_douyin_comments(self, page: Any) -> int:
        """Click the visible comment pagination control before scrolling."""
        clicked = 0
        controls = page.locator("button")
        for index in range(controls.count()):
            control = controls.nth(index)
            try:
                label = control.inner_text() or ""
                if control.is_visible() and re.search(r"^\s*(点击)?加载更多(?:评论)?\s*$", label):
                    control.click(timeout=1_500)
                    page.wait_for_timeout(min(self.scroll_wait_ms, 1_000))
                    clicked += 1
                    break
            except Exception:
                continue
        return clicked

    def _expand_xhs_replies(self, page: Any) -> int:
        """Expand every currently visible XHS reply group before extraction.

        XHS renders only top-level comments initially. Reply groups are loaded
        after clicking a `.show-more` node, and newly loaded groups can expose
        another expansion control. Re-query the DOM after every click so Vue's
        list replacement does not invalidate a previously captured locator.
        """
        clicked = 0
        failed_attempts = 0
        while self.comment_limit is None or clicked < self.comment_limit:
            controls = page.locator(".show-more")
            count = controls.count()
            target = None
            for index in range(count):
                candidate = controls.nth(index)
                try:
                    if candidate.is_visible():
                        target = candidate
                        break
                except Exception:
                    continue
            if target is None:
                break
            try:
                target.click(timeout=1_500)
                page.wait_for_timeout(min(self.scroll_wait_ms, 1_000))
                clicked += 1
                failed_attempts = 0
            except Exception:
                # A control can disappear while Vue replaces the reply list;
                # re-querying on the next iteration is safer than aborting the
                # entire comment snapshot.
                failed_attempts += 1
                if failed_attempts >= 3:
                    break
        return clicked

    def _expand_douyin_replies(self, page: Any) -> int:
        """Expand visible Douyin reply groups before taking the snapshot."""
        clicked = 0
        failed_attempts = 0
        while self.reply_expand_limit is None or clicked < self.reply_expand_limit:
            controls = page.locator("button")
            count = controls.count()
            target = None
            for index in range(count):
                candidate = controls.nth(index)
                try:
                    if candidate.is_visible() and re.search(r"展开\s*\d+\s*条回复", candidate.inner_text() or ""):
                        target = candidate
                        break
                except Exception:
                    continue
            if target is None:
                break
            try:
                target.click(timeout=1_500)
                page.wait_for_timeout(min(self.scroll_wait_ms, 1_000))
                clicked += 1
                failed_attempts = 0
            except Exception:
                failed_attempts += 1
                if failed_attempts >= 3:
                    break
        return clicked

    def _scroll_douyin(self, page: Any) -> None:
        """Scroll Douyin's route container instead of relying on mouse focus."""
        scroller = page.locator("[class*='route-scroll-container']")
        try:
            if scroller.count() > 0:
                scroller.first.evaluate("(el, amount) => el.scrollBy(0, amount)", 1_200)
                return
        except Exception:
            pass
        page.mouse.wheel(0, 1_200)

    def is_authenticated(self, context: Any, page: Any) -> bool:
        current_url = str(getattr(page, "url", "") or "").lower()
        if any(token in current_url for token in ("login", "passport", "account")):
            return False
        try:
            body = str(page.locator("body").inner_text(timeout=1_000) or "")[:20_000]
        except Exception:
            body = ""
        if any(token in body for token in ("扫码登录", "手机号登录", "立即登录")) and "退出登录" not in body:
            return False
        cookies = {str(item.get("name")) for item in context.cookies() if item.get("name")}
        if self.platform == "xiaohongshu":
            return bool(cookies.intersection({"a1", "web_session", "webId"})) or "退出登录" in body
        return bool(cookies.intersection({"sessionid", "sessionid_ss", "passport_csrf_token"})) or "退出登录" in body

    def probe_login_status(self) -> dict[str, str]:
        """Probe the existing CDP Profile without opening a login session."""
        with self.connected() as (_playwright, _browser, context):
            existing_pages = list(context.pages)
            created_page = not existing_pages
            page = existing_pages[0] if existing_pages else context.new_page()
            try:
                authenticated = self.is_authenticated(context, page)
                return {
                    "platform": self.platform,
                    "profile_name": self.profile_name,
                    "status": "authenticated" if authenticated else "waiting_login",
                    "message": (
                        "已检测到登录会话。"
                        if authenticated
                        else "未检测到有效登录会话，请在专用 Chrome 中完成登录。"
                    ),
                }
            finally:
                if created_page:
                    page.close()

    def _discover_xiaohongshu(self, creator: SocialCreator, page: Any, rows: list[dict[str, Any]]) -> list[SocialPost]:
        today = datetime.now(SHANGHAI_TZ).date()
        selected: list[SocialPost] = []
        for row in rows[: self.post_limit]:
            post = self._post_from_row(creator, row)
            if post is None:
                continue
            published_at = post.published_at or self._probe_xhs_date(page, post)
            if published_at is None:
                # Do not write unverifiable content. Keep the scan bounded so a
                # provider layout change cannot turn one daily job into a full crawl.
                continue
            post = replace(post, published_at=published_at)
            published_date = published_at.astimezone(SHANGHAI_TZ).date()
            if published_date < today and not row.get("pinned") and "置顶" not in str(row.get("text") or ""):
                break
            if published_date == today:
                selected.append(post)
        return selected

    def _discover_douyin(self, creator: SocialCreator, _page: Any, rows: list[dict[str, Any]]) -> list[SocialPost]:
        today = datetime.now(SHANGHAI_TZ).date()
        selected: list[SocialPost] = []
        for row in rows[: self.post_limit]:
            post = self._post_from_row(creator, row)
            if post is None or post.published_at is None:
                continue
            if post.published_at.astimezone(SHANGHAI_TZ).date() == today:
                selected.append(post)
        return selected

    def _post_rows(self, page: Any) -> list[dict[str, Any]]:
        embedded = self._embedded_post_rows(page)
        if self.platform == "xiaohongshu":
            embedded = [row for row in embedded if "xsec_token=" in str(row.get("href") or "")]
        selector = self._selector("post_link_selectors", DEFAULT_POST_SELECTORS[self.platform])
        rows = page.locator(selector).evaluate_all(
            """els => els.map(el => {
              const card = el.closest('article, li, [data-e2e], [class*="note"], [class*="video"]') || el;
              const tokenLink = Array.from(card.querySelectorAll('a')).find(link =>
                /xsec_token=/.test(link.href || '') && new RegExp('user/profile/[^/]+/[^/]+').test(link.pathname || '')
              );
              const href = tokenLink ? tokenLink.href : (el.href || el.getAttribute('href') || '');
              return {
                href,
                text: card.innerText || el.innerText || el.textContent || '',
                aria_label: el.getAttribute('aria-label') || '',
                published_at: el.getAttribute('data-publish-time') || el.getAttribute('data-create-time') || el.getAttribute('datetime') || '',
                pinned: /置顶|pinned/i.test(card.innerText || '')
              };
            })"""
        )
        result: list[dict[str, Any]] = [row for row in embedded if isinstance(row, dict)]
        seen: set[str] = set()
        for row in result:
            key = extract_post_key(self.platform, str(row.get("href") or ""))
            if key:
                seen.add(key)
        for row in rows if isinstance(rows, list) else []:
            if not isinstance(row, dict):
                continue
            key = extract_post_key(self.platform, str(row.get("href") or ""))
            if key and key not in seen:
                seen.add(key)
                result.append(row)
        return result

    def _embedded_post_rows(self, page: Any) -> list[dict[str, Any]]:
        """Read platform state embedded in the already-authorized page.

        This is intentionally page-local: it does not call private HTTP APIs or
        bypass access controls. It gives Douyin its explicit create_time and
        lets Xiaohongshu retain xsec_token values when the DOM only exposes a
        shortened card link.
        """
        try:
            payload = page.evaluate(
                """() => {
                  const result = [];
                  const seen = new Set();
                  const visit = (value, depth) => {
                    if (!value || depth > 8 || result.length >= 300) return;
                    if (Array.isArray(value)) {
                      for (const item of value) visit(item, depth + 1);
                      return;
                    }
                    if (typeof value !== 'object') return;
                    const id = value.aweme_id || value.awemeId || value.note_id || value.noteId;
                    const token = value.xsec_token || value.xsecToken || '';
                    const createTime = value.create_time || value.createTime || value.publish_time || value.published_at;
                    if (id && (createTime || value.desc || value.title)) {
                      const key = String(id);
                      if (!seen.has(key)) {
                        seen.add(key);
                        result.push({
                          id: key,
                          url: value.url || value.note_url || value.share_url || '',
                          xsec_token: token,
                          title: value.desc || value.title || value.display_title || '',
                          create_time: createTime || '',
                          pinned: Boolean(value.is_top || value.isTop || value.pinned)
                        });
                      }
                    }
                    for (const child of Object.values(value)) visit(child, depth + 1);
                  };
                  for (const script of Array.from(document.scripts)) {
                    const text = script.textContent || '';
                    if (!text || text.length > 2000000) continue;
                    try { visit(JSON.parse(text), 0); } catch (_) {}
                  }
                  return result;
                }"""
            )
        except Exception:
            return []
        result: list[dict[str, Any]] = []
        for item in payload if isinstance(payload, list) else []:
            if not isinstance(item, dict):
                continue
            post_id = str(item.get("id") or "").strip()
            if not post_id:
                continue
            url = str(item.get("url") or "").strip()
            if self.platform == "xiaohongshu":
                if not url:
                    url = f"https://www.xiaohongshu.com/explore/{post_id}"
                token = str(item.get("xsec_token") or "").strip()
                if token and "xsec_token=" not in url:
                    separator = "&" if "?" in url else "?"
                    url = f"{url}{separator}xsec_token={token}&xsec_source=pc_user"
            else:
                url = url or f"https://www.douyin.com/video/{post_id}"
            result.append(
                {
                    "href": url,
                    "text": item.get("title") or "",
                    "published_at": item.get("create_time") or "",
                    "pinned": bool(item.get("pinned")),
                }
            )
        return result

    def _post_from_row(self, creator: SocialCreator, row: dict[str, Any]) -> SocialPost | None:
        url = str(row.get("href") or "").strip()
        if self.platform == "xiaohongshu":
            url = _carry_xhs_token(url, creator.profile_url)
        key = extract_post_key(self.platform, url)
        if not key or not url:
            return None
        return SocialPost(
            platform=self.platform,
            post_key=key,
            url=url,
            title=_post_title(row.get("text") or row.get("aria_label")),
            published_at=parse_datetime_value(row.get("published_at")) or _parse_social_date(row.get("text")),
            raw_payload={"creator_key": creator.creator_key, "source": row},
        )

    def _probe_xhs_date(self, page: Any, post: SocialPost) -> datetime | None:
        try:
            page.goto(post.url, wait_until="domcontentloaded", timeout=self.timeout_ms)
            page.wait_for_timeout(self.wait_ms)
            structured = page.locator("script[type='application/ld+json']").evaluate_all(
                """els => els.map(el => el.textContent || '')"""
            )
            for raw in structured if isinstance(structured, list) else []:
                try:
                    payload = json.loads(str(raw))
                except (TypeError, ValueError):
                    continue
                if isinstance(payload, dict) and payload.get("datePublished"):
                    published_at = parse_datetime_value(payload["datePublished"])
                    if published_at is not None:
                        return published_at
            body = str(page.locator("body").inner_text(timeout=1_000) or "")[:30_000]
        except Exception:
            return None
        return _parse_social_date(body, title=post.title)

    def _goto(self, page: Any, url: str) -> None:
        try:
            page.goto(url, wait_until="domcontentloaded", timeout=self.timeout_ms)
            page.wait_for_timeout(self.wait_ms)
        except Exception as exc:
            raise SocialAccessError("PAGE_LOAD_FAILED", f"无法加载社媒页面：{url}") from exc
        current_url = str(getattr(page, "url", "") or "")
        try:
            body = str(page.locator("body").inner_text(timeout=1_000) or "")[:10_000]
        except Exception:
            body = ""
        if any(token in current_url.lower() for token in ("login", "passport", "account")):
            raise SocialAccessError("LOGIN_REQUIRED", f"{self.platform} 专用 Chrome 尚未登录")
        if self.platform == "xiaohongshu" and ("300012" in current_url or "IP存在风险" in body):
            raise SocialAccessError("PLATFORM_BLOCKED", "小红书返回 300012（当前网络环境被平台拒绝）")

    def _selector(self, key: str, default: str) -> str:
        values = self._configured.get(key) or {}
        return str(values.get(self.platform) or default) if isinstance(values, dict) else default

    def _resolve_port(self, configured: dict[str, Any]) -> int:
        env_name = f"CYNEXUS_SOCIAL_CDP_PORT_{self.platform.upper()}"
        value = os.getenv(env_name) or configured.get("cdp_port")
        if value in (None, ""):
            value = {"xiaohongshu": 19221, "douyin": 19222}[self.platform]
        return _bounded_int(value, 19221, 1_024, 65_535)


def _with_comment_keys(rows: list[dict[str, Any]]) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    for row in rows:
        content = _optional_text(row.get("content")) or ""
        if not content:
            continue
        if not str(row.get("id") or "").strip():
            identity = "|".join((content, str(row.get("author_id") or ""), str(row.get("parent_id") or "")))
            row = {**row, "id": hashlib.sha256(identity.encode("utf-8")).hexdigest()}
        result.append(row)
    return result


def _merge_comment_rows(existing: list[dict[str, Any]], current: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Merge visible batches without duplicating rows across scroll rounds."""
    result = list(existing)
    seen = {_comment_row_identity(row) for row in result}
    for row in current:
        if not isinstance(row, dict):
            continue
        identity = _comment_row_identity(row)
        if identity in seen:
            continue
        seen.add(identity)
        result.append(row)
    return result


def _comment_row_identity(row: dict[str, Any]) -> str:
    provider_id = str(row.get("id") or "").strip()
    if provider_id:
        return f"id:{provider_id}"
    value = "|".join(
        (
            str(row.get("content") or "").strip(),
            str(row.get("author_id") or "").strip(),
            str(row.get("parent_id") or "").strip(),
        )
    )
    return f"fingerprint:{hashlib.sha256(value.encode('utf-8')).hexdigest()}"


_XHS_COMMENT_ROWS_SCRIPT = """
els => els.map((el, index) => {
  const text = (node) => node ? (node.innerText || node.textContent || '').trim() : '';
  const attr = (node, name) => node ? (node.getAttribute(name) || '') : '';
  const parent = el.closest('.parent-comment');
  const ancestorItem = el.parentElement?.closest('.comment-item');
  const rootItem = parent?.querySelector('.comment-item:not(.comment-item-sub)');
  const replyText = text(parent).match(/(\\d+)\\s*条回复/);
  const content = el.querySelector('.content .note-text, .content');
  const date = el.querySelector('.info .date, .date, time');
  const like = el.querySelector('.interactions .like .count, .like .count');
  const reply = el.querySelector('.interactions .reply .count, .reply .count');
  const author = el.querySelector('.author .name, .author-wrapper .name');
  return {
    id: el.getAttribute('data-comment-id') || el.getAttribute('data-id') || el.id || '',
    parent_id: el.classList.contains('comment-item-sub')
      ? (attr(el, 'data-parent-id') || ancestorItem?.id || rootItem?.id || '')
      : '',
    content: text(content),
    published_at: text(date),
    like_count: text(like),
    reply_count: replyText ? replyText[1] : text(reply),
    author_id: attr(author, 'data-user-id') || attr(el, 'data-user-id'),
    author_name: text(author),
    index
  };
})
"""


_DOUYIN_COMMENT_ROWS_SCRIPT = """
els => els.map((el, index) => {
  const text = (node) => node ? (node.innerText || node.textContent || '').trim() : '';
  const attr = (node, name) => node ? (node.getAttribute(name) || '') : '';
  // Current Douyin comment-item structure is:
  // item > content-wrapper > [author, body, date, stats]. The generated CSS
  // classes are not stable, while the item boundary is exposed via data-e2e.
  const body = el.children?.[1]?.firstElementChild;
  const content = body?.children?.[1];
  const date = body?.children?.[2];
  const stats = body?.children?.[3];
  const replyContainer = el.children?.[1]?.children?.[1];
  const author = body?.querySelector('a[href*="/user/"]');
  const like = stats?.querySelector('p:first-child span');
  const reply = replyContainer?.querySelector(':scope > button');
  const parent = el.parentElement?.closest('[data-e2e="comment-item"]');
  return {
    id: el.getAttribute('data-comment-id') || el.getAttribute('data-id') || el.id || '',
    parent_id: el.getAttribute('data-parent-id') || (parent ? (parent.getAttribute('data-comment-id') || parent.getAttribute('data-id') || parent.id || '') : ''),
    content: text(content),
    published_at: text(date.querySelector?.('time') || date),
    like_count: attr(el, 'data-like-count') || text(like),
    reply_count: attr(el, 'data-reply-count') || (reply ? (text(reply).match(/(\\d+)/) || [])[1] || '' : ''),
    author_id: attr(el, 'data-author-id') || attr(el, 'data-user-id') || attr(author, 'href'),
    author_name: text(author),
    index
  };
})
"""


def _carry_xhs_token(note_url: str, profile_url: str) -> str:
    if not note_url or "xsec_token=" in note_url or "xsec_token=" not in profile_url:
        return note_url
    source = urlsplit(profile_url)
    target = urlsplit(note_url)
    source_query = parse_qs(source.query)
    token = source_query.get("xsec_token", [""])[0]
    if not token:
        return note_url
    query = parse_qs(target.query)
    query["xsec_token"] = [token]
    query.setdefault("xsec_source", source_query.get("xsec_source", ["pc_user"])[0])
    return urlunsplit((target.scheme, target.netloc, target.path, urlencode(query, doseq=True), target.fragment))


def _parse_social_date(value: Any, *, title: str | None = None) -> datetime | None:
    text = str(value or "")
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    if title:
        try:
            start = len(lines) - 1 - lines[::-1].index(title) + 1
            lines = lines[start : start + 20]
        except ValueError:
            pass
    now = datetime.now(SHANGHAI_TZ)
    for line in lines[:30]:
        if line == "今天" or line.startswith("今天 "):
            return datetime.combine(now.date(), time.min, tzinfo=SHANGHAI_TZ)
        if line == "昨天" or line.startswith("昨天 "):
            return datetime.combine(now.date() - timedelta(days=1), time.min, tzinfo=SHANGHAI_TZ)
        match = re.search(r"(?<!\d)(\d{4})[-/]?(\d{1,2})[-/]?(\d{1,2})(?:日)?", line)
        if match:
            year, month, day = (int(item) for item in match.groups())
        else:
            match = re.search(r"(?<!\d)(\d{1,2})[-月/]?(\d{1,2})(?:日)?", line)
            if not match:
                continue
            year = now.year
            month, day = (int(item) for item in match.groups())
            try:
                candidate = date(year, month, day)
            except ValueError:
                continue
            if candidate > now.date() + timedelta(days=1):
                year -= 1
        try:
            return datetime.combine(date(year, month, day), time.min, tzinfo=SHANGHAI_TZ)
        except ValueError:
            continue
    return None


def _optional_text(value: Any) -> str | None:
    if value in (None, ""):
        return None
    return " ".join(str(value).split()) or None


def _post_title(value: Any) -> str | None:
    if value in (None, ""):
        return None
    lines = [line.strip() for line in str(value).splitlines() if line.strip()]
    if lines and lines[0] in {"置顶", "Pinned"}:
        lines = lines[1:]
    return lines[0] if lines else _optional_text(value)


def _bounded_int(value: Any, default: int, minimum: int, maximum: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        parsed = default
    return max(minimum, min(maximum, parsed))


def _optional_bounded_int(value: Any, minimum: int, maximum: int) -> int | None:
    if value in (None, ""):
        return None
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        return None
    if parsed <= 0:
        return None
    return max(minimum, min(maximum, parsed))


def _timeout_ms(value: Any, default: int) -> int:
    try:
        seconds = float(value)
    except (TypeError, ValueError):
        return default
    return max(1_000, min(120_000, int(seconds * 1_000)))
