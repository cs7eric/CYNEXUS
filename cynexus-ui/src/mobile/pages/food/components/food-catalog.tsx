import { ChevronRight, Plus, Search, Star } from "lucide-react"
import { useState } from "react"
import { formatNutrition } from "@/services/food/model"
import type { Food } from "@/services/food/types"
import { useFoodSearch, type FoodFilter } from "../hooks/use-food-data"
import { FoodArt } from "./food-art"

export function FoodCatalog({ onSelect, onCustom, onQuick, compact = false }: { onSelect: (food: Food) => void; onCustom: () => void; onQuick?: () => void; compact?: boolean }) {
  const [query, setQuery] = useState("")
  const [filter, setFilter] = useState<FoodFilter>("all")
  const result = useFoodSearch(query, filter)
  return <div className="food-catalog">
    {!compact && <div className="food-section-title"><p className="food-eyebrow">Your food library</p><h1>Good food,<br /><em>at your fingertips.</em></h1></div>}
    <label className="food-search"><Search aria-hidden="true" /><input type="search" placeholder="Search foods, brands…" aria-label="Search foods" maxLength={80} value={query} onChange={event => setQuery(event.target.value)} /></label>
    <div className="food-segments" role="group" aria-label="Food library filter">{([['all', 'All foods'], ['favorites', 'Favorites'], ['mine', 'My foods']] as const).map(([key, label]) => <button key={key} type="button" aria-pressed={filter === key} onClick={() => setFilter(key)}>{key === "favorites" && <Star aria-hidden="true" />}{label}</button>)}</div>
    <div className="food-library-actions"><button type="button" className="food-text-button" onClick={onCustom}><Plus />Create food</button>{onQuick && <button type="button" className="food-text-button" onClick={onQuick}>Quick add</button>}</div>
    {result.loading && <p className="food-feedback" role="status">Finding something good…</p>}
    {result.error && <div className="food-feedback" role="alert"><p>{result.error}</p><button type="button" className="food-text-button" onClick={result.reload}>Try again</button></div>}
    {!result.loading && !result.error && !result.data?.length && <div className="food-empty"><Search /><h3>{query ? "No matching foods" : filter === "favorites" ? "Keep your favorites close." : "What’s on your menu?"}</h3><p>{query ? "Try another name or create your own food." : "Search the nutrition library or create a food. Saved favorites will appear here."}</p></div>}
    <div className="food-catalog-list">{result.data?.map(food => <button type="button" className="food-catalog-row" key={`${food.fridgeItemId ? `fridge/${food.fridgeItemId}` : `${food.kind}/${food.id}`}`} onClick={() => onSelect(food)}><FoodArt food={food} /><span><strong>{food.name}</strong><small>{food.fridgeItemId ? `In your fridge · ${food.availableQuantity ?? 0} ${food.availableUnit ?? ""}` : [food.brand, food.source ?? (food.kind === "custom" ? "My food" : null)].filter(Boolean).join(" · ") || "Food"}</small><small>{food.fridgeItemId && food.fridgeMealReady === false ? "Nutrition mapping needed" : `${formatNutrition(food.nutrients.caloriesKcal)} kcal / 100 g${food.isComplete === false && " · Partial"}`}</small></span><ChevronRight aria-hidden="true" /></button>)}</div>
    {query && !!result.data?.length && <p className="food-footnote">Showing a limited set of matches. Refine your search for more specific results.</p>}
  </div>
}
