\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000309,
    'portfolio_trade_screenshot_parse',
    'Portfolio trade screenshot parsing',
    'portfolio_trade_screenshot_parse',
    'v4',
    'active',
    '你是 Cynexus 个人持仓交易截图解析器。只提取图片中明确可见的成交事实，不要猜测、补全或计算收益。支持沪深北 A 股股票、交易所上市 ETF、LOF 和基金份额。只要图片存在一行或多行可辨认的成交记录，isTradeScreenshot 必须为 true；只有完全没有成交记录证据时才返回 false。\n'
    '严格遵守输出格式：symbol、securityName、instrumentType、side、tradedAt、quantity、price、fee、externalTradeNo 这些字段必须直接是字符串或 null，绝对不能写成 {value,evidence,confidence} 对象。所有证据统一放在 candidate.evidence 对象中，warnings 放在 candidate.warnings 数组中。instrumentType 只能是 STOCK、ETF 或 null；side 只能是 BUY、SELL 或 null。将买入/买进映射为 BUY，将卖出映射为 SELL。优先识别证券代码、证券名称、成交日期/时间、成交价格和成交数量；字段不可见时使用 null，不要猜测。\n'
    '只输出合法 JSON，不要 Markdown、思考过程或额外文字。',
    '请解析当前上传的股票或 ETF 交易截图，并按截图中的每一行生成一个 candidate。tradedAt 使用 ISO 形式 YYYY-MM-DDTHH:mm:ss；只有日期时使用 YYYY-MM-DD。',
    '{"type":"object","required":["isTradeScreenshot","candidates","warnings"],"properties":{"isTradeScreenshot":{"type":"boolean"},"candidates":{"type":"array","items":{"type":"object","required":["rowNo","symbol","securityName","instrumentType","side","tradedAt","quantity","price","fee","externalTradeNo","confidence","evidence","warnings"],"properties":{"rowNo":{"type":"integer"},"symbol":{"type":["string","null"]},"securityName":{"type":["string","null"]},"instrumentType":{"type":["string","null"],"enum":["STOCK","ETF",null]},"side":{"type":["string","null"],"enum":["BUY","SELL",null]},"tradedAt":{"type":["string","null"]},"quantity":{"type":["string","null"]},"price":{"type":["string","null"]},"fee":{"type":["string","null"]},"externalTradeNo":{"type":["string","null"]},"confidence":{"type":"number"},"evidence":{"type":"object"},"warnings":{"type":"array","items":{"type":"string"}}}}},"warnings":{"type":"array","items":{"type":"string"}}}}'::jsonb,
    '[]'::jsonb,
    '["market","portfolio","vision","trade-import","json-only","broker-history","instrument-classification"]'::jsonb,
    '{"capability":"portfolio_trade_screenshot_parse","input":"one image","scope":"A-share stocks and exchange-traded funds in CNY","field_contract":"trade fields are scalar-or-null; evidence is candidate-level"}'::jsonb,
    'AI 只产生候选交易，用户确认后才进入 Portfolio 账本。兼容券商成交明细和 ETF 交易记录。'
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
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v4"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_key = 'portfolio_trade_screenshot_parse'
   AND prompt.prompt_version = 'v4'
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
