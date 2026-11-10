import { ChevronRight, Droplet, Dumbbell, Flame, LoaderCircle, Plus, Refrigerator, Search, Star, UserRound, Wheat } from "lucide-react"
import { Card } from "@heroui/react"
import { useRef, useState } from "react"
import { addFavorite, announceFoodChange, deleteFavorite } from "@/services/food/api"
import { formatNutrition } from "@/services/food/model"
import type { Food } from "@/services/food/types"
import { useFoodSearch, type FoodFilter } from "../hooks/use-food-data"
import { FoodArt } from "./food-art"
import "./food-catalog.css"

export function FoodLibraryFavoriteButton({ food }: { food: Food }) {
  const [favoriteId, setFavoriteId] = useState(food.favoriteId ?? null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const pending = useRef(false)

  async function toggleFavorite() {
    if (pending.current) return
    pending.current = true
    setBusy(true)
    setError(null)
    try {
      if (favoriteId) {
        await deleteFavorite(favoriteId)
        setFavoriteId(null)
      } else {
        const saved = await addFavorite(food.kind, food.id, crypto.randomUUID())
        setFavoriteId(saved.favoriteId ?? null)
      }
      announceFoodChange()
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to update favorite.")
    } finally {
      pending.current = false
      setBusy(false)
    }
  }

  return <>
    <button className="food-library-favorite" type="button" aria-label={`${favoriteId ? "Remove" : "Add"} ${food.name} ${favoriteId ? "from" : "to"} favorites`} aria-pressed={!!favoriteId} aria-busy={busy} disabled={busy} title={favoriteId ? "Remove from favorites" : "Add to favorites"} onClick={toggleFavorite}>
      {busy ? <LoaderCircle aria-hidden="true" /> : <Star aria-hidden="true" fill={favoriteId ? "currentColor" : "none"} />}
    </button>
    {error && <span className="food-library-favorite-error" role="alert">{error}</span>}
  </>
}

export function FoodCatalog({ onSelect, onCustom, onOpenFridge, onQuick, compact = false }: { onSelect: (food: Food) => void; onCustom: () => void; onOpenFridge: (fridgeItemId: string) => void; onQuick?: () => void; compact?: boolean }) {
  const [query, setQuery] = useState("")
  const [filter, setFilter] = useState<FoodFilter>("all")
  const result = useFoodSearch(query, filter)
  const foods = result.data ?? []
  const filterLabel = filter === "favorites" ? "Favorites" : filter === "mine" ? "My foods" : "All foods"
  return <div className={`food-catalog${compact ? " food-catalog-compact" : " food-catalog-full"}`}>
    {!compact && <header className="food-library-header">
      <p className="food-library-eyebrow">Your kitchen</p>
      <h1>Good food,<br /><span>at your fingertips.</span></h1>
      <p className="food-library-subtitle">Saved for your next meal.</p>
    </header>}
    {compact ? <>
      <label className="food-search"><Search aria-hidden="true" /><input type="search" placeholder="Search foods, brands…" aria-label="Search foods" maxLength={80} value={query} onChange={event => setQuery(event.target.value)} /></label>
      <div className="food-segments" role="group" aria-label="Food library filter">{([['all', 'All foods'], ['favorites', 'Favorites'], ['mine', 'My foods']] as const).map(([key, label]) => <button key={key} type="button" aria-pressed={filter === key} onClick={() => setFilter(key)}>{key === "favorites" && <Star aria-hidden="true" />}{label}</button>)}</div>
      <div className="food-library-actions"><button type="button" className="food-text-button" onClick={onCustom}><Plus />Create food</button>{onQuick && <button type="button" className="food-text-button" onClick={onQuick}>Quick add</button>}</div>
      {result.loading && <p className="food-feedback" role="status">Finding something good…</p>}
      {result.error && <div className="food-feedback" role="alert"><p>{result.error}</p><button type="button" className="food-text-button" onClick={result.reload}>Try again</button></div>}
      {!result.loading && !result.error && !foods.length && <div className="food-empty"><Search /><h3>{query ? "No matching foods" : filter === "favorites" ? "Keep your favorites close." : "What’s on your menu?"}</h3><p>{query ? "Try another name or create your own food." : "Search the nutrition library or create a food. Saved favorites will appear here."}</p></div>}
      <div className="food-catalog-grid">{foods.map(food => <Card<"div">
        key={`${food.fridgeItemId ? `fridge/${food.fridgeItemId}` : `${food.kind}/${food.id}`}`}
        className="food-catalog-card"
        variant="default"
        render={props => <div {...props} />}
      >
        <button className="food-catalog-card-select" type="button" aria-label={`Add ${food.name} to today. ${formatNutrition(food.nutrients.caloriesKcal)} kilocalories; per 100 grams: carbs ${formatNutrition(food.nutrients.carbsG, 1)} grams, protein ${formatNutrition(food.nutrients.proteinG, 1)} grams, fat ${formatNutrition(food.nutrients.fatG, 1)} grams.`} onClick={() => onSelect(food)} />
        <span className="food-catalog-card-art"><FoodArt food={food} /></span>
        <Card.Content className="food-catalog-card-details">
          <Card.Header className="food-catalog-card-heading">
            <Card.Title className="food-catalog-card-name">{food.name}</Card.Title>
            <span className="food-catalog-card-energy"><Flame aria-hidden="true" /><strong>{formatNutrition(food.nutrients.caloriesKcal)}</strong><small>kcal / 100 g</small></span>
          </Card.Header>
          <span className="food-catalog-card-summary">
            <span className="food-catalog-card-meta">{food.fridgeItemId ? <><Refrigerator aria-hidden="true" /><button className="food-catalog-card-stock food-catalog-card-stock-action" type="button" aria-label={`Open fridge view. ${formatNutrition(food.availableQuantity, 1)} ${food.availableUnit ?? "g"} in fridge.`} onClick={event => { event.stopPropagation(); onOpenFridge(food.fridgeItemId!) }}><strong>{formatNutrition(food.availableQuantity, 1)} {food.availableUnit ?? "g"}</strong><span>in fridge</span><ChevronRight aria-hidden="true" /></button></> : <>{food.kind === "custom" && <UserRound aria-hidden="true" />}<span>{[food.brand, food.source ?? (food.kind === "custom" ? "My food" : null)].filter(Boolean).join(" · ") || "Food"}</span></>}</span>
          </span>
        </Card.Content>
        <span className="food-catalog-card-macros">
          <span className="food-catalog-card-macro food-catalog-card-macro-carbs"><span className="food-catalog-card-macro-label"><Wheat aria-hidden="true" />Carbs</span><strong>{formatNutrition(food.nutrients.carbsG, 1)} g</strong></span>
          <span className="food-catalog-card-macro food-catalog-card-macro-protein"><span className="food-catalog-card-macro-label"><Dumbbell aria-hidden="true" />Protein</span><strong>{formatNutrition(food.nutrients.proteinG, 1)} g</strong></span>
          <span className="food-catalog-card-macro food-catalog-card-macro-fat"><span className="food-catalog-card-macro-label"><Droplet aria-hidden="true" />Fat</span><strong>{formatNutrition(food.nutrients.fatG, 1)} g</strong></span>
        </span>
      </Card>)}</div>
    </> : <>
      <div className="food-library-toolbar">
        <label className="food-library-search"><Search aria-hidden="true" /><input type="search" placeholder="Search foods or brands" aria-label="Search foods" maxLength={80} value={query} onChange={event => setQuery(event.target.value)} /></label>
        <button className="food-library-create" type="button" onClick={onCustom}><Plus aria-hidden="true" /><span>Create</span></button>
      </div>
      <div className="food-library-filters" role="group" aria-label="Food library filter">{([['all', 'All foods'], ['favorites', 'Favorites'], ['mine', 'My foods']] as const).map(([key, label]) => <button key={key} type="button" aria-pressed={filter === key} onClick={() => setFilter(key)}>{key === "favorites" && <Star aria-hidden="true" />}{label}</button>)}</div>
      <section className="food-library-results" aria-labelledby="food-library-list-title">
        <header className="food-library-results-heading"><h2 id="food-library-list-title">{filterLabel}</h2><span>{result.loading && !result.data ? "Loading…" : `${foods.length} ${foods.length === 1 ? "item" : "items"}`}</span></header>
        {result.loading && <p className="food-library-feedback" role="status">Finding foods…</p>}
        {result.error && <div className="food-library-feedback" role="alert"><p>{result.error}</p><button type="button" onClick={result.reload}>Try again</button></div>}
        {!result.loading && !result.error && !foods.length && <div className="food-library-empty"><Search aria-hidden="true" /><strong>{query ? "No foods found" : filter === "favorites" ? "No favorites yet" : filter === "mine" ? "No personal foods yet" : "Your library is empty"}</strong><p>{query ? "Try another name or create a food for your library." : filter === "favorites" ? "Save a food with the star to keep it here." : "Create a food or search for one to add it to your day."}</p></div>}
        <div className="food-library-list">{foods.map(food => <article className="food-library-item" key={`${food.fridgeItemId ? `fridge/${food.fridgeItemId}` : `${food.kind}/${food.id}`}`}>
          <button type="button" className="food-library-open" aria-label={`Add ${food.name} to today`} onClick={() => onSelect(food)}><FoodArt food={food} className="food-library-art" /><span className="food-library-copy"><strong>{food.name}</strong><small>{food.fridgeItemId ? `In your fridge · ${food.availableQuantity ?? 0} ${food.availableUnit ?? ""}` : [food.brand, food.source ?? (food.kind === "custom" ? "My food" : null)].filter(Boolean).join(" · ") || "Food"}</small><span className="food-library-energy">{food.fridgeItemId && food.fridgeMealReady === false ? "Nutrition mapping needed" : <><strong>{formatNutrition(food.nutrients.caloriesKcal)}</strong> kcal <small>/ 100 g{food.isComplete === false ? " · Partial" : ""}</small></>}</span></span><ChevronRight aria-hidden="true" /></button>
          {!food.fridgeItemId && <FoodLibraryFavoriteButton key={`${food.kind}/${food.id}/${food.favoriteId ?? ""}`} food={food} />}
        </article>)}</div>
      </section>
      {query && foods.length > 0 && <p className="food-library-footnote">Showing up to 30 matches. Refine your search for more results.</p>}
    </>}
  </div>
}
