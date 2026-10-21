export type FoodCategory = "vegetable" | "meat" | "seafood" | "fruit" | "dairy" | "egg" | "soy" | "grain" | "prepared" | "drink"

export interface FoodCatalogItem {
  readonly id: string
  readonly index: number
  readonly name: string
  readonly englishName: string
  readonly category: FoodCategory
  /** Whether the asset already represents a serving, bowl, pile, or cluster. */
  readonly visualStackMode: "stack" | "single" | "serving"
  /** Local fallback only. The authoritative URL is loaded from Core. */
  readonly imageUrl: string
  readonly aliases: readonly string[]
}

type SourceRow = readonly [
  name: string,
  englishName: string,
  category: FoodCategory,
  aliases?: readonly string[],
]

const sourceCatalog: readonly SourceRow[] = [
  // The atlas assets use this same row-major catalog order. The second
  // supplied sheet prints non-sequential labels, so those labels are only
  // annotations; the generated asset id is the catalog index below.
  ["大白菜", "Napa cabbage", "vegetable"],
  ["上海青", "Bok choy", "vegetable", ["Shanghai bok choy"]],
  ["生菜", "Lettuce", "vegetable"],
  ["菠菜", "Spinach", "vegetable"],
  ["韭菜", "Garlic chives", "vegetable"],
  ["香菜", "Cilantro", "vegetable", ["Coriander"]],
  ["芹菜", "Celery", "vegetable"],
  ["空心菜", "Water spinach", "vegetable"],
  ["紫甘蓝", "Red cabbage", "vegetable"],
  ["包菜", "Cabbage", "vegetable"],
  ["西兰花", "Broccoli", "vegetable"],
  ["菜花", "Cauliflower", "vegetable"],
  ["黄瓜", "Cucumber", "vegetable", ["Cucumbers"]],
  ["茄子", "Eggplant", "vegetable", ["Aubergine"]],
  ["西红柿", "Tomatoes", "vegetable", ["Tomato", "番茄"]],
  ["青椒", "Green pepper", "vegetable", ["Green bell pepper"]],
  ["红彩椒", "Red pepper", "vegetable", ["Red bell pepper"]],
  ["苦瓜", "Bitter melon", "vegetable"],
  ["丝瓜", "Luffa", "vegetable"],
  ["西葫芦", "Zucchini", "vegetable"],
  ["冬瓜", "Winter melon", "vegetable"],
  ["南瓜", "Pumpkin", "vegetable"],
  ["胡萝卜", "Carrots", "vegetable", ["Carrot"]],
  ["白萝卜", "Daikon", "vegetable"],
  ["土豆", "Potatoes", "vegetable", ["Potato"]],
  ["红薯", "Sweet potato", "vegetable"],
  ["山药", "Chinese yam", "vegetable"],
  ["莲藕", "Lotus root", "vegetable"],
  ["芋头", "Taro", "vegetable"],
  ["竹笋", "Bamboo shoots", "vegetable"],
  ["洋葱", "Onion", "vegetable"],
  ["大蒜", "Garlic", "vegetable"],
  ["生姜", "Ginger", "vegetable"],
  ["四季豆", "Green beans", "vegetable"],
  ["荷兰豆", "Snow peas", "vegetable"],
  ["黄豆芽", "Bean sprouts", "vegetable"],
  ["香菇", "Shiitake mushrooms", "vegetable", ["Mushrooms", "Shiitake"]],
  ["金针菇", "Enoki mushrooms", "vegetable"],
  ["杏鲍菇", "King oyster mushrooms", "vegetable"],
  ["平菇", "Oyster mushrooms", "vegetable"],
  ["五花肉", "Pork belly", "meat"],
  ["猪排骨", "Pork ribs", "meat"],
  ["猪里脊", "Pork tenderloin", "meat"],
  ["猪蹄", "Pig trotters", "meat"],
  ["猪肝", "Pork liver", "meat"],
  ["牛排", "Steak", "meat", ["Beef steak"]],
  ["牛腩", "Beef brisket", "meat"],
  ["羊排", "Lamb chops", "meat"],
  ["羊肉卷", "Lamb rolls", "meat"],
  ["猪肉末", "Ground pork", "meat", ["Minced pork"]],
  ["整鸡", "Whole chicken", "meat"],
  ["鸡胸肉", "Chicken breast", "meat"],
  ["鸡腿", "Chicken drumstick", "meat", ["Chicken leg"]],
  ["鸡翅", "Chicken wings", "meat"],
  ["鸡爪", "Chicken feet", "meat"],
  ["鸭腿", "Duck leg", "meat"],
  ["鲫鱼", "Crucian carp", "seafood"],
  ["鲈鱼", "Sea bass", "seafood"],
  ["带鱼", "Hairtail", "seafood"],
  ["黄花鱼", "Yellow croaker", "seafood"],
  ["三文鱼", "Salmon", "seafood"],
  ["鳕鱼", "Cod", "seafood"],
  ["鲳鱼", "Pomfret", "seafood"],
  ["虾", "Shrimp", "seafood", ["Prawns"]],
  ["螃蟹", "Crab", "seafood"],
  ["皮皮虾", "Mantis shrimp", "seafood"],
  ["鱿鱼", "Squid", "seafood"],
  ["章鱼", "Octopus", "seafood"],
  ["扇贝", "Scallops", "seafood"],
  ["蛤蜊", "Clams", "seafood"],
  ["苹果", "Apple", "fruit"],
  ["香蕉", "Bananas", "fruit", ["Banana"]],
  ["橙子", "Orange", "fruit"],
  ["橘子", "Mandarin", "fruit"],
  ["梨", "Pear", "fruit"],
  ["桃子", "Peach", "fruit"],
  ["西瓜", "Watermelon", "fruit"],
  ["葡萄", "Grapes", "fruit"],
  ["草莓", "Strawberries", "fruit", ["Strawberry"]],
  ["菠萝", "Pineapple", "fruit"],
  ["芒果", "Mango", "fruit"],
  ["猕猴桃", "Kiwi", "fruit", ["Kiwifruit"]],
  ["火龙果", "Dragon fruit", "fruit"],
  ["柠檬", "Lemon", "fruit"],
  ["柚子", "Pomelo", "fruit"],
  ["石榴", "Pomegranate", "fruit"],
  ["山竹", "Mangosteen", "fruit"],
  ["榴莲", "Durian", "fruit"],
  ["蓝莓", "Blueberries", "fruit", ["Blueberry"]],
  ["樱桃", "Cherries", "fruit", ["Cherry"]],
  ["荔枝", "Lychee", "fruit"],
  ["龙眼", "Longan", "fruit"],
  ["枇杷", "Loquat", "fruit"],
  ["李子", "Plum", "fruit"],
  ["冬枣", "Jujube", "fruit"],
  ["哈密瓜", "Hami melon", "fruit"],
  ["木瓜", "Papaya", "fruit"],
  ["椰子", "Coconut", "fruit"],
  ["百香果", "Passion fruit", "fruit"],
  ["牛油果", "Avocado", "fruit", ["Avocados"]],
  ["牛奶", "Milk", "dairy"],
  ["低脂牛奶", "Low-fat milk", "dairy"],
  ["无糖酸奶", "Unsweetened yogurt", "dairy"],
  ["希腊酸奶", "Greek yogurt", "dairy"],
  ["果味酸奶", "Fruit yogurt", "dairy"],
  ["淡奶油", "Whipping cream", "dairy"],
  ["奶油", "Cream", "dairy"],
  ["奶酪块", "Cheese blocks", "dairy"],
  ["芝士片", "Cheese slices", "dairy"],
  ["黄油", "Butter", "dairy"],
  ["鸡蛋", "Chicken eggs", "egg", ["Egg", "Eggs"]],
  ["鸭蛋", "Duck eggs", "egg"],
  ["鹌鹑蛋", "Quail eggs", "egg"],
  ["茶叶蛋", "Tea eggs", "prepared"],
  ["水煮蛋", "Boiled eggs", "prepared"],
  ["豆腐", "Tofu", "soy"],
  ["北豆腐", "Firm tofu", "soy"],
  ["内酯豆腐", "Silken tofu", "soy"],
  ["豆干", "Dried tofu", "soy"],
  ["豆皮", "Tofu skin", "soy"],
  ["白米饭", "White rice", "grain"],
  ["糙米饭", "Brown rice", "grain"],
  ["小米粥", "Millet porridge", "prepared"],
  ["白粥", "Rice porridge", "prepared"],
  ["燕麦粥", "Oatmeal porridge", "prepared"],
  ["玉米粥", "Corn porridge", "prepared"],
  ["玉米", "Corn", "vegetable"],
  ["玉米粒", "Corn kernels", "vegetable"],
  ["馒头", "Steamed bun", "prepared"],
  ["花卷", "Steamed flower roll", "prepared"],
  ["意大利面", "Pasta", "prepared"],
  ["番茄肉酱意面", "Spaghetti with tomato meat sauce", "prepared"],
  ["奶油蘑菇意面", "Creamy mushroom pasta", "prepared"],
  ["汤面", "Noodle soup", "prepared"],
  ["牛肉面", "Beef noodle soup", "prepared"],
  ["鸡汤面", "Chicken noodle soup", "prepared"],
  ["拌面", "Tossed noodles", "prepared"],
  ["炸酱面", "Noodles with soybean paste", "prepared"],
  ["凉面", "Cold noodles", "prepared"],
  ["米粉", "Rice vermicelli", "prepared"],
  ["蛋炒饭", "Egg fried rice", "prepared"],
  ["鸡肉炒饭", "Chicken fried rice", "prepared"],
  ["牛肉炒饭", "Beef fried rice", "prepared"],
  ["扬州炒饭", "Yangzhou fried rice", "prepared"],
  ["拌饭", "Mixed rice bowl", "prepared"],
  ["石锅拌饭", "Bibimbap", "prepared"],
  ["咖喱饭", "Curry rice", "prepared"],
  ["卤肉饭", "Braised pork rice", "prepared"],
  ["煲仔饭", "Clay pot rice", "prepared"],
  ["盖浇饭", "Rice with topping", "prepared"],
  ["番茄炒蛋", "Tomato and egg stir-fry", "prepared"],
  ["宫保鸡丁", "Kung pao chicken", "prepared"],
  ["鱼香肉丝", "Shredded pork with garlic sauce", "prepared"],
  ["青椒肉丝", "Shredded pork with green pepper", "prepared"],
  ["红烧肉", "Braised pork belly", "prepared"],
  ["糖醋里脊", "Sweet and sour pork tenderloin", "prepared"],
  ["回锅肉", "Twice-cooked pork", "prepared"],
  ["麻婆豆腐", "Mapo tofu", "prepared"],
  ["土豆炖牛肉", "Beef and potato stew", "prepared"],
  ["可乐鸡翅", "Cola chicken wings", "prepared"],
  ["番茄蛋汤", "Tomato and egg soup", "prepared"],
  ["紫菜蛋花汤", "Seaweed egg drop soup", "prepared"],
  ["鸡汤", "Chicken soup", "prepared"],
  ["排骨汤", "Pork rib soup", "prepared"],
  ["玉米排骨汤", "Corn and pork rib soup", "prepared"],
  ["冬瓜汤", "Winter melon soup", "prepared"],
  ["海带汤", "Kelp soup", "prepared"],
  ["罗宋汤", "Borscht", "prepared"],
  ["南瓜汤", "Pumpkin soup", "prepared"],
  ["味噌汤", "Miso soup", "prepared"],
  ["烤鸡", "Roast chicken", "meat"],
  ["烤鸭", "Roast duck", "meat"],
  ["卤鸡腿", "Braised chicken leg", "meat"],
  ["卤牛肉", "Braised beef", "meat"],
  ["香肠", "Sausage", "meat"],
  ["火腿", "Ham", "meat"],
  ["培根", "Bacon", "meat"],
  ["肉丸", "Meatballs", "meat"],
  ["鱼丸", "Fish balls", "seafood"],
  ["虾仁", "Peeled shrimp", "seafood"],
  ["速冻水饺", "Frozen dumplings", "prepared"],
  ["馄饨", "Wontons", "prepared"],
  ["小笼包", "Soup dumplings", "prepared"],
  ["包子", "Steamed stuffed buns", "prepared"],
  ["汤圆", "Sweet rice balls", "prepared"],
  ["春卷", "Spring rolls", "prepared"],
  ["锅贴", "Pot stickers", "prepared"],
  ["年糕", "Rice cake", "prepared"],
  ["鱼豆腐", "Fish tofu", "seafood"],
  ["关东煮", "Oden", "prepared"],
  ["豆浆", "Soy milk", "drink"],
  ["豆奶", "Soy drink", "drink"],
  ["椰奶", "Coconut milk", "drink"],
  ["鲜榨橙汁", "Fresh orange juice", "drink"],
  ["面包", "Bread", "prepared"],
  ["吐司", "Toast", "prepared"],
  ["可颂", "Croissant", "prepared"],
  ["三明治", "Sandwich", "prepared"],
  ["饭团", "Rice ball", "prepared"],
  ["汉堡", "Hamburger", "prepared"],
  ["即食燕麦片", "Instant oats", "grain", ["燕麦", "Oat", "Oats", "Oatmeal"]],
  ["原味燕麦片", "Rolled oats", "grain"],
  ["燕麦麸", "Oat bran", "grain"],
  ["紫薯", "Purple sweet potato", "vegetable"],
  ["烤紫薯", "Roasted purple sweet potato", "prepared"],
  ["蒸红薯", "Steamed sweet potato", "prepared"],
  ["烤土豆", "Roasted potato", "prepared"],
  ["小土豆", "Baby potatoes", "vegetable"],
  ["藜麦", "Quinoa", "grain"],
  ["杂粮饭", "Multigrain rice", "grain"],
  ["黑米饭", "Black rice", "grain"],
  ["燕麦饭", "Oat rice", "grain"],
  ["全麦面包", "Whole wheat bread", "grain"],
  ["全麦吐司", "Whole wheat toast", "grain"],
  ["黑麦面包", "Rye bread", "grain"],
  ["全麦贝果", "Whole wheat bagel", "grain"],
  ["荞麦面", "Buckwheat noodles", "grain"],
  ["小米饭", "Millet rice", "grain"],
  ["熟鸡胸肉", "Cooked chicken breast", "meat"],
  ["去皮鸡腿肉", "Skinless chicken thigh", "meat"],
  ["鸡里脊", "Chicken tenderloin", "meat"],
  ["瘦牛肉片", "Lean beef slices", "meat"],
  ["牛里脊", "Beef tenderloin", "meat"],
  ["瘦猪里脊", "Lean pork tenderloin", "meat"],
  ["低脂火鸡胸", "Lean turkey breast", "meat"],
  ["鸡蛋白", "Egg whites", "egg"],
  ["水浸鱼罐头", "Tuna in water", "seafood"],
  ["白灼虾", "Poached shrimp", "seafood"],
  ["虾仁", "Peeled shrimp", "seafood"],
  ["清蒸鳕鱼", "Steamed cod", "seafood"],
  ["清煎三文鱼", "Pan-seared salmon", "seafood"],
  ["金枪鱼排", "Tuna steak", "seafood"],
  ["鲭鱼", "Mackerel", "seafood"],
  ["比目鱼", "Flounder", "seafood"],
  ["扇贝圈", "Scallop rings", "seafood"],
  ["水浸沙丁鱼", "Sardines in water", "seafood"],
  ["脱脂牛奶", "Skim milk", "dairy"],
  ["无糖希腊酸奶", "Unsweetened Greek yogurt", "dairy"],
  ["高蛋白酸奶", "High-protein yogurt", "dairy"],
  ["低脂酸奶", "Low-fat yogurt", "dairy"],
  ["无糖豆浆", "Unsweetened soy milk", "drink"],
  ["无糖豆乳", "Unsweetened soy drink", "drink"],
  ["低脂奶酪", "Low-fat cheese", "dairy"],
  ["茅屋奶酪", "Cottage cheese", "dairy"],
  ["乳清蛋白饮料", "Whey protein drink", "drink"],
  ["毛豆", "Edamame", "soy"],
  ["青豆", "Green peas", "vegetable"],
  ["鹰嘴豆", "Chickpeas", "soy"],
  ["黑豆", "Black beans", "soy"],
  ["红腰豆", "Kidney beans", "soy"],
  ["扁豆", "Lentils", "soy"],
  ["嫩豆腐", "Soft tofu", "soy"],
  ["豆腐皮", "Tofu skin", "soy"],
  ["天贝", "Tempeh", "soy"],
  ["芦笋", "Asparagus", "vegetable"],
  ["樱桃番茄", "Cherry tomatoes", "vegetable"],
  ["芥蓝", "Chinese broccoli", "vegetable"],
  ["油麦菜", "A-choy", "vegetable"],
  ["羽衣甘蓝", "Kale", "vegetable"],
  ["菜心", "Choy sum", "vegetable"],
  ["秋葵", "Okra", "vegetable"],
  ["豌豆苗", "Pea shoots", "vegetable"],
  ["紫菜", "Nori", "vegetable"],
  ["蓝莓", "Blueberries", "fruit"],
  ["树莓", "Raspberries", "fruit"],
  ["黑莓", "Blackberries", "fruit"],
  ["葡萄柚", "Grapefruit", "fruit"],
  ["蔓越莓", "Cranberries", "fruit"],
  ["桑葚", "Mulberries", "fruit"],
  ["无花果", "Figs", "fruit"],
  ["椰枣", "Dates", "fruit"],
  ["山楂", "Hawthorn berries", "fruit"],
  ["杏仁", "Almonds", "grain"],
  ["核桃", "Walnuts", "grain"],
  ["腰果", "Cashews", "grain"],
  ["开心果", "Pistachios", "grain"],
  ["南瓜籽", "Pumpkin seeds", "grain"],
  ["葵花籽", "Sunflower seeds", "grain"],
  ["奇亚籽", "Chia seeds", "grain"],
  ["亚麻籽", "Flax seeds", "grain"],
  ["花生", "Peanuts", "grain"],
  ["鸡胸肉沙拉", "Chicken breast salad", "prepared"],
  ["金枪鱼沙拉", "Tuna salad", "prepared"],
  ["虾仁沙拉", "Shrimp salad", "prepared"],
  ["牛油果鸡蛋沙拉", "Avocado egg salad", "prepared"],
  ["鸡胸肉糙米饭", "Chicken and brown rice", "prepared"],
  ["三文鱼藜麦饭", "Salmon quinoa bowl", "prepared"],
  ["鸡蛋全麦三明治", "Egg whole wheat sandwich", "prepared"],
  ["牛肉蔬菜碗", "Beef and vegetable bowl", "prepared"],
  ["隔夜燕麦", "Overnight oats", "prepared"],
]

/**
 * These assets already show a cluster or small loose food group. Repeating
 * the whole cutout for every gram makes the visual read as several bowls or
 * plates, so quantity stays in the text while the art remains one image.
 */
const singleVisualFoodIds = new Set([
  "food-036", // bean sprouts
  "food-078", "food-089", "food-090", "food-091", "food-092", // fruit clusters
  "food-121", "food-122", // rice bowls
  "food-201", "food-202", "food-203", // oats
  "food-209", "food-210", "food-211", "food-212", // grains and rice bowls
  "food-246", "food-247", "food-248", "food-249", "food-250", "food-251", // legumes
  "food-256", "food-264", "food-265", "food-266", "food-268", "food-269", "food-270", "food-271", "food-272", // fruit clusters
  "food-273", "food-274", "food-275", "food-276", "food-277", "food-278", "food-279", "food-280", "food-281", // nuts and seeds
])

const servingVisualFoodIds = new Set([
  "food-123", "food-124", "food-125", "food-126", // porridge bowls
  "food-064", "food-069", "food-070", "food-113", "food-178", "food-179", "food-180", // small foods shown as a group
  "food-217", "food-218", // noodle and rice portions
  "food-228", "food-229", "food-235", // seafood portions
])

const servingVisualCategories = new Set<FoodCategory>(["dairy", "drink", "egg", "prepared", "soy"])

export const fridgeFoodCatalog: readonly FoodCatalogItem[] = Object.freeze(
  sourceCatalog.map(([name, englishName, category, aliases = []], offset) => {
    const index = offset + 1
    const id = "food-" + String(index).padStart(3, "0")
    return Object.freeze({
      id,
      index,
      name,
      englishName,
      category,
      visualStackMode: singleVisualFoodIds.has(id)
        ? "single"
        : servingVisualCategories.has(category) || servingVisualFoodIds.has(id) || ["food-213", "food-214", "food-215", "food-216"].includes(id)
          ? "serving"
          : "stack",
      imageUrl: "/mobile/fridge/foods/" + id + ".webp",
      aliases: Object.freeze([...aliases]),
    })
  }),
)

let foodAssetUrls = new Map<string, string>()

/** Replace the in-memory view of the DB-maintained asset mapping. */
export function setFridgeFoodAssets(mappings: readonly { catalogKey: string; assetUrl: string }[]) {
  foodAssetUrls = new Map(mappings
    .filter((mapping) => mapping.catalogKey && /^\/[-\w./]+$/.test(mapping.assetUrl))
    .map((mapping) => [mapping.catalogKey, mapping.assetUrl]))
}

export function getFridgeFoodImageUrl(food: FoodCatalogItem) {
  return foodAssetUrls.get(food.id) ?? food.imageUrl
}

export function getFridgeFoodAssetUrl(catalogKey: string | null | undefined) {
  return catalogKey ? foodAssetUrls.get(catalogKey) : undefined
}

function normalizeFoodName(value: string) {
  return value.normalize("NFKC").trim().toLowerCase().replace(/[\s_-]+/g, " ")
}

const foodBySearchName = new Map<string, FoodCatalogItem>()
for (const food of fridgeFoodCatalog) {
  for (const candidate of [food.name, food.englishName, ...food.aliases]) {
    const key = normalizeFoodName(candidate)
    if (!foodBySearchName.has(key)) foodBySearchName.set(key, food)
  }
}

export function findFridgeFoodByName(value: string) {
  return foodBySearchName.get(normalizeFoodName(value))
}
