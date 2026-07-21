\set ON_ERROR_STOP on

BEGIN;

UPDATE cynexus_appl_ai.ai_prompt
   SET system_prompt = system_prompt || E'\n分期识别规则：若图片显示“花呗分期(3期)”“分三期”“第1/3期”等，必须提取 installmentCount 为实际总期数；消费金额 amount 仍填写图片顶部的本次交易总金额，不要填写单期金额。若看得到手续费才填写 installmentFee，否则为 null。信用支付方式中的花呗、白条、抖音月付或信用卡应通过 accountHint 返回对应账户名称。',
       updated_at = now()
 WHERE prompt_key = 'accounting_receipt_parse'
   AND prompt_version = 'v1'
   AND deleted_at IS NULL;

COMMIT;
