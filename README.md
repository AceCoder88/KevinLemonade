# Kevin watches the lemonade get made — native Android (Kotlin + Jetpack Compose)

A port of the original web game (`original/kevin.html`) to a native Android app: same lines,
prices, formulas and timings. Progress saves automatically (the web version didn't save).

## Get it on a phone
Every push to `main` builds the APK on GitHub Actions and puts it on the
[latest release](../../releases/tag/latest). Open that page on the phone, tap `KevinLemonade.apk`,
and allow the install. Every build is signed with the same key (`app/kevin.keystore`), so a new
APK installs as an update over the old one.

## Build it here
1. Open this folder in Android Studio (File > Open > KevinLemonade), or run
   `./gradlew assembleRelease` with a JDK 17 or 21 (Gradle 8.11 can't run on JDK 25).
2. The release build is much faster than the debug build; use it to judge performance.

## What's in it
- **The stand:** squeezing (hand crank until the motor), golden/diamond lemons, limes, extra
  squeezers and the second squeezer after rebirth, Kevin's moods, sadness and therapy taps,
  holding Kevin, the puppy, day and night, Lemy at the stand, outfits and potion effects drawn on Kevin.
- **Death Realm and Nightmare Realm:** visiting the family, ghosts and the ever-growing crowd,
  turning gray then evil, escaping the Nightmare Realm, the Dead Closet, Death War, Broken War
  and the Horrible Hole, spooky upgrades.
- **Shops:** Mr. Zest's full shop (tools, luck, therapy, advanced), Wanda's traveling shop and
  helpers, General Sourpuss's war shop and traveling deals.
- **Worlds:** Catch the Lemons, War, Space (planets, missions, the Space Station, Darth Lime),
  Digging and the Underworld, Pirates, the Horse Ride, the Wizard Tower.
- **Fun:** lottery, races and bettors, trophies (and secret ones), surprise events, rebirth,
  secret codes, the admin panel, MAX OUT, settings (what Kevin talks about, cute mode, OG mode).
- **Kevin:** therapy with Dr. Rind, his closet (64 pieces, 17 sets), the LemonLove dating app.

## Known gaps
- Some art is simplified compared with the original: dating-app portraits, Wanda, space
  fruit people and the duel, war enemies (shapes and colors match; not every stroke does).
- "Furious Kevin" (the Lemy-rescue boss in the Underworld) isn't ported, so the
  "Calm down, Kevin!" secret trophy can't be earned yet.
- A few admin-panel buttons that reach into other features are left out.

## Layout
See `PORTING.md`. In short: `Game.kt` is the core; each other part of the game is a `Feature`
in its own file (`War.kt`, `Space.kt`, `Dig.kt`, `Pirate.kt`, `Ride.kt`, `Tower.kt`, `Closet.kt`,
`Dating.kt`, `Travel.kt`, `Therapy.kt`, `RealmState.kt`, `Fun*.kt`, `Shop.kt`), saved inside the
main save and ticked once a second. `Hub.kt` is the "Places to go" menu.
