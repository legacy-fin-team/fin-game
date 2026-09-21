package com.legacy.fingame.game.items

/**
 * Asset paths (relative to `assets/textures/`) of everything an item is drawn from.
 *
 * Every item keeps its pictures in a folder named after the item's id, and the file names inside it
 * say what each picture is for: `icon.webp` is the item as the shop and the inventory show it, and
 * an `equipped-…` file is the item as it looks once it is put on. A folder of an item that is never
 * put on — food, a toy — holds the icon alone.
 *
 * Path building lives here and nowhere else, the way [com.legacy.fingame.ui.components.Sprites] is
 * the one place that knows where the UI sprites are: a screen asks an [Item] for a path (see
 * [Item.iconPath] and [Item.getEquippedSpritePath]) instead of spelling one out of its own.
 *
 * A file that is not in the assets yet is not an error here: the path is still built, and
 * [com.legacy.fingame.utils.SpriteLoader] draws its placeholder in place of the missing picture.
 */
object ItemSprites {

    /** Folder the items' own folders sit in, relative to `assets/textures/`. */
    private const val ITEMS_FOLDER = "items"

    /** Name of the icon file inside an item's folder. */
    private const val ICON_FILE = "icon.webp"

    /** Start of the name of every file holding an item as it is worn. */
    private const val EQUIPPED_PREFIX = "equipped"

    /** Extension every sprite file has. */
    private const val SPRITE_EXTENSION = ".webp"

    /**
     * Builds the path to the item's icon, the 32x32 picture the shop and the inventory show it by.
     *
     * One icon stands for the whole item, variants and all: the player tells a black hat from a
     * white one by the variant picker, not by two icons.
     *
     * @param itemId id of the item, used verbatim as a path segment, so it must match the name of
     *   the item's folder in the assets.
     * @return path (relative to `assets/textures/`) to the item's icon.
     */
    fun icon(itemId: String): String = "$ITEMS_FOLDER/$itemId/$ICON_FILE"

    /**
     * Builds the path to the 32x32 picture of the item as it sits on a pet, i.e. what is drawn on
     * [com.legacy.fingame.game.scene.GameLayer.CLOTHES] over the animal.
     *
     * A worn item is painted for one species at a time — a hat sits on a cat's head the way it
     * never would on a dog's — hence the animal in the file name. The variant, on the other hand,
     * is the item's own (the colour of the hat the player bought), not the animal's colouring: the
     * pet's colouring is under the clothes and changes nothing about them.
     *
     * @param itemId id of the item, used verbatim as a path segment.
     * @param animalId species id of the pet the item is worn by (see
     *   [com.legacy.fingame.game.animals.Animal.id]), used verbatim as part of the file name.
     * @param variantId id of the item's variant (see [Item.variantIds]), used verbatim as part of
     *   the file name.
     * @return path (relative to `assets/textures/`) to the worn item's sprite.
     */
    fun equippedOnAnimal(itemId: String, animalId: String, variantId: String): String =
        "$ITEMS_FOLDER/$itemId/$EQUIPPED_PREFIX-$animalId-$variantId$SPRITE_EXTENSION"

    /**
     * Builds the path to the 128x128 picture of the item as it stands in the room, i.e. what is
     * drawn on [com.legacy.fingame.game.scene.GameLayer.ENVIRONMENT_BACK] or
     * [com.legacy.fingame.game.scene.GameLayer.ENVIRONMENT_FRONT] around the pet.
     *
     * A decoration belongs to the room rather than to the pet, so it is painted at the resolution
     * of the scenery and no animal is named in its file: the same rug lies under a cat and under a
     * dog.
     *
     * @param itemId id of the item, used verbatim as a path segment.
     * @param variantId id of the item's variant (see [Item.variantIds]), used verbatim as part of
     *   the file name.
     * @return path (relative to `assets/textures/`) to the decoration's sprite.
     */
    fun equippedInScenery(itemId: String, variantId: String): String =
        "$ITEMS_FOLDER/$itemId/$EQUIPPED_PREFIX-$variantId$SPRITE_EXTENSION"
}
