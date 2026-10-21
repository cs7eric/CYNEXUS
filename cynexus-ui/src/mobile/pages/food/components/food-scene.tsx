import { ChevronRight, CookingPot, Plus } from "lucide-react"
import { useState, type CSSProperties } from "react"
import type { DiaryItem, MealType } from "@/services/food/types"
import { FoodArt } from "./food-art"
import { foodAmount, groupFoods } from "./scene-model"

const slots = [{ x: 18, y: 26, angle: -12 }, { x: 81, y: 26, angle: 9 }, { x: 50, y: 40, angle: -8 }, { x: 18, y: 74, angle: 5 }, { x: 82, y: 74, angle: 15 }, { x: 50, y: 80, angle: -10 }]

export function FoodScene({ items, onSelectGroup, onAdd, busy }: { items: DiaryItem[]; onSelectGroup: (items: DiaryItem[]) => void; onAdd: (meal: MealType) => void; busy: boolean }) {
  const [showMore, setShowMore] = useState(false)
  const allGroups = groupFoods(items)
  const groups = allGroups.map((group, index) => ({ ...group, slot: index % slots.length })).filter(group => group.entries.length)
  const occupied = new Set<number>()
  const visible = groups.slice(0, 6).map(group => {
    const slot = occupied.has(group.slot) ? slots.findIndex((_, index) => !occupied.has(index)) : group.slot
    occupied.add(slot)
    return { ...group, slot }
  })
  return <>
    <div className="food-table-stage" role="group" aria-label="All day food visualization">
      <div className="food-table-linen" aria-hidden="true" />
      {visible.map(group => {
        const anchor = slots[group.slot]
        return <button type="button" key={group.key} className="food-table-object" style={{ "--food-x": `${anchor.x}%`, "--food-y": `${anchor.y}%`, "--food-angle": `${anchor.angle}deg`, zIndex: group.slot + 1 } as CSSProperties} onClick={() => onSelectGroup(group.entries)} aria-label={`${group.entries[0].foodName}, ${foodAmount(group.entries)}. View details`}>
          <FoodArt food={group.entries[0]} /><span className="food-table-amount">{foodAmount(group.entries)}</span>
        </button>
      })}
      {!visible.length && <div className="food-table-empty"><CookingPot /><strong>A fresh table, a fresh start.</strong><button type="button" disabled={busy} onClick={() => onAdd("breakfast")}><Plus />Log your first food</button></div>}
    </div>
    <div className="food-table-caption"><span>{groups.length ? "Your day, one bite at a time." : "Your next good thing starts here."}</span>{groups.length > 6 ? <button type="button" aria-expanded={showMore} onClick={() => setShowMore(value => !value)} className="food-table-more">{showMore ? "Show less" : `+${groups.length - 6} more`}<ChevronRight /></button> : <span>{groups.length > 0 && "Tap a food to explore"}</span>}</div>
    {showMore && groups.length > 6 && <div className="food-scene-more-list">{groups.slice(6).map(group => <button type="button" key={group.key} onClick={() => onSelectGroup(group.entries)}><FoodArt food={group.entries[0]} /><span><strong>{group.entries[0].foodName}</strong><small>{foodAmount(group.entries)}</small></span><ChevronRight /></button>)}</div>}
  </>
}
