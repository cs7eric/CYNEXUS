export interface ApplicationAnalysisDailySnapshot {
  target?: Record<string, unknown> | null
  date?: string
  recent_window?: Record<string, unknown> | null
  analysis_result?: Record<string, unknown> | null
  short_term_trend?: Record<string, unknown> | null
  current_situation?: Record<string, unknown> | null
  summary?: Record<string, unknown> | null
  updated_at?: string
}
