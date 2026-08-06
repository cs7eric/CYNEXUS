package cn.cccs7.cynexus.ai.infra.agent.market;

final class FuturesPositionAiAgentToolGuidance {

    static final String ANALYSIS = """
            {
              "purpose": "Analyze the current and historical member-position structure for IF, IH, IM, or IC.",
              "useWhen": ["The user asks why an index-futures position structure changed.", "The user needs a grounded summary of net position, persistence, and member attribution."],
              "avoidWhen": ["The user asks for a buy or sell signal.", "The variety is not IF, IH, IM, or IC."],
              "parameterNotes": {"varietyCode": "Required and restricted to IF, IH, IM, or IC. Provide current, history, and optional members from the futures position query."},
              "examples": [{"arguments": {"varietyCode": "IF", "current": {}, "history": []}, "note": "Analyze a bounded futures position snapshot."}],
              "resultNotes": "The result explains observed position facts only; preserve the data date and disclaimer."
            }
            """;

    private FuturesPositionAiAgentToolGuidance() {
    }
}
