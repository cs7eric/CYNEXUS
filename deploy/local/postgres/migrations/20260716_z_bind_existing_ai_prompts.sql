\set ON_ERROR_STOP on

BEGIN;

WITH capability_prompt(capability, prompt_key) AS (
    VALUES
        ('text_polish', 'mp4_text_polish'),
        ('text_summary', 'mp4_text_summary'),
        ('post_metadata', 'post_metadata'),
        ('mp4_qa', 'mp4_qa'),
        ('application_analysis', 'application_analysis_annotation'),
        ('application_recent30', 'application_recent30_short_term_daily'),
        ('auction_analysis', 'auction_analysis'),
        ('knowledge_base_document_generate', 'knowledge_base_document_generate'),
        ('market_information_event_analysis', 'market_information_event_analysis'),
        ('market_information_daily_brief', 'market_information_daily_brief')
)
UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET prompt_id = prompt.id,
       updated_at = now()
  FROM capability_prompt mapping
  JOIN cynexus_appl_ai.ai_prompt prompt
    ON prompt.prompt_key = mapping.prompt_key
   AND prompt.status = 'active'
   AND prompt.deleted_at IS NULL
 WHERE binding.capability = mapping.capability
   AND binding.prompt_id IS NULL
   AND binding.deleted_at IS NULL;

COMMIT;
