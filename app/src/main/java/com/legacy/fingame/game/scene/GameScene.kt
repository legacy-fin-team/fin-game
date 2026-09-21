package com.legacy.fingame.game.scene

import com.legacy.fingame.game.animals.Animal
import com.legacy.fingame.game.items.ItemCatalog
import com.legacy.fingame.game.items.ItemSelection

/**
 * One sprite standing on a layer of the game area.
 *
 * @property assetPath path to the sprite, relative to /assets/textures/.
 * @property description what the sprite is, for screen readers, or null when it is pure scenery the
 * player doesn't need to be told about.
 */
data class SceneSprite(
    val assetPath: String,
    val description: String?
)

/**
 * Everything drawn in the game area, sorted into the [GameLayer]s it is drawn on.
 *
 * Built by [of] out of the player's state, so the rules of what covers what are decided here — where
 * they can be checked without a screen — and the screen only walks the layers in
 * [GameLayer.DRAW_ORDER] and draws what each of them holds.
 *
 * @property sprites sprites of every layer that has any, keyed by layer. A layer nothing stands on is
 * missing from the map rather than holding an empty list.
 */
data class GameScene(val sprites: Map<GameLayer, List<SceneSprite>> = emptyMap()) {

    companion object {

        /**
         * Puts the game area together out of what the player's pet has on and stands in.
         *
         * @param background scenery of the sub-location the pet is in, or null while there is none.
         * @param pet the pet itself, at the age stage it has grown to, or null while no pet has been
         * picked yet.
         * @param animalId species id of that pet, or null while there is none: clothes are cut to
         * fit the animal they sit on, so they cannot be drawn without knowing which animal that is
         * (see [com.legacy.fingame.game.items.Item.getEquippedSpritePath]).
         * @param animalAge age stage that pet has grown to, the very one its own sprite is drawn at
         * (see [com.legacy.fingame.game.animals.Animal.getIdleSpritePath]), so that the clothes on
         * it grow along with it. Defaults to the stage every pet starts at, which is the pet the
         * previews show.
         * @param worn items the player put on the pet; an item that is not drawn in the game area at
         * all (see [com.legacy.fingame.game.items.Item.layer]) is skipped, as is one the catalog no
         * longer registers.
         * @param catalog what the game knows about the items, for their layers and sprites.
         * @return The scene, with the worn items of one layer ordered by their ids, so two things on
         * the same layer always cover each other the same way — the saved state keeps no order of its
         * own that could be relied on.
         */
        fun of(
            background: SceneSprite?,
            pet: SceneSprite?,
            animalId: String?,
            worn: Set<ItemSelection>,
            catalog: ItemCatalog,
            animalAge: Int = Animal.FIRST_AGE
        ): GameScene {
            val sprites = mutableMapOf<GameLayer, MutableList<SceneSprite>>()

            if (background != null) {
                sprites.getOrPut(GameLayer.BACKGROUND) { mutableListOf() }.add(background)
            }
            if (pet != null) {
                sprites.getOrPut(GameLayer.ANIMAL) { mutableListOf() }.add(pet)
            }

            worn.sortedWith(compareBy({ it.itemId }, { it.variantId })).forEach { selection ->
                val item = catalog.findItemById(selection.itemId) ?: return@forEach
                val layer = item.layer ?: return@forEach
                val assetPath = item.getEquippedSpritePath(
                    variantId = selection.variantId,
                    animalId = animalId,
                    animalAge = animalAge
                ) ?: return@forEach
                sprites.getOrPut(layer) { mutableListOf() }.add(
                    SceneSprite(
                        assetPath = assetPath,
                        description = item.name
                    )
                )
            }

            return GameScene(sprites.mapValues { (_, layerSprites) -> layerSprites.toList() })
        }
    }

    /**
     * @param layer layer to look at.
     * @return The sprites standing on [layer], from the one covered by the others to the topmost
     * one, or an empty list when nothing stands there.
     */
    operator fun get(layer: GameLayer): List<SceneSprite> = sprites[layer] ?: emptyList()
}
