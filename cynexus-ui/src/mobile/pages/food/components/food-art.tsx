import { CookingPot } from "lucide-react"
import { useState } from "react"

const artwork: Record<string, string> = {
  bowl: "bowl.svg",
  oats: "tabletop/oats.svg",
  yogurt: "tabletop/yogurt.svg",
  blueberries: "tabletop/blueberries.svg",
  almonds: "tabletop/almonds.svg",
  avocado: "tabletop/avocado.svg",
  chicken: "tabletop/chicken.svg",
  rice: "tabletop/rice.svg",
}
const nameAliases: Record<string, keyof typeof artwork> = {
  oat: "oats", oatmeal: "oats", oats: "oats", "燕麦": "oats",
  milk: "yogurt", yogurt: "yogurt", "牛奶": "yogurt", 酸奶: "yogurt",
  blueberry: "blueberries", blueberries: "blueberries", "蓝莓": "blueberries",
  almond: "almonds", almonds: "almonds", "杏仁": "almonds",
  avocado: "avocado", "牛油果": "avocado",
  chicken: "chicken", "鸡胸": "chicken", "鸡腿": "chicken", "鸡肉": "chicken",
  rice: "rice", "米饭": "rice", "大米": "rice",
}

/** Artwork is an explicit catalog association, independent of names and nutrition. */
export function FoodArt({ food, className = "" }: { food: { assetKey?: string | null; assetUrl?: string | null; name?: string; foodName?: string; id?: string | null; foodId?: string | null; kind?: string | null; entryKind?: string | null }; className?: string }) {
  const key = food.assetKey?.trim().toLowerCase()
  const name = (food.name ?? food.foodName ?? "").trim().toLowerCase()
  const customFoodId = food.foodId ?? (food.kind === "custom" || food.entryKind === "custom" ? food.id : null)
  const matchingAlias = Object.keys(nameAliases).find(alias => name.includes(alias))
  const inferredKey = nameAliases[name] ?? (matchingAlias ? nameAliases[matchingAlias] : undefined)
  const sources = [
    food.assetUrl,
    customFoodId && /^\d+$/.test(customFoodId) ? `/api/core/food/custom-foods/${customFoodId}/asset` : null,
    key && /^food-\d{3}$/.test(key) ? `/mobile/fridge/foods/${key}.webp` : null,
    key && artwork[key] ? `/mobile/food/${artwork[key]}` : null,
    key && /^(?:tabletop\/)?[^/]+\.(?:svg|webp|png)$/i.test(key) ? `/mobile/food/${key}` : null,
    inferredKey === "oats" ? "/mobile/fridge/foods/food-201.webp" : null,
    inferredKey ? `/mobile/food/${artwork[inferredKey]}` : null,
  ].filter((value): value is string => !!value)
  const [failedSource, setFailedSource] = useState<string | null>(null)
  const source = sources.find(value => value !== failedSource)

  return source
    ? <img className={`food-art ${className}`} src={source} alt="" draggable={false} onError={() => setFailedSource(source)} />
    : <span className={`food-art food-art-placeholder ${className}`} aria-hidden="true"><CookingPot /><span>{food.name ?? food.foodName ?? "Food"}</span></span>
}
