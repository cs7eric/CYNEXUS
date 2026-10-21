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

/** Artwork is an explicit catalog association, independent of names and nutrition. */
export function FoodArt({ food, className = "" }: { food: { assetKey?: string | null; assetUrl?: string | null; name?: string; foodName?: string }; className?: string }) {
  const key = food.assetKey?.trim().toLowerCase()
  const sources = [
    food.assetUrl,
    key && artwork[key] ? `/mobile/food/${artwork[key]}` : null,
    key && /^food-\d{3}$/.test(key) ? `/mobile/fridge/foods/${key}.webp` : null,
  ].filter((value): value is string => !!value)
  const [failedSource, setFailedSource] = useState<string | null>(null)
  const source = sources.find(value => value !== failedSource)

  return source
    ? <img className={`food-art ${className}`} src={source} alt="" draggable={false} onError={() => setFailedSource(source)} />
    : <span className={`food-art food-art-placeholder ${className}`} aria-hidden="true"><CookingPot /><span>{food.name ?? food.foodName ?? "Food"}</span></span>
}
