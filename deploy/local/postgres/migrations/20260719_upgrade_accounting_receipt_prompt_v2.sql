\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_ai.ai_prompt
   SET system_prompt = system_prompt || E'\n微信转账详情中的 Payment Method: Balance 表示微信余额，优先选择名称或备注包含微信、微信余额、微信钱包的账户；如果候选中没有对应账户，accountHint 必须返回“微信余额”。',
       updated_at = now()
 WHERE prompt_key = 'accounting_receipt_parse'
   AND prompt_version = 'v1'
   AND deleted_at IS NULL;

COMMIT;
