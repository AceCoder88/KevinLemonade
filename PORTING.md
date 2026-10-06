# Porting the rest of the original

`original/kevin.html` is the original web game (one file: CSS, markup, then one big
`<script>` from line 1652). Everything is being ported to Kotlin + Jetpack Compose,
matching the original as closely as possible: **same lines of dialogue, same prices,
same formulas, same timings, same names.** When in doubt, copy what the original does.

## How the app is laid out

- `Game.kt` - `GameViewModel`: core stand state, squeezing, Kevin's mood, the shop's
  `lvl` map, save/load, the once-a-second `ticker()`. Owns one object per feature
  (`g.war`, `g.space`, `g.dig`, `g.pirate`, `g.ride`, `g.tower`, `g.closet`,
  `g.dating`, `g.travel`, `g.therapy`, `g.fun_`).
- `Features.kt` - the `Feature` interface every feature object implements:
  `save(j)` / `load(j)` (its own JSON object, saved inside the main save),
  `reset()` (Restart), `onRebirth()`, `maxOut()` (the MAX OUT code), `tick()`
  (once a second, main thread). Keep state in Compose state
  (`mutableStateOf` etc.) so screens recompose.
- One file per feature: `War.kt`, `Space.kt`, `Dig.kt`, `Pirate.kt`, `Ride.kt`,
  `Tower.kt`, `Closet.kt`, `Dating.kt`, `Travel.kt`, `Therapy.kt`, `Fun.kt`
  (lottery, races, trophies, rebirth, codes, settings), `Shop.kt` (Mr. Zest).
  Each starts as a stub: a state class plus a `XxxScreen(g)` composable showing
  "Coming soon". You may split a feature into more files with the same prefix
  (`WarArt.kt`, `WarLines.kt`, ...).
- `Screen` enum (in `Game.kt`) has one entry per screen; `KevinApp` in `Ui.kt`
  routes them and handles Back. Go somewhere with `g.screen = Screen.WAR`, come
  home with `g.screen = Screen.STAND`.
- `Hub.kt` - "Places to go" tabs on the stand, with the original's unlock rules.
- Shared helpers you should reuse: `ChunkyButton`, `OutlinedTitle`, `MoneyText`,
  `UpgradeCard` (Ui.kt), the `Lilita` / `Nunito` fonts, colors `LINE`, `RED`,
  `PEEL`, `CARD`, `BG`, `GRASS`, `SKY`; SVG-path drawing helpers `svg()`,
  `shape()`, `stroke()`, `oval()` (StageArt.kt); `g.say(text)` (Kevin's bubble),
  `g.popAt(...)` (floating text on the stage), `g.toast(text)` (the original's
  `toast()`), `fmt(n)` (money), `List.pick()`, `g.named(s)` (`{N}` -> Kevin's name),
  `g.lv(id)` (shop upgrade level), `g.money`, `g.lemonsLeft`, `g.addLemons(n)`.
- `CatchScreen.kt` is the model for porting a `<canvas>` minigame: a plain Kotlin
  sim class stepped from a `withFrameMillis` loop, drawn in a Compose `Canvas`
  scaled from the original's canvas coordinates.
- Cross-feature values already declared (so every branch compiles against them):
  `g.closet.wearing(id)` / `g.closet.wearingSet(id)`, `g.dating.momoWith`,
  `g.war.bonds`, `g.fun_.rebirths` / `g.fun_.rebirthMult()`, `g.inRealm()`.
  Keep these names and signatures.

## Rules while several agents work in parallel

- Each agent works in its own git worktree on its own branch and **only edits the
  files it owns** (listed in its brief). Anything you need changed in a file you
  don't own: don't edit it - write it down under "Integration notes" in your final
  report (what file, what change, why) and the integrator will do it.
- Don't rename or remove anything public that already exists.
- Build with `bash /c/Users/paula/Documents/Code/Kevin/kbuild.sh <your worktree>`
  (it queues behind other agents' builds; prints only errors). It must end
  `BUILD SUCCESSFUL` before you finish. Don't run gradle directly.
- Commit your work on your branch (small commits are fine). Commit message
  trailer: `Co-Authored-By: Claude Sonnet 5 <noreply@anthropic.com>`
- Kids play this. Keep the original's tone; don't add anything new that isn't in
  the original.
