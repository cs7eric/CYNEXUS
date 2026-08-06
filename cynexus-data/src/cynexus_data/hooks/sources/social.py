from __future__ import annotations

import os

from cynexus_data.hooks.base import BaseHook
from cynexus_data.sources.social.browser import SocialBrowserClient
from cynexus_data.sources.social.cdp import CdpSocialClient
from cynexus_data.sources.social.opencli import OpenCliSocialClient


class SocialBrowserHook(BaseHook):
    def get_client(self, platform: str, profile_name: str = "default") -> SocialBrowserClient | CdpSocialClient | OpenCliSocialClient:
        normalized = str(platform).strip().lower()
        if normalized not in {"douyin", "xiaohongshu"}:
            raise ValueError(f"unsupported social platform: {platform}")
        backend = os.getenv("CYNEXUS_SOCIAL_BACKEND", "cdp").strip().lower()
        if backend == "opencli":
            return OpenCliSocialClient(normalized, self.connection, profile_name=profile_name)
        if backend == "playwright":
            return SocialBrowserClient(normalized, self.connection, profile_name=profile_name)
        return CdpSocialClient(normalized, self.connection, profile_name=profile_name)
