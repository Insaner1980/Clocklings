package fi.clocklings.audio

import fi.clocklings.R
import fi.clocklings.domain.CompanionKind

/**
 * A sound effect. A sound with a [clip] plays a recorded file from res/raw; the others are
 * synthesized by [ToneSynth]. A [voice] is a companion's voice: only one voice plays at a time.
 */
enum class Sound(
    val clip: Int? = null,
    val voice: Boolean = false,
) {
    OK(R.raw.correct),
    BAD(R.raw.wrong),
    WIN(R.raw.success),
    POP,
    INTRO(R.raw.clocklings),
    STAR(R.raw.star),
    UNLOCK(R.raw.unlock),
    RECORD(R.raw.record),
    LESSON_COMPLETE(R.raw.lesson_complete),
    EQUIP(R.raw.equip),
    HUMAN(R.raw.companion_human, voice = true),
    CAT(R.raw.companion_cat, voice = true),
    DOG(R.raw.companion_dog, voice = true),
    ROBOT(R.raw.companion_robot, voice = true),
    DRAGON(R.raw.companion_dragon, voice = true),
    DINOSAUR(R.raw.companion_dinosaur, voice = true),
    BUNNY(R.raw.companion_bunny, voice = true),
    UNICORN(R.raw.companion_unicorn, voice = true),
    CAR(R.raw.companion_car, voice = true),
}

/** The voice of [kind], or null without a companion. */
fun companionVoice(kind: CompanionKind): Sound? =
    when (kind) {
        CompanionKind.HUMAN -> Sound.HUMAN
        CompanionKind.CAT -> Sound.CAT
        CompanionKind.DOG -> Sound.DOG
        CompanionKind.ROBOT -> Sound.ROBOT
        CompanionKind.DRAGON -> Sound.DRAGON
        CompanionKind.DINOSAUR -> Sound.DINOSAUR
        CompanionKind.BUNNY -> Sound.BUNNY
        CompanionKind.UNICORN -> Sound.UNICORN
        CompanionKind.CAR -> Sound.CAR
        CompanionKind.NONE -> null
    }
