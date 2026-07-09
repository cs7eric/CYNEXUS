export const QA_STYLE_FIX = `
.columns,
.result-box,
.summary-box,
.summary-stage,
.summary-workspace,
.qa-box,
.qa-json-response {
  min-width: 0;
  max-width: 100%;
}

.columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.result-box,
.summary-box {
  overflow: hidden !important;
  border: 0 !important;
  border-radius: 6px !important;
  background: #f9f9f9 !important;
  box-shadow: none !important;
}

.qa-box {
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--background));
  box-shadow: none;
}

.result-header,
.qa-item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 48px;
  padding: 11px 14px;
  border-bottom: 1px solid hsl(var(--border));
  background: hsl(var(--muted) / 0.62);
  cursor: pointer;
  user-select: none;
}

.result-box > .result-header,
.summary-box > .result-header {
  border-bottom: 0 !important;
  background: #f9f9f9 !important;
}

.result-header:hover,
.qa-item-header:hover {
  background: hsl(var(--muted));
}

.result-title {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 650;
  line-height: 1.35;
}

.result-title .icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.result-meta {
  display: inline-flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  flex-shrink: 0;
}

.char-count {
  color: hsl(var(--muted-foreground));
  font-size: 12px;
}

.result-toggle,
.qa-item-toggle {
  flex-shrink: 0;
  color: hsl(var(--muted-foreground));
  transition: transform 0.16s ease;
}

.result-toggle.open,
.qa-item-toggle.open {
  transform: rotate(180deg);
}

.result-body-wrap {
  display: none;
}

.result-body-wrap.open {
  display: block;
  background: #f9f9f9 !important;
}

.result-body {
  max-height: 62vh;
  min-height: 320px;
  overflow: auto;
  padding: 14px;
  color: hsl(var(--foreground));
  background: #f9f9f9 !important;
  font-size: 14px;
  line-height: 1.78;
  overflow-wrap: anywhere;
  scrollbar-width: thin;
}

.result-body pre,
.summary-raw,
.summary-raw-view {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
  font-family: inherit;
}

.summary-stage {
  position: relative;
  padding: 14px;
  background: #f9f9f9 !important;
}

.summary-workspace {
  display: grid;
  gap: 14px;
  overflow-wrap: anywhere;
}

.summary-raw,
.summary-raw-view {
  display: none;
  max-height: 62vh;
  overflow: auto;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted) / 0.45);
  padding: 14px;
  color: hsl(var(--foreground));
  font-size: 13px;
  line-height: 1.7;
  scrollbar-width: thin;
}

.summary-stage.raw-mode .summary-workspace,
.summary-stage.raw-mode .summary-raw {
  display: none;
}

.summary-stage.raw-mode .summary-raw-view {
  display: block;
}

.summary-panel,
.qa-json-logic-pane,
.qa-json-support-block,
.knowledge-node,
.learning-card,
.decision-card,
.qa-fallback-card {
  overflow: hidden;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--background));
  overflow-wrap: anywhere;
}

.summary-panel-head {
  padding: 12px 14px;
  border-bottom: 1px solid hsl(var(--border));
  background: hsl(var(--muted) / 0.58);
}

.summary-kicker,
.qa-json-eyebrow {
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  font-weight: 650;
  text-transform: uppercase;
}

.summary-panel-title,
.qa-json-title {
  margin-top: 4px;
  color: hsl(var(--foreground));
  font-size: 15px;
  font-weight: 650;
  line-height: 1.35;
}

.summary-panel-subtitle,
.qa-json-pane-subtitle {
  margin-top: 3px;
  color: hsl(var(--muted-foreground));
  font-size: 12px;
  line-height: 1.5;
}

.summary-panel-body {
  padding: 14px;
}

.summary-hero-card,
.knowledge-hub {
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted) / 0.45);
  padding: 13px;
}

.summary-hero-eyebrow,
.knowledge-hub-label {
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  font-weight: 650;
  text-transform: uppercase;
}

.summary-hero-title,
.knowledge-hub-text {
  margin-top: 4px;
  color: hsl(var(--foreground));
  font-size: 16px;
  font-weight: 650;
  line-height: 1.45;
}

.summary-hero-text {
  margin-top: 10px;
  color: hsl(var(--foreground));
  font-size: 13px;
  line-height: 1.75;
}

.summary-flow,
.decision-stack,
.risk-radar-grid,
.learning-grid,
.knowledge-board {
  display: grid;
  gap: 10px;
}

.knowledge-board {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.summary-flow-step {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  padding: 10px;
}

.summary-flow-index,
.summary-list-icon,
.qa-json-line-dot,
.qa-json-pane-icon,
.qa-json-core-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted));
  color: hsl(var(--foreground));
  font-size: 11px;
  font-weight: 650;
}

.summary-flow-title,
.knowledge-node-title,
.decision-title,
.learning-title,
.qa-json-pane-title,
.qa-json-support-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  color: hsl(var(--foreground));
  font-size: 13px;
  font-weight: 650;
  line-height: 1.45;
}

.summary-flow-text,
.knowledge-node-body,
.learning-body {
  margin-top: 5px;
  color: hsl(var(--foreground));
  font-size: 13px;
  line-height: 1.7;
}

.summary-pill,
.qa-json-pill {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted) / 0.65);
  color: hsl(var(--muted-foreground));
  padding: 2px 7px;
  font-size: 11px;
  font-weight: 600;
}

.summary-list,
.qa-json-lines {
  display: grid;
  gap: 8px;
}

.summary-list-item,
.qa-json-line {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr);
  gap: 9px;
  align-items: flex-start;
  color: hsl(var(--foreground));
  font-size: 13px;
  line-height: 1.7;
}

.decision-card,
.learning-card,
.knowledge-node,
.qa-json-logic-pane,
.qa-json-support-block,
.qa-fallback-card {
  padding: 13px;
}

.decision-card.risk,
.qa-json-support-block.risk {
  border-color: hsl(var(--destructive) / 0.28);
  background: hsl(var(--destructive) / 0.04);
}

.summary-empty-state,
.qa-empty {
  border: 1px dashed hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted) / 0.45);
  padding: 14px;
  color: hsl(var(--muted-foreground));
  font-size: 13px;
}

.qa-section {
  margin-top: 14px;
}

.qa-body {
  padding: 14px;
  background: hsl(var(--background));
}

.qa-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.qa-item {
  overflow: hidden !important;
  margin-bottom: 0 !important;
  border: 1px solid hsl(var(--border)) !important;
  border-radius: 6px !important;
  background: hsl(var(--background)) !important;
  box-shadow: none !important;
}

.qa-item-q-text {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  color: hsl(var(--foreground)) !important;
  font-size: 13px !important;
  font-weight: 650 !important;
  line-height: 1.45 !important;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.qa-item-q-text.expanded {
  white-space: normal;
}

.qa-item-q-text::before {
  content: "Q";
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  margin-right: 8px;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted));
  color: hsl(var(--foreground));
  font-size: 10px;
  font-weight: 700;
}

.qa-item-body {
  display: none;
  border-top: 1px solid hsl(var(--border));
  background: hsl(var(--background));
}

.qa-item-body.open {
  display: block;
}

.qa-answer-body {
  max-height: 720px !important;
  overflow-y: auto !important;
  padding: 12px !important;
  background: hsl(var(--background)) !important;
  scrollbar-width: thin;
}

.qa-answer {
  margin: 0;
  color: hsl(var(--foreground));
  font-size: 14px;
  line-height: 1.8;
  white-space: normal;
  word-break: break-word;
}

.qa-json-response {
  display: block;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--background));
  padding: 12px;
  white-space: normal;
}

.qa-json-top,
.qa-json-footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.qa-json-pills,
.qa-json-actions,
.qa-json-followups {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.qa-json-core {
  margin-bottom: 12px;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--muted) / 0.45);
  padding: 13px;
  overflow-wrap: anywhere;
}

.qa-json-core-label,
.qa-json-pane-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
  color: hsl(var(--foreground));
  font-size: 12px;
  font-weight: 650;
}

.qa-json-logic {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  gap: 10px;
  align-items: stretch;
  margin-bottom: 12px;
}

.qa-json-flow-arrow {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: hsl(var(--muted-foreground));
  font-size: 16px;
  font-weight: 650;
}

.qa-json-support {
  display: grid;
  gap: 10px;
  margin-bottom: 12px;
}

.qa-json-support.two {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.qa-json-support.one {
  grid-template-columns: 1fr;
}

.qa-action-btn,
.qa-followup-chip,
.action-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  min-height: 32px !important;
  width: auto !important;
  border: 1px solid hsl(var(--border)) !important;
  border-radius: 6px !important;
  background: hsl(var(--background)) !important;
  color: hsl(var(--foreground)) !important;
  padding: 6px 10px !important;
  font-size: 12px !important;
  font-weight: 600 !important;
  cursor: pointer !important;
  transition: background-color 0.14s ease, border-color 0.14s ease;
}

.qa-action-btn:hover,
.qa-followup-chip:hover,
.action-btn:hover {
  background: hsl(var(--muted)) !important;
}

.qa-json-followup-label,
.qa-json-disclaimer {
  color: hsl(var(--muted-foreground));
  font-size: 11px;
  line-height: 1.6;
}

.qa-floating-anchor {
  height: 86px;
}

.qa-floating-wrap {
  position: fixed;
  left: 50%;
  bottom: 18px;
  z-index: 45;
  width: min(760px, calc(100vw - 32px));
  transform: translateX(-50%);
  pointer-events: none;
}

.qa-floating-shell {
  pointer-events: auto;
}

.qa-floating-inner {
  display: flex;
  align-items: center;
  gap: 8px;
}

.qa-search-box {
  position: relative;
  flex: 1;
}

.qa-search-input {
  width: 100%;
  height: 42px;
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  background: hsl(var(--background));
  color: hsl(var(--foreground));
  padding: 0 42px 0 12px;
  font-size: 14px;
  line-height: 1.4;
  outline: none;
  box-shadow: 0 8px 24px hsl(var(--foreground) / 0.08);
}

.qa-search-input:focus {
  border-color: hsl(var(--foreground) / 0.35);
}

.qa-search-input::placeholder {
  color: hsl(var(--muted-foreground));
}

.qa-search-icon {
  position: absolute;
  top: 50%;
  right: 13px;
  width: 18px;
  height: 18px;
  color: hsl(var(--muted-foreground));
  transform: translateY(-50%);
  pointer-events: none;
}

.result-body::-webkit-scrollbar,
.summary-raw-view::-webkit-scrollbar,
.qa-answer-body::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

.result-body::-webkit-scrollbar-track,
.summary-raw-view::-webkit-scrollbar-track,
.qa-answer-body::-webkit-scrollbar-track {
  background: transparent;
}

.result-body::-webkit-scrollbar-thumb,
.summary-raw-view::-webkit-scrollbar-thumb,
.qa-answer-body::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: hsl(var(--muted-foreground) / 0.3);
}

@media (max-width: 900px) {
  .columns,
  .knowledge-board,
  .qa-json-logic,
  .qa-json-support.two {
    grid-template-columns: 1fr;
  }

  .qa-json-flow-arrow {
    display: none;
  }
}

@media (max-width: 640px) {
  .columns {
    gap: 12px;
    margin-bottom: 12px;
  }

  .result-header,
  .qa-item-header {
    align-items: flex-start;
    padding: 11px 12px;
  }

  .result-meta {
    flex-wrap: wrap;
  }

  .result-body {
    max-height: 58vh;
    min-height: 260px;
    padding: 12px;
    font-size: 14px;
  }

  .summary-stage,
  .qa-body {
    padding: 12px;
  }

  .qa-floating-anchor {
    height: 78px;
  }

  .qa-floating-wrap {
    bottom: 12px;
    width: calc(100vw - 24px);
  }

  .qa-search-input {
    height: 40px;
    font-size: 16px;
  }
}
`;
