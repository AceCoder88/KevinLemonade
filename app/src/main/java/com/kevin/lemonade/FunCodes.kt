package com.kevin.lemonade

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * One admin-panel button: a label and an action that returns the message shown after it runs.
 * A group whose features aren't ported yet on this branch is left out entirely (see the final
 * report's Integration notes) rather than shown as a button that does nothing.
 */
private class AdminAction(val label: String, val run: (GameViewModel) -> String)
private class AdminGroup(val title: String, val actions: List<AdminAction>)

private fun adminGroups(g: GameViewModel): List<AdminGroup> {
    val f = g.fun_
    return listOf(
        AdminGroup("Money", listOf(
            AdminAction("+\$1,000,000") { it.money += 1_000_000L; "+\$1,000,000!" },
            AdminAction("+\$1,000,000,000") { it.money += 1_000_000_000L; "A BILLION dollars!" },
            AdminAction("Double my money") { it.money *= 2; "Your money doubled!" },
            AdminAction("Free lottery jackpot") { it.money += 5000; f.wonJackpot = true; f.checkTrophies(); "JACKPOT! +\$5,000 and the Lucky ticket trophy." },
        )),
        AdminGroup("Customers", listOf(
            AdminAction("Summon 1,000 customers") { f.custQueue += 1000; "Here they come! 1,000 customers are walking up to the stand." },
            AdminAction("Summon 100 customers") { f.custQueue += 100; "100 customers are on the way!" },
            // INTEGRATION: "Max every WAR/space/pirate/dig upgrade" and "Hire every helper" belong here too,
            // once WarState/SpaceState/PirateState/DigState/TravelState have their shops ported.
            AdminAction("Max Catch the Lemons") { for (u in LemonUpgrades.all) it.lemonLvl[u.id] = u.max; "Catch the Lemons is maxed out!" },
            AdminAction("Unlock every tool") { for (t in listOf("motor", "lottopass", "racepass", "wand", "saddle", "bow", "pager", "telescope", "map", "closetkey", "shovel")) it.lvl[t] = 1; "Every tool is unlocked!" },
        )),
        AdminGroup("Kevin", listOf(
            AdminAction("Make Kevin super happy") { it.sad = 0.0; it.happyUntil = it.now() + 300000; it.showFace(Face.CHEER); it.say("Wheee! The admin made me SO happy!"); "Kevin is happy for 5 minutes." },
            AdminAction("Make Kevin sadder") { it.sad = minOf(it.doom(), it.sad + it.doom() * 0.35); it.happyUntil = 0; it.showFace(Face.WORRY); "Kevin got a lot sadder. Check the sad meter!" },
            AdminAction("Make Kevin super sad") { it.sad = it.doom() - 0.5; it.happyUntil = 0; it.showFace(Face.WORRY); "Kevin is SUPER sad now. Careful, he might turn evil!" },
            AdminAction("Remove Lemy (no fight)") {
                if (!it.dating.momoWith) return@AdminAction "Kevin is already single."
                it.dating.momoWith = false
                it.sad = minOf(it.doom(), it.sad + it.doom() * 0.3)
                it.say("{N} went home... I miss them already.")
                "Lemy went home."
            },
            // INTEGRATION: "Get Lemy", "Furious Kevin fight", "Rainbow/Giant/Tiny/Bubble Kevin", and "Secret
            // dance" all need either DatingState.getGirlfriend(), WarState's Furious Kevin encounter, or a
            // public hook onto GameViewModel's private poke()/dance()/size animations - none exist yet.
        )),
        AdminGroup("Stuff", listOf(
            AdminAction("Fill up lemons") { it.lemonsLeft = it.lemonCap(); "Lemons are full!" },
            AdminAction("1,000 war bonds") { it.war.bonds += 1000; "+1,000 war bonds!" },
            // INTEGRATION: "100 limes" needs PirateState.limesLeft, "Full mana" needs TowerState.mana.
        )),
        AdminGroup("Sky and events", listOf(
            AdminAction("Rainbow (5 minutes)") { f.startRainbow(300000); "Rainbow for 5 minutes: double money!" },
            AdminAction("UFO") { f.runEvent(0); "UFO incoming!" },
            AdminAction("Famous customer") { f.runEvent(1); "Someone famous stopped by!" },
            AdminAction("Sneaky bird") { f.runEvent(3); "Watch out for that bird!" },
            AdminAction("Lemon meteor shower") { f.runEvent(4); "Lemons from SPACE!" },
            AdminAction("Lemonade party") { it.partyUntil = it.now() + 60000; "PARTY! Triple money for a minute!" },
            AdminAction("Snow day") { f.runEvent(5); "Let it snow!" },
            AdminAction("Windy day") { f.runEvent(6); "Whoosh!" },
            AdminAction("Kevin's birthday") { f.runEvent(7); "Happy birthday!" },
            // INTEGRATION: "Make it night/day" needs a day/night cycle on GameViewModel (dayOffset/dayStart);
            // nothing like that exists yet.
        )),
        AdminGroup("Fun", listOf(
            AdminAction("Unlock every trophy") { f.gotTrophy.clear(); f.gotTrophy.addAll(TROPHIES.indices); f.gotSecret.clear(); f.gotSecret.addAll(SECRETS.indices); "Every trophy is unlocked, even the secret ones!" },
            AdminAction("Switch to Kevia") { f.setGender("girl"); "Hi, Kevia!" },
            AdminAction("Switch to Kevin") { f.setGender("boy"); "Hi, Kevin!" },
            AdminAction("Super squeeze fest") { it.goldenNext += 20; "The next 20 squeezes are golden!" },
            // INTEGRATION: "Kevin heart eyes" needs a showHeartEyes() hook on the stand art (StageArt.kt).
        )),
        // INTEGRATION: the original also has War / Space / Pirate / Realms / most of "Crazy" admin tabs.
        // Those poke almost entirely into other features' internals (enemies[], ship, voyage, realm/nightmare,
        // disco/upside stage classes, comet/whale sprites...) that don't exist yet on this branch. Once those
        // features land, port them here the same way: one AdminAction per original button.
    )
}

@Composable
fun CodesScreenImpl(g: GameViewModel) {
    val f = g.fun_
    var step by remember { mutableStateOf(1) }
    var codeInput by remember { mutableStateOf("") }
    var cashInput by remember { mutableStateOf("") }
    var codeWants by remember { mutableStateOf("money") }
    var msg by remember { mutableStateOf("") }
    var adminMsg by remember { mutableStateOf("") }
    var adminTab by remember { mutableStateOf(0) }

    fun tryCode() {
        if (f.tryHallCode(codeInput)) { step = 2; msg = "Code accepted!" } else msg = "Wrong code! Nice try."
    }
    fun giveCash() {
        val amt = codeInput2Long(cashInput)
        if (amt == null || amt < 1) { msg = "Type a number, like 1000."; return }
        msg = f.giveCash(codeWants, amt)
        cashInput = ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ChunkyButton("Back to lemonade", color = CARD) { g.screen = Screen.STAND }
        Spacer(Modifier.height(10.dp))
        OutlinedTitle("Secret codes")
        Spacer(Modifier.height(10.dp))

        if (step == 1) {
            Text("Know a secret code? Type it below.", fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))
            CodeField(codeInput, { codeInput = it }, "Enter code")
            Spacer(Modifier.height(8.dp))
            ChunkyButton("Enter", color = PEEL) { tryCode() }
        } else {
            Text("Pick what you want:", fontWeight = FontWeight.Black, fontSize = 14.sp)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChunkyButton("Money", color = if (codeWants == "money") PEEL else CARD) { codeWants = "money" }
                ChunkyButton("War bonds", color = if (codeWants == "bonds") PEEL else CARD) { codeWants = "bonds" }
                ChunkyButton("Both", color = if (codeWants == "both") PEEL else CARD) { codeWants = "both" }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when (codeWants) { "money" -> "How much money do you want?"; "bonds" -> "How many war bonds do you want?"; else -> "How much money AND war bonds do you want?" },
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(6.dp))
            CodeField(cashInput, { cashInput = it }, "1000")
            Spacer(Modifier.height(8.dp))
            ChunkyButton(when (codeWants) { "money" -> "Get money"; "bonds" -> "Get war bonds"; else -> "Get both" }, color = PEEL) { giveCash() }

            Spacer(Modifier.height(16.dp))
            Text("Admin panel", fontFamily = Lilita, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            val groups = adminGroups(g)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for ((i, grp) in groups.withIndex()) {
                    Text(
                        grp.title, fontWeight = FontWeight.Black, fontSize = 12.sp,
                        modifier = Modifier.clip(CircleShape).background(if (i == adminTab) PEEL else CARD)
                            .border(2.dp, LINE, CircleShape).clickable { adminTab = i }.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            for (a in groups.getOrNull(adminTab)?.actions ?: emptyList()) {
                ChunkyButton(a.label, color = CARD, modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    adminMsg = a.run(g)
                    g.save()
                }
            }
            if (adminMsg.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(adminMsg, textAlign = TextAlign.Center, fontSize = 13.sp)
            }

            Spacer(Modifier.height(16.dp))
            Text("MAX OUT", fontFamily = Lilita, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            ChunkyButton("Max EVERYTHING", color = RED, textColor = Color.White) {
                f.maxEverything()
                msg = "EVERYTHING is maxed out! Every upgrade, every shop, and the traveling shop too!"
            }
        }
        if (msg.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Text(msg, textAlign = TextAlign.Center, fontSize = 14.sp)
        }
    }
}

private fun codeInput2Long(s: String): Long? = s.filter { it.isDigit() }.toLongOrNull()

@Composable
private fun CodeField(value: String, onChange: (String) -> Unit, placeholder: String) {
    Box(
        Modifier.width(180.dp).border(3.dp, LINE, CircleShape).background(CARD, CircleShape).padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        if (value.isEmpty()) Text(placeholder, color = LINE.copy(alpha = 0.4f), fontSize = 15.sp)
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true,
            textStyle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LINE),
            cursorBrush = SolidColor(LINE),
        )
    }
}
