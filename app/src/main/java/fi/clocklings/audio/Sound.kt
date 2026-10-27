package fi.clocklings.audio

import fi.clocklings.R
import fi.clocklings.domain.CompanionKind

/**
 * A sound effect. A sound with a [clip] plays a recorded file from res/raw; the others are
 * synthesized by [ToneSynth].
 */
enum class Sound(
    val clip: Int? = null,
) {
    OK,
    BAD,
    WIN(R.raw.success),
    POP,
    TICK,
    CAT(R.raw.companion_cat),
    DOG(R.raw.companion_dog),
}

/** The voice of [kind], or null while the companion has no recorded voice. */
fun companionVoice(kind: CompanionKind): Sound? =
    when (kind) {
        CompanionKind.CAT -> Sound.CAT

        CompanionKind.DOG -> Sound.DOG

        CompanionKind.HUMAN,
        CompanionKind.ROBOT,
        CompanionKind.DRAGON,
        CompanionKind.DINOSAUR,
        CompanionKind.NONE,
        -> null
    }
