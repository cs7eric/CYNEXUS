\set ON_ERROR_STOP on

BEGIN;

-- Labels are product metadata, while capability remains the stable runtime key.
-- Backfill older/manual bindings so the Provider routing page never falls back
-- to a raw capability code when a binding predates its UI catalog entry.
WITH labels(capability, label) AS (
    VALUES
        ('accounting_receipt_parse', 'Accounting receipt recognition'),
        ('application_analysis', 'Stock application analysis'),
        ('application_recent30', 'AI Direction recent 30-day'),
        ('auction_analysis', 'Auction AI analysis'),
        ('autonomous_agent', 'Cynexus autonomous Agent'),
        ('knowledge_base_document_generate', 'Knowledge Base document generation'),
        ('market.futures_position_analysis', 'Futures position structure analysis'),
        ('market_futures_position_analysis', 'Futures position structure analysis'),
        ('market.stock_daily_feature', 'Stock daily feature analysis'),
        ('market_information_daily_brief', 'Market Information daily brief'),
        ('market_information_event_analysis', 'Market Information event analysis'),
        ('mp4_qa', 'MP4 Ask AI'),
        ('portfolio_trade_screenshot_parse', 'Portfolio trade screenshot parsing'),
        ('post_metadata', 'Markdown metadata'),
        ('text_polish', 'MP4 transcription polish'),
        ('text_summary', 'MP4 summary'),
        ('workspace_chat', 'Workspace chat')
)
UPDATE cynexus_appl_ai.ai_usage_bindings binding
   SET label = labels.label,
       updated_at = now()
  FROM labels
 WHERE binding.capability = labels.capability
   AND binding.deleted_at IS NULL
   AND (binding.label IS NULL OR btrim(binding.label) = '');

COMMIT;
