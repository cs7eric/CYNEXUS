UPDATE cynexus_appl_market.portfolio_import_task
SET last_error_code = NULL,
    last_error_message = NULL
WHERE status = 'READY_FOR_REVIEW'
  AND last_error_code = 'PORTFOLIO_PARSE_RETRY';
