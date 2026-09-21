package com.legacy.fingame.game.items

/**
 * Asset paths (relative to `assets/textures/`) of everything an item is drawn from.
 *
 * Every variant of every item keeps its pictures in a folder of its own, `items/<item>/<variant>/`,
 * and the file names inside it say what each picture is for: `icon.webp` is the variant as the shop
 * and the inventory show it, `equipped-…` is a piece of clothing as it sits on a pet and
 * `placed.webp` is a decoration as it stands in the room. A folder of something that is never put
 * on — food, a toy — holds the icon alone.
 *
 * Path building lives here and nowhere else, the way [com.legacy.fingame.ui.components.Sprites] is
 * the one place that knows where the UI sprites are: a screen asks an [Item] for a path (see
 * [Item.getIconPath] and [Item.getEquippedSpritePath]) instead of spelling one out of its own.
 *
 * A file that is not in the assets yet is not an error here: the path is still built, and
 * [com.legacy.fingame.utils.SpriteLoader] draws its placeholder in place of the missing picture.
 */
object ItemSprites {

    /** Folder the items' own folders sit in, relative to `assets/textures/`. */
    private const val ITEMS_FOLDER = "items"

    /** Name of the icon file inside a variant's folder. */
    private const val ICON_FILE = "icon.webp"

    /** Name of the file holding a decoration as it stands in the room. */
    private const val PLACED_FILE = "placed.webp"

    /** Start of the name of every file holding a piece of clothing as it is worn. */
    private const val EQUIPPED_PREFIX = "equipped"

    /** Extension every sprite file has. */
    private const val SPRITE_EXTENSION = ".webp"

    /**
     * Builds the path to the folder holding the pictures of one variant of an item.
     *
     * @param itemId id of the item, used verbatim as a path segment, so it must match the name of
     *   the item's folder in the assets.
     * @param variantId id of the variant (see [Item.variantIds]), used verbatim as a path segment.
     * @return path (relative to `assets/textures/`) of the variant's folder, without a trailing
     *   slash.
     */
    private fun variantFolder(itemId: String, variantId: String): String =
        "$ITEMS_FOLDER/$itemId/$variantId"

    /**
     * Builds the path to the icon of one variant, the 32x32 picture the shop and the inventory show
     * it by.
     *
     * Every variant has an icon of its own: a black hat and a white one are two different things to
     * the player, so the shop lets them be told apart by sight and not by the variant picker alone.
     *
     * @param itemId id of the item, used verbatim as a path segment.
     * @param variantId id of the variant (see [Item.variantIds]), used verbatim as a path segment.
     * @return path (relative to `assets/textures/`) to the variant's icon.
     */
    fun icon(itemId: String, variantId: String): String =
        "${variantFolder(itemId, variantId)}/$ICON_FILE"

    /**
     * Builds the path to the 32x32 picture of a piece of clothing as it sits on a pet, i.e. what is
     * drawn on [com.legacy.fingame.game.scene.GameLayer.CLOTHES] over the animal.
     *
     * A worn item is painted for one animal at one age at a time: a hat sits on a cat's head the
     * way it never would on a dog's, and a kitten's hat is not the same picture as a grown cat's,
     * so the pet the clothes are drawn over is named by its species and its age stage. Which
     * variant of the item it is has already been said by the folder the file lies in.
     *
     * @param itemId id of the item, used verbatim as a path segment.
     * @param variantId id of the item's variant (see [Item.variantIds]), used verbatim as a path
     *   segment.
     * @param animalId species id of the pet wearing the item (see
     *   [com.legacy.fingame.game.animals.Animal.id]), used verbatim as part of the file name.
     * @param animalAge age stage the pet has grown to, counted the way the animal's own sprites
     *   count it (see [com.legacy.fingame.game.animals.Animal.getIdleSpritePath]) and used verbatim
     *   as part of the file name, so the clothes grow with the pet.
     * @return path (relative to `assets/textures/`) to the worn item's sprite.
     */
    fun equippedOnAnimal(
        itemId: String,
        variantId: String,
        animalId: String,
        animalAge: Int
    ): String = "${variantFolder(itemId, variantId)}/" +
            "$EQUIPPED_PREFIX-$animalId-$animalAge$SPRITE_EXTENSION"

    /**
     * Builds the path to the 128x128 picture of a decoration as it stands in the room, i.e. what is
     * drawn on [com.legacy.fingame.game.scene.GameLayer.ENVIRONMENT_BACK] or
     * [com.legacy.fingame.game.scene.GameLayer.ENVIRONMENT_FRONT] around the pet.
     *
     * A decoration belongs to the room rather than to the pet, so it is painted at the resolution
     * of the scenery and neither the animal nor its age is named in its file: the same rug lies
     * under a kitten and under a grown dog.
     *
     * @param itemId id of the item, used verbatim as a path segment.
     * @param variantId id of the item's variant (see [Item.variantIds]), used verbatim as a path
     *   segment.
     * @return path (relative to `assets/textures/`) to the decoration's sprite.
     */
    fun placedInScenery(itemId: String, variantId: String): String =
        "${variantFolder(itemId, variantId)}/$PLACED_FILE"
}
