import type { ComponentType } from "react"
import DemoPoc from "../demo-poc"
import SentimentOverlayPoc from "../sentiment-overlay"

export interface PocTabConfig {
  key: string
  label: string
  component: ComponentType
}

export const POC_TABS: PocTabConfig[] = [
  { key: "demo", label: "Demo", component: DemoPoc },
  { key: "sentiment-overlay", label: "情绪+上证叠加", component: SentimentOverlayPoc },
]
