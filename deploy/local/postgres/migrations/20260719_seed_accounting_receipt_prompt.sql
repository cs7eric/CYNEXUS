\set ON_ERROR_STOP on

BEGIN;

INSERT INTO cynexus_appl_ai.ai_prompt (
    id, prompt_key, name, scenario_code, prompt_version, status,
    system_prompt, user_prompt_template, output_schema, variables, tags, extra, remark
) VALUES (
    910003000000000201,
    'accounting_receipt_parse',
    'Accounting Receipt Recognition',
    'accounting_receipt_parse',
    'v1',
    'active',
    '你是记账单据识别助手。只从用户上传的图片中提取可见事实，不要猜测；金额必须是正数，无法确认的字段使用 null。\n一张图片可能包含多笔消费，必须拆成多条 items；多张图片中的重复小票只保留一次。transactionType 只能是 expense 或 income。\n用户输入会提供当前可用账户和分类候选。必须优先根据小票、支付方式、商户、备注等可见信息从候选中选择 accountId 和 categoryId，不得编造候选之外的 ID；无法可靠判断时返回 null，并填写 accountHint 或 categoryHint 供后端处理。\n只输出合法 JSON，不要 Markdown、代码围栏或解释。',
    '当前可用账户与分类候选：\n{{candidate_context}}\n\n严格输出：{"items":[{"imageIndex":0,"transactionType":"expense","occurredAt":"2026-01-01T12:00:00+08:00","amount":12.50,"currency":"CNY","merchant":"","accountId":null,"accountHint":"","categoryId":null,"categoryHint":"餐饮","note":"","installmentCount":null,"installmentFee":null,"confidence":0.8,"rawText":""}]}',
    '{"type":"object","required":["items"],"properties":{"items":{"type":"array","items":{"type":"object","required":["imageIndex","transactionType","amount","currency","merchant","accountId","categoryId","confidence"],"properties":{"imageIndex":{"type":"integer"},"transactionType":{"enum":["expense","income"]},"occurredAt":{"type":["string","null"]},"amount":{"type":["number","null"]},"currency":{"type":"string"},"merchant":{"type":["string","null"]},"accountId":{"type":["integer","null"]},"accountHint":{"type":["string","null"]},"categoryId":{"type":["integer","null"]},"categoryHint":{"type":["string","null"]},"note":{"type":["string","null"]},"installmentCount":{"type":["integer","null"]},"installmentFee":{"type":["number","null"]},"confidence":{"type":["number","null"]},"rawText":{"type":["string","null"]}}}}}}'::jsonb,
    '["candidate_context"]'::jsonb,
    '["finance","receipt","vision","structured-output"]'::jsonb,
    '{"capability":"accounting_receipt_parse","vision":true}'::jsonb,
    'Finance AI Capture 的单图片小票识别 Prompt，支持账户、分类候选映射和信用分期字段。'
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
       params = COALESCE(binding.params, '{}'::jsonb) || '{"prompt_version":"v1"}'::jsonb,
       updated_at = now()
  FROM cynexus_appl_ai.ai_prompt prompt
 WHERE binding.capability = 'accounting_receipt_parse'
   AND prompt.prompt_key = 'accounting_receipt_parse'
   AND prompt.prompt_version = 'v1'
   AND binding.deleted_at IS NULL
   AND prompt.deleted_at IS NULL;

COMMIT;
