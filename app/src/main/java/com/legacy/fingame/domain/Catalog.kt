package com.legacy.fingame.domain

/** Каталог товаров магазина. Еда и декор — расходуемые (CONSUMABLE), одежда и игрушки — покупаются один раз (UNIQUE). */
object ShopCatalog {
    val items: List<ShopItem> = listOf(
        // Еда — CONSUMABLE
        ShopItem("food_bone", "Косточка", 30, ItemCategory.FOOD, PurchaseKind.CONSUMABLE, "🦴"),
        ShopItem("food_fish", "Рыбка", 45, ItemCategory.FOOD, PurchaseKind.CONSUMABLE, "🐟"),
        ShopItem("food_milk", "Молоко", 25, ItemCategory.FOOD, PurchaseKind.CONSUMABLE, "🥛"),
        ShopItem("food_cake", "Тортик", 80, ItemCategory.FOOD, PurchaseKind.CONSUMABLE, "🍰"),
        ShopItem("food_apple", "Яблоко", 20, ItemCategory.FOOD, PurchaseKind.CONSUMABLE, "🍎"),

        // Игрушки — в основном UNIQUE
        ShopItem("toy_ball", "Мячик", 60, ItemCategory.TOYS, PurchaseKind.UNIQUE, "🎾"),
        ShopItem("toy_mouse", "Игрушечная мышь", 50, ItemCategory.TOYS, PurchaseKind.UNIQUE, "🐭"),
        ShopItem("toy_yarn", "Клубок ниток", 40, ItemCategory.TOYS, PurchaseKind.UNIQUE, "🧶"),
        ShopItem("toy_bone_chew", "Жевательная игрушка", 70, ItemCategory.TOYS, PurchaseKind.UNIQUE, "🦴"),
        ShopItem("toy_bubbles", "Мыльные пузыри", 35, ItemCategory.TOYS, PurchaseKind.CONSUMABLE, "🫧"),

        // Одежда — в основном UNIQUE
        ShopItem("cloth_hat", "Шляпа", 120, ItemCategory.CLOTHES, PurchaseKind.UNIQUE, "🎩"),
        ShopItem("cloth_scarf", "Шарф", 90, ItemCategory.CLOTHES, PurchaseKind.UNIQUE, "🧣"),
        ShopItem("cloth_bandana", "Бандана", 60, ItemCategory.CLOTHES, PurchaseKind.UNIQUE, "🏴"),
        ShopItem("cloth_glasses", "Очки", 100, ItemCategory.CLOTHES, PurchaseKind.UNIQUE, "🕶️"),
        ShopItem("cloth_bowtie", "Бабочка", 70, ItemCategory.CLOTHES, PurchaseKind.UNIQUE, "🎀"),

        // Декор — CONSUMABLE
        ShopItem("decor_flower", "Цветок", 40, ItemCategory.DECOR, PurchaseKind.CONSUMABLE, "🌸"),
        ShopItem("decor_lamp", "Лампа", 90, ItemCategory.DECOR, PurchaseKind.CONSUMABLE, "🪔"),
        ShopItem("decor_carpet", "Ковёр", 110, ItemCategory.DECOR, PurchaseKind.CONSUMABLE, "🧵"),
        ShopItem("decor_plant", "Растение", 55, ItemCategory.DECOR, PurchaseKind.CONSUMABLE, "🪴")
    )
}

/** Каталог доступных питомцев. */
object PetCatalog {
    val pets: List<Pet> = listOf(
        Pet("cat", "Барсик", "🐱"),
        Pet("dog", "Шарик", "🐶"),
        Pet("rabbit", "Пушок", "🐰"),
        Pet("hamster", "Хома", "🐹")
    )
}

/** Подлокации, доступные для перемещения на основном экране. */
object Locations {
    val subLocations: List<String> = listOf("Комната", "Кухня", "Двор", "Балкон")
}
