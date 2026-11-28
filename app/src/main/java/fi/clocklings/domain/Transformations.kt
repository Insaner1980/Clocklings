package fi.clocklings.domain

/** Existing rewards retain their slots and star thresholds. New full-body rewards share milestones. */
private data class Design(
    val id: String,
    val newAt: Int? = null,
)

private val designs =
    mapOf(
        CompanionKind.CAR to
            listOf(
                Design("car-model-fire-engine"),
                Design("car-model-ice-cream", 5),
                Design("car-model-monster", 8),
                Design("car-model-recycling", 12),
                Design("car-model-rocket"),
            ),
        CompanionKind.CAT to
            listOf(
                Design("cat-head-sailor-hat"),
                Design("cat-extra-yarn"),
                Design("cat-model-detective", 9),
                Design("cat-model-boots", 13),
            ),
        CompanionKind.DOG to
            listOf(
                Design("dog-model-branch", 4),
                Design("dog-model-rescue", 7),
                Design("dog-model-post", 11),
                Design("dog-extra-raincoat"),
            ),
        CompanionKind.ROBOT to
            listOf(
                Design("robot-model-cuckoo", 4),
                Design("robot-model-rover", 10),
                Design("robot-extra-bubble-pack"),
                Design("robot-model-driver", 14),
            ),
        CompanionKind.DRAGON to
            listOf(
                Design("dragon-model-marshmallow", 4),
                Design("dragon-model-ice", 8),
                Design("dragon-model-ruby", 11),
                Design("dragon-model-woodland", 14),
            ),
        CompanionKind.DINOSAUR to
            listOf(
                Design("dinosaur-model-egg", 3),
                Design("dinosaur-model-stegosaurus", 6),
                Design("dinosaur-model-fossil", 10),
                Design("dinosaur-extra-skateboard"),
            ),
        CompanionKind.BUNNY to
            listOf(
                Design("bunny-head-top-hat"),
                Design("bunny-model-carrot-car", 5),
                Design("bunny-model-moon", 11),
                Design("bunny-model-strawberry", 14),
            ),
        CompanionKind.UNICORN to
            listOf(
                Design("unicorn-model-sea", 5),
                Design("unicorn-model-carousel", 8),
                Design("unicorn-model-shooting-star", 14),
                Design("unicorn-head-butterfly"),
            ),
        CompanionKind.HUMAN to
            listOf(
                Design("model-veterinarian", 6),
                Design("shoes-roller-skates"),
                Design("model-dragon-costume", 14),
            ),
    )

internal val additionalModelItems =
    designs.flatMap { (kind, choices) ->
        choices.mapNotNull { design -> design.newAt?.let { Item(design.id, kind, Slot.MODEL, it) } }
    }

internal val transformationIds = designs.mapValues { (_, choices) -> choices.map { it.id }.sorted() }
private val transformedItems = transformationIds.values.flatten().toSet()

/** Prefer the model slot if an older outfit contains multiple newly modelled counterparts. */
internal fun Outfit.transformationId(): String? =
    transformation ?: this[Slot.MODEL]?.takeIf { it in transformedItems }
        ?: items.values.firstOrNull { it in transformedItems }

internal fun Outfit.withoutTransformations(kind: CompanionKind): Outfit =
    if (transformationId() == null) {
        this
    } else {
        Outfit(Outfit.default(kind).items + items.filterValues { it !in transformedItems })
    }

internal val Item.isTransformation: Boolean get() = id in transformedItems

/** Keep ordinary clothes and car fittings available when a full-body reward is removed. */
internal fun Outfit.equip(item: Item): Outfit {
    val base = withoutTransformations(item.companion)
    return if (item.isTransformation) base.copy(transformation = item.id) else base.with(item.slot, item.id)
}
