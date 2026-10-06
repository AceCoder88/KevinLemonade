# Kevin watches the lemonade get made — native Android (Kotlin + Jetpack Compose)

## Run it
1. Open this folder in Android Studio (File > Open > KevinLemonade).
2. Let Gradle sync (first time takes a few minutes).
3. Pick a phone or emulator and press Run.

## What's ported (part 1: the core)
- Kevin, drawn natively from the original artwork: eyes that follow the action, blinking,
  mood colors, tears, every mouth and brow state, jump, shiver, and the secret 6-tap dance.
- The squeezer: relatives drop in one at a time with all their original lines, hand cranking
  (with the SUPER SQUEEZE bonus) until you buy the motor, golden and diamond lemons,
  the pitcher filling up, and selling with VIPs and tips.
- Sadness: the meter, half price and $1 price, poking for free therapy.
- Holding Kevin: the ring and all five warnings.
- Mr. Zest's shop: every upgrade that affects the stand, with the original prices and formulas.
- Catch the Lemons: the tree, basket, helpers, and the lemon tree shop.
- Progress saves automatically (the web version didn't save).

## Not ported yet
Death Realm, turning evil and the Nightmare Realm (so holding Kevin stops at "LAST WARNING"
for now), therapy with Dr. Rind, Lemy, the closet, war, space, digging, pirates, horse ride,
wizard tower, lottery, races, trophies, rebirth, traveling shop, and codes.

## Files
- `Game.kt` — all game state, formulas, the squeeze sequence and timers
- `StageArt.kt` — the main scene, drawn from the original SVG
- `Ui.kt` — the stand screen and Mr. Zest's shop
- `CatchScreen.kt` — Catch the Lemons
- `Upgrades.kt` — upgrade catalog
- `Lines.kt` — every line Kevin says (copied from the original)
