import type { ComponentType } from "react"
import AStockSkillPoc from "../a-stock-skill"
import DemoPoc from "../demo-poc"
import SentimentOverlayPoc from "../sentiment-overlay"

export interface PocTabConfig {
  key: string
  label: string
  component: ComponentType
}

export const POC_TABS: PocTabConfig[] = [
  { key: "demo", label: "Demo", component: DemoPoc },
  { key: "a-stock-skill", label: "A股 Skill", component: AStockSkillPoc },
  { key: "sentiment-overlay", label: "情绪+上证叠加", component: SentimentOverlayPoc },
]
