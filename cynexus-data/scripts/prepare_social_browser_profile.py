from __future__ import annotations

import argparse
import os
from pathlib import Path


PLATFORM_HOME_URLS = {
    "xiaohongshu": "https://www.xiaohongshu.com/",
    "douyin": "https://www.douyin.com/",
}


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Open a headed Playwright browser for one-time social-platform login."
    )
    parser.add_argument("--platform", choices=sorted(PLATFORM_HOME_URLS), required=True)
    parser.add_argument("--profile-name", default="default")
    args = parser.parse_args()

    try:
        from playwright.sync_api import sync_playwright
    except ImportError as exc:
        raise SystemExit(
            "Playwright is not installed. Install the live dependencies first: "
            "pip install -r requirements-live.txt"
        ) from exc

    project_root = Path(__file__).resolve().parents[1]
    configured_root = os.getenv("CYNEXUS_SOCIAL_PROFILE_ROOT", "var")
    profile_root = Path(configured_root).expanduser()
    if not profile_root.is_absolute():
        profile_root = project_root / profile_root
    profile_dir = profile_root / "social-browser-profiles" / args.platform / args.profile_name
    profile_dir.mkdir(parents=True, exist_ok=True)

    executable_path = os.getenv("CYNEXUS_PLAYWRIGHT_EXECUTABLE_PATH", "").strip()
    launch_kwargs = {
        "headless": False,
        "locale": "zh-CN",
        "viewport": {"width": 412, "height": 915},
        "args": ["--disable-dev-shm-usage"],
    }
    if executable_path and Path(executable_path).exists():
        launch_kwargs["executable_path"] = executable_path

    print(f"Profile directory: {profile_dir}")
    print("请在打开的浏览器中完成登录；登录成功后回到此窗口按 Enter 保存。")
    with sync_playwright() as playwright:
        context = playwright.chromium.launch_persistent_context(str(profile_dir), **launch_kwargs)
        try:
            page = context.pages[0] if context.pages else context.new_page()
            page.goto(PLATFORM_HOME_URLS[args.platform], wait_until="domcontentloaded", timeout=60_000)
            input("登录完成后按 Enter 关闭浏览器并保存 Profile：")
        finally:
            context.close()

    print(f"Profile 已保存：{profile_dir}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
