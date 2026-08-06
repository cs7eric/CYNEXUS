from __future__ import annotations

from datetime import datetime, timezone

from cynexus_data.connections.models import Connection
from cynexus_data.sources.social.models import SocialPost
from cynexus_data.sources.social.opencli import OpenCliSocialClient, _extract_xhs_publish_date


def _client(platform: str) -> OpenCliSocialClient:
    return OpenCliSocialClient(platform, Connection(conn_id="social_browser", type="browser"))


def test_xhs_opencli_post_preserves_tokenized_url() -> None:
    client = _client("xiaohongshu")
    post = client._xhs_post(  # noqa: SLF001 - focused adapter contract test
        type("Creator", (), {"creator_key": "creator-001"})(),
        {
            "id": "note-001",
            "title": "标题",
            "url": "https://www.xiaohongshu.com/user/profile/u1/note-001?xsec_token=token-001&xsec_source=pc_user",
        },
    )

    assert post.post_key == "note-001"
    assert "xsec_token=token-001" in post.url


def test_xhs_opencli_comments_get_deterministic_keys(monkeypatch) -> None:
    client = _client("xiaohongshu")
    monkeypatch.setattr(
        client,
        "_run_json",
        lambda *_args: [{"author": "甲", "text": "有用", "time": "刚刚", "likes": 3}],
    )

    first = client.fetch_comments(
        SocialPost(
            platform="xiaohongshu",
            post_key="note-001",
            url="https://www.xiaohongshu.com/explore/note-001?xsec_token=token-001",
        )
    )
    second = client.fetch_comments(
        SocialPost(
            platform="xiaohongshu",
            post_key="note-001",
            url="https://www.xiaohongshu.com/explore/note-001?xsec_token=token-001",
        )
    )

    assert first[0].comment_key == second[0].comment_key
    assert first[0].like_count == 3


def test_opencli_browser_bridge_error_is_actionable() -> None:
    error = _client("xiaohongshu").command_error(
        "",
        "code: BROWSER_CONNECT\nmessage: Browser Bridge extension not connected",
        69,
    )

    assert error.code == "OPENCLI_BROWSER_NOT_CONNECTED"
    assert "Browser Bridge" in str(error)


def test_xhs_login_opens_public_site_and_uses_public_session_probe(monkeypatch) -> None:
    client = _client("xiaohongshu")
    calls: list[tuple[str, ...]] = []

    class _Process:
        pid = 1

        def poll(self):
            return None

    monkeypatch.setattr(client, "ensure_browser_bridge", lambda: None)
    monkeypatch.setattr(client, "_spawn", lambda *args: calls.append(args) or _Process())
    monkeypatch.setattr(client, "_run_json", lambda *args: calls.append(args) or [])

    profile_url = "https://www.xiaohongshu.com/user/profile/u1?xsec_token=token-001"
    process = client.start_login_process(profile_url)
    client.probe_login(profile_url)

    assert process.pid == 1
    assert calls[0][:2] == ("xiaohongshu", "user")
    assert calls[0][-2:] == ("--keep-tab", "true")
    assert calls[1][:2] == ("xiaohongshu", "user")
    assert client.login_requires_probe()


def test_xhs_publish_date_probe_ignores_date_like_title() -> None:
    published_at = _extract_xhs_publish_date(
        "今天吃了三个板\n爱狗人士233\n上蹿下跳\n这是猴市吗🐒\n07-14 河南\n共 48 条评论",
        "今天吃了三个板",
        datetime(2026, 8, 5, 12, 0, tzinfo=timezone.utc),
    )

    assert published_at is not None
    assert published_at.date().isoformat() == "2026-07-14"


def test_xhs_publish_date_probe_stops_at_first_historical_note(monkeypatch) -> None:
    client = _client("xiaohongshu")
    posts = [
        SocialPost(platform="xiaohongshu", post_key="today", url="today", title="today"),
        SocialPost(platform="xiaohongshu", post_key="yesterday", url="yesterday", title="yesterday"),
        SocialPost(platform="xiaohongshu", post_key="older", url="older", title="older"),
    ]
    bodies = iter(("today\n今天", "yesterday\n昨天"))
    eval_count = 0

    monkeypatch.setattr(client, "_opencli_browser_tab", lambda *_args: "tab-001")

    def run_browser(_session, command, *args):
        nonlocal eval_count
        if command == "eval":
            eval_count += 1
            return next(bodies)
        return ""

    monkeypatch.setattr(client, "_run_browser_text", run_browser)

    selected = client._probe_xhs_publish_dates(posts)  # noqa: SLF001 - focused boundary test

    assert [post.post_key for post in selected] == ["today"]
    assert eval_count == 2
