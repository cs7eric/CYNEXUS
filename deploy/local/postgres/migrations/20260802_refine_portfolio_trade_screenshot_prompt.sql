\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000307,
    'portfolio_trade_screenshot_parse',
    'Portfolio trade screenshot parsing',
    'portfolio_trade_screenshot_parse',
    'v2',
    'active',
    '你是 Cynexus 个人持仓交易截图解析器。你需要识别券商、银行或交易软件中的成交记录，并提取图片中明确可见的成交事实。\n'
    '支持沪深北 A 股股票、交易所上市 ETF、LOF 和基金份额；它们都属于可导入的证券成交。以下情况都属于交易截图：标题或标签包含历史成交、成交明细、成交记录、当日成交、交易记录、委托成交，或图片中出现多行证券名称/代码、成交日期或时间、买入/卖出、成交价、成交量/数量、成交额/金额等组合。A 股券商常用红色表示买入、蓝色表示卖出，也必须按文字“买入/卖出”或行内买卖标签判断。\n'
    '只要图片中存在一行或多行可辨认的股票或 ETF 成交记录，isTradeScreenshot 必须为 true；证券代码缺失、只有证券名称、名称包含 ETF、日期只有部分、费用未展示，都不能把图片判为非交易截图。只有完全没有成交记录证据（例如行情图、新闻、资产总览或无关图片）时，isTradeScreenshot 才为 false。\n'
    '一张图片可能包含多笔交易，必须拆分为 candidates 数组。将买入、买、买进映射为 BUY，将卖出、卖、卖出映射为 SELL。证券代码、成交日期、成交价格、成交数量任一不可辨认时返回 null 并在该 candidate 的 warnings 中说明；不要猜测、补全或计算持仓收益。证券名称、成交时间、成交额、费用、成交编号只有在图片明确出现时填写，否则为 null。每个字段提供 confidence 和原始 evidence 文本。\n'
    '只输出合法 JSON，不要 Markdown、思考过程或额外文字。',
    '请解析当前上传的股票或 ETF 交易截图。优先识别每一行的证券名称、证券代码、买卖方向、成交日期/时间、成交价格、成交数量、成交金额和费用；字段不可见时使用 null，但不要因此否定截图是交易截图。',
    '{"type":"object","required":["isTradeScreenshot","candidates","warnings"],"properties":{"isTradeScreenshot":{"type":"boolean"},"candidates":{"type":"array","items":{"type":"object","required":["rowNo","symbol","securityName","side","tradedAt","quantity","price","fee","externalTradeNo","confidence","evidence","warnings"],"properties":{"rowNo":{"type":"integer"},"symbol":{"type":["string","null"]},"securityName":{"type":["string","null"]},"side":{"type":["string","null"],"enum":["BUY","SELL",null]},"tradedAt":{"type":["string","null"]},"quantity":{"type":["string","null"]},"price":{"type":["string","null"]},"fee":{"type":["string","null"]},"externalTradeNo":{"type":["string","null"]},"confidence":{"type":"number"},"evidence":{"type":"object"},"warnings":{"type":"array","items":{"type":"string"}}}}},"warnings":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '[]'::jsonb,
    '["market","portfolio","vision","trade-import","json-only","broker-history"]'::jsonb,
    '{"capability":"portfolio_trade_screenshot_parse","input":"one image","scope":"A-share stocks, exchange-traded funds and CNY trades","classification":"broker trade-history rows are positive evidence"}'::jsonb,
    'AI 只产生候选交易，用户确认后才进入 Portfolio 账本。证券代码缺失或 ETF 名称不应阻止交易截图识别。'
)
ON CONFLICT (prompt_key, prompt_version) WHERE deleted_at IS NULL DO UPDATE SET
    name = EXCLUDED.name,
    scenario_code = EXCLUDED.scenario_code,
    status = EXCLUDED.status,
    system_prompt = EXCLUDED.system_prompt,
    user_prompt_template = EXCLUDED.user_prompt_template,
    output_schema = EXCLUDED.output_schema,
    variables = EXCLUDED.variables,
    tags = EXCLUDED.tags,
    extra = EXCLUDED.extra,
    remark = EXCLUDED.remark,
    updated_at = now(),
    deleted_at = NULL;

UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v2"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v2'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
