from __future__ import annotations

from datetime import datetime

from cynexus_data.connections.models import Connection
from cynexus_data.sources.social.cdp import (
    CdpSocialClient,
    DEFAULT_COMMENT_SELECTORS,
    _merge_comment_rows,
    _parse_social_date,
    _with_comment_keys,
)
from cynexus_data.sources.social.models import SocialCreator, SocialPost


def _client(platform: str) -> CdpSocialClient:
    return CdpSocialClient(platform, Connection(conn_id="social_browser", type="browser"))


def test_cdp_defaults_to_dedicated_platform_ports() -> None:
    assert _client("xiaohongshu").cdp_port == 19221
    assert _client("douyin").cdp_port == 19222


def test_douyin_comments_use_comment_item_boundaries() -> None:
    assert DEFAULT_COMMENT_SELECTORS["douyin"] == "[data-e2e='comment-item']"


def test_comment_collection_has_no_default_limit(monkeypatch) -> None:
    monkeypatch.delenv("CYNEXUS_SOCIAL_CDP_COMMENT_LIMIT", raising=False)
    monkeypatch.delenv("CYNEXUS_SOCIAL_CDP_COMMENT_SCROLL_ROUNDS", raising=False)
    monkeypatch.delenv("CYNEXUS_SOCIAL_DOUYIN_REPLY_EXPAND_LIMIT", raising=False)
    client = _client("douyin")
    assert client.comment_limit is None
    assert client.comment_scroll_rounds is None
    assert client.reply_expand_limit is None


def test_douyin_post_verification_rejects_redirected_modal() -> None:
    client = _client("douyin")

    class FakePage:
        url = "https://www.douyin.com/jingxuan?modal_id=other-post"

    try:
        client._verify_douyin_post(  # noqa: SLF001 - focused redirect contract test
            FakePage(),
            SocialPost(platform="douyin", post_key="expected-post", url="https://www.douyin.com/video/expected-post"),
        )
    except Exception as exc:
        assert getattr(exc, "code", None) == "POST_REDIRECTED"
    else:
        raise AssertionError("redirected Douyin post should not be accepted")


def test_xhs_date_parser_handles_today_yesterday_and_month_day() -> None:
    assert _parse_social_date("今天 10:30").date() == datetime.now().date()
    assert _parse_social_date("昨天 23:00") is not None
    assert _parse_social_date("08-05 河南").month == 8


def test_comment_rows_without_provider_id_get_stable_keys() -> None:
    rows = [{"content": "有用", "author_id": "u1", "parent_id": ""}]
    first = _with_comment_keys(rows)
    second = _with_comment_keys(rows)
    assert first[0]["id"] == second[0]["id"]


def test_comment_rows_merge_by_provider_id_across_scroll_rounds() -> None:
    merged = _merge_comment_rows(
        [{"id": "comment-001", "content": "第一版"}],
        [
            {"id": "comment-001", "content": "第一版"},
            {"id": "comment-002", "content": "第二条"},
        ],
    )

    assert [row["id"] for row in merged] == ["comment-001", "comment-002"]


def test_xhs_dom_note_url_carries_profile_xsec_token() -> None:
    client = _client("xiaohongshu")
    post = client._post_from_row(  # noqa: SLF001 - focused adapter contract test
        SocialCreator(
            platform="xiaohongshu",
            creator_key="creator-001",
            profile_url="https://www.xiaohongshu.com/user/profile/u1?xsec_token=token-001&xsec_source=pc_search",
        ),
        {"href": "https://www.xiaohongshu.com/explore/note-001", "text": "标题"},
    )
    assert post is not None
    assert "xsec_token=token-001" in post.url


def test_xhs_reply_expander_requeries_replaced_controls() -> None:
    client = _client("xiaohongshu")

    class FakeControl:
        def __init__(self, controls: list[object]) -> None:
            self.controls = controls

        def is_visible(self) -> bool:
            return True

        def click(self, timeout: int) -> None:
            assert timeout == 1_500
            self.controls.pop(0)

    class FakeLocator:
        def __init__(self, controls: list[object]) -> None:
            self.controls = controls

        def count(self) -> int:
            return len(self.controls)

        def nth(self, index: int) -> FakeControl:
            assert index == 0
            return FakeControl(self.controls)

    class FakePage:
        def __init__(self) -> None:
            self.controls = [object(), object(), object()]

        def locator(self, selector: str) -> FakeLocator:
            assert selector == ".show-more"
            return FakeLocator(self.controls)

        def wait_for_timeout(self, _milliseconds: int) -> None:
            return None

    page = FakePage()

    assert client._expand_xhs_replies(page) == 3  # noqa: SLF001 - focused DOM interaction test
    assert page.controls == []
