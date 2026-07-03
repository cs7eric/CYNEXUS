from cynexus_data.domain.transcription.fallbacks import answer_transcript_question, build_transcript_summary


def test_build_transcript_summary_emits_expected_sections() -> None:
    summary = build_transcript_summary(
        "指数今天高开之后震荡回落。午后资金回流到券商和科技方向。交易上更适合等回踩确认再跟进。",
        title="午盘复盘",
        platform="Video",
        duration=321,
    )
    assert "## 核心主题" in summary
    assert "## 核心观点" in summary
    assert "午盘复盘" in summary
    assert "321s" in summary


def test_answer_transcript_question_returns_matching_excerpt() -> None:
    answer = answer_transcript_question(
        "科技方向怎么走？",
        transcript="指数震荡。午后资金回流到科技方向，情绪明显修复。尾盘没有继续放量。",
        summary="",
    )
    assert "transcript fallback" in answer
    assert "科技方向" in answer
