from __future__ import annotations

import math
import re
from collections import Counter
from typing import Iterable


_SECTION_TITLES = (
    "核心主题",
    "核心观点",
    "关键知识点",
    "方法论框架",
    "可复用原则",
    "适用场景",
    "可执行清单",
    "风险提醒",
    "学习笔记",
)


def build_transcript_summary(transcript: str, *, title: str | None = None, platform: str | None = None, duration: float | None = None) -> str:
    cleaned = _normalize_text(transcript)
    if not cleaned:
        return ""
    sentences = _split_sentences(cleaned)
    highlights = _top_sentences(sentences, limit=5)
    keywords = _extract_keywords(cleaned, limit=6)
    duration_text = f"{int(math.floor(duration))}s" if duration and duration > 0 else "未知"
    header_title = title.strip() if title else "转写内容"
    lines: list[str] = []
    lines.extend(_section("核心主题", [f"{header_title} 的转写已完成，当前内容基于原始 transcript 自动整理。"]))
    lines.extend(_section("核心观点", highlights[:3] or sentences[:3]))
    lines.extend(_section("关键知识点", keywords or highlights[:4] or sentences[:4]))
    lines.extend(_section("方法论框架", ["先完成媒体转写，再按段阅读原文并提炼重点。"]))
    lines.extend(_section("可复用原则", ["优先回看原始 transcript 里的关键句，避免把自动整理结果当成最终结论。"]))
    lines.extend(_section("适用场景", [f"平台：{platform or '未知'}", f"时长：{duration_text}"]))
    lines.extend(_section("可执行清单", ["通读 transcript 并补充人工润色。", "基于关键句继续做主题拆分和后续问答。"]))
    lines.extend(_section("风险提醒", ["当前为 transcript-first fallback，总结与问答不是 AI 精修结果。"]))
    lines.extend(_section("学习笔记", [f"原始文本长度：{len(cleaned)} 字符。"]))
    return "\n".join(lines).strip()


def answer_transcript_question(question: str, *, transcript: str, summary: str | None = None) -> str:
    cleaned_question = _normalize_text(question)
    cleaned_transcript = _normalize_text(transcript)
    if not cleaned_question:
        return "问题为空，无法生成回答。"
    if not cleaned_transcript:
        return "当前任务还没有可用 transcript。"
    keywords = _extract_question_keywords(cleaned_question)
    candidates = _split_sentences(cleaned_transcript)
    scored = _score_sentences(candidates, keywords)
    if not scored:
        fallback = _split_sentences(_normalize_text(summary or ""))
        scored = _score_sentences(fallback, keywords) if fallback else []
    if not scored:
        scored = [(sentence, 1) for sentence in candidates[:3]]
    if not scored:
        return "当前只接入 transcript fallback，暂时没有可返回的片段。"
    top_hits = [sentence for sentence, _score in scored[:3]]
    answer_lines = [
        "当前回答来自 transcript fallback，不是 AI 深度问答。",
        "",
        "相关原文片段：",
        *[f"- {sentence}" for sentence in top_hits],
    ]
    return "\n".join(answer_lines).strip()


def _section(title: str, items: Iterable[str]) -> list[str]:
    lines = [f"## {title}"]
    normalized = [item.strip() for item in items if item and item.strip()]
    if not normalized:
        lines.append("- 暂无内容")
        return lines
    lines.extend(f"- {item}" for item in normalized)
    return lines


def _split_sentences(text: str) -> list[str]:
    raw_parts = re.split(r"(?<=[。！？!?；;])|\n+", text)
    return [part.strip(" \t\r\n-•*") for part in raw_parts if part and part.strip(" \t\r\n-•*")]


def _extract_keywords(text: str, *, limit: int) -> list[str]:
    tokens = _tokenize(text)
    counts = Counter(token for token in tokens if len(token) >= 2 and token not in _STOP_WORDS)
    return [token for token, _count in counts.most_common(limit)]


def _extract_question_keywords(text: str) -> list[str]:
    expanded: list[str] = []
    for token in _tokenize(text):
        if len(token) < 2 or token in _STOP_WORDS:
            continue
        expanded.append(token)
        if re.fullmatch(r"[\u4e00-\u9fff]+", token):
            if len(token) >= 4:
                expanded.append(token[:4])
            if len(token) >= 3:
                expanded.extend(token[index : index + 2] for index in range(len(token) - 1))
    deduped: list[str] = []
    for token in expanded:
        if token not in deduped and token not in _STOP_WORDS:
            deduped.append(token)
    return deduped[:12]


def _score_sentences(sentences: list[str], keywords: list[str]) -> list[tuple[str, int]]:
    if not sentences:
        return []
    if not keywords:
        return [(sentence, 1) for sentence in sentences[:3]]
    scored: list[tuple[str, int]] = []
    for sentence in sentences:
        score = 0
        for keyword in keywords:
            if keyword in sentence:
                score += max(1, len(keyword))
        if score > 0:
            scored.append((sentence, score))
    scored.sort(key=lambda item: (-item[1], len(item[0])))
    return scored


def _top_sentences(sentences: list[str], *, limit: int) -> list[str]:
    if len(sentences) <= limit:
        return sentences
    return sentences[:limit]


def _tokenize(text: str) -> list[str]:
    return re.findall(r"[\u4e00-\u9fff]{2,}|[A-Za-z0-9_]{2,}", text)


def _normalize_text(text: str) -> str:
    return re.sub(r"\s+", " ", str(text or "")).strip()


_STOP_WORDS = {
    "这个",
    "那个",
    "然后",
    "就是",
    "我们",
    "你们",
    "他们",
    "自己",
    "当前",
    "已经",
    "因为",
    "所以",
    "如果",
    "还是",
    "以及",
    "一个",
    "没有",
    "可以",
    "进行",
    "需要",
    "问题",
    "内容",
    "什么",
    "如何",
    "为什么",
    "which",
    "what",
    "when",
    "where",
    "that",
    "this",
    "with",
    "from",
    "have",
}
