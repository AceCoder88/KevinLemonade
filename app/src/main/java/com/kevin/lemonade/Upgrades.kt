package com.kevin.lemonade

import kotlin.math.pow
import kotlin.math.roundToLong

enum class Cat(val title: String, val note: String) {
    MONEY("Money makers", "Sell every pitcher for more"),
    SQUEEZE("Squeezing", "Faster, stronger, and automatic squeezing"),
    TOOLS("Tools", "Each tool unlocks something new to do"),
    LUCK("Luck", "Golden and diamond lemons, and winning more"),
    LEMONS("Lemons", "Get more lemons without catching them"),
    HAPPY("Kevin's happiness", "Keep Kevin smiling"),
    ADVANCED("Advanced upgrades", "Big, pricey, powerful"),
    THERAPY("Therapy", "Dr. Rind's best stuff, and maybe even love"),
    SPOOKY("Spooky upgrades", "Only sold in the Death Realm. They help in the Death Realm and the Nightmare Realm."),
}

/** {N} in a name/desc becomes Kevin's name. Numbers match the original game exactly. */
data class Upgrade(
    val id: String,
    val cat: Cat,
    val name: String,
    val desc: String,
    val base: Double,
    val grow: Double,
    val max: Int,
    val say: String,
) {
    /**
     * [discount] is Mr. Zest's mustache discount (the original's
     * `wearingSet('mustache') ? 0.7 : wearing('mustache') ? 0.9 : 1`), passed in by the
     * shop via [ClosetState.mustacheDiscount]. Defaults to 1.0 so existing call sites
     * (Game.kt's buy()) keep compiling; see Shop.kt's report for what buy() must do
     * to actually charge this same discounted price.
     */
    fun cost(level: Int, discount: Double = 1.0): Long = (base * grow.pow(level) * discount).roundToLong()
}

object Upgrades {
    val all = listOf(
        // Money makers
        Upgrade("sugar", Cat.MONEY, "Sugar", "+\$1 for every pitcher", 4.0, 1.6, 20, "Sweet. Literally."),
        Upgrade("ice", Cat.MONEY, "Ice cubes", "Doubles the price of lemonade", 60.0, 3.0, 4, "Ice cold. Just like your heart."),
        Upgrade("sign", Cat.MONEY, "Fancy stand sign", "+25% money for every pitcher", 20.0, 2.0, 4, "'Fresh-squeezed family.' Catchy. I hate it."),
        Upgrade("tips", Cat.MONEY, "Tip jar", "Customers sometimes leave a big tip", 15.0, 1.9, 5, "Tips?! For MY cousins?!"),
        Upgrade("billboard", Cat.MONEY, "Billboard", "Earn money every few seconds, even while you're away", 60.0, 2.3, 5, "There's a giant me on a billboard. I look GREAT."),
        Upgrade("zest", Cat.MONEY, "Lemon zest", "+\$1 base price for every pitcher", 8.0, 1.45, 15, "Zest? That's my cousin's PEEL you're grating!"),
        Upgrade("awning", Cat.MONEY, "Striped awning", "+10% money for every pitcher", 50.0, 2.0, 5, "Ooh, shade! Fancy. My family would have loved this."),
        Upgrade("truck", Cat.MONEY, "Lemonade truck", "A truck sells lemonade around town: +\$5 for every pitcher", 200.0, 1.8, 5, "A TRUCK?! Honk honk! Fresh lemonade!"),
        Upgrade("bigcups", Cat.MONEY, "Bigger cups", "+15% money for every pitcher", 45.0, 1.8, 5, "Bigger cups = more juice = more... cousins. Hmm."),
        Upgrade("neon", Cat.MONEY, "Neon sign", "A glowing sign: +20% money for every pitcher", 120.0, 1.9, 5, "Ooh, it GLOWS! Everyone can see my shame from SPACE."),
        Upgrade("register", Cat.MONEY, "Golden cash register", "+\$10 for every pitcher", 300.0, 1.9, 5, "Ka-CHING! That's a fancy register."),
        Upgrade("vip", Cat.MONEY, "VIP customers", "Sometimes a VIP buys a pitcher for 5 times the price", 250.0, 2.0, 5, "VIPs?! Do I have to wear a tie?"),

        // Squeezing
        Upgrade("multisq", Cat.SQUEEZE, "Mega hopper", "Your squeezer squeezes 1 more lemon at a time (uses more lemons, makes more juice)", 40.0, 2.0, 4, "A mega hopper! Now I can squeeze my relatives in BULK. ...Yay?"),
        Upgrade("arms", Cat.SQUEEZE, "Strong arms", "Cranking the squeezer by hand takes fewer taps", 6.0, 1.7, 4, "Look at those muscles! Crank crank crank!"),
        Upgrade("juicy", Cat.SQUEEZE, "Juicier lemons", "1 less lemon to fill a pitcher", 10.0, 2.4, 4, "Juicier cousins?! That's WORSE!"),
        Upgrade("fast", Cat.SQUEEZE, "Faster squeezer", "Squeezes 30% faster", 6.0, 1.8, 5, "Faster?! Well, at least it's quick."),
        Upgrade("auto", Cat.SQUEEZE, "Auto-squeezer", "Squeezes lemons by itself", 25.0, 2.0, 5, "A robot that squishes lemons. Great. Love that."),
        Upgrade("press", Cat.SQUEEZE, "Squeezer upgrade", "", 30.0, 5.0, 2, "Ooh, shiny. ...Wait, is that BETTER at squeezing?!"),
        Upgrade("lucky", Cat.SQUEEZE, "Lucky lemons", "Sometimes a squeeze doesn't use up a lemon", 40.0, 2.0, 5, "A lemon that came BACK? Lucky cousin!"),
        Upgrade("turbo", Cat.SQUEEZE, "Turbo plunger", "Squeezing goes 15% faster", 60.0, 1.9, 5, "VROOM! That plunger is FAST now!"),
        Upgrade("doubleplunge", Cat.SQUEEZE, "Double plunger", "Sometimes one squeeze fills the pitcher twice", 150.0, 2.0, 5, "TWO plungers?! That's twice the squish!"),
        Upgrade("hydraulic", Cat.SQUEEZE, "Hydraulic auto-squeezer", "The auto-squeezer works 25% faster", 120.0, 1.9, 5, "Pssshhh! Hydraulic power!"),

        // Tools (only the motor so far; the others unlock modes that aren't ported yet)
        Upgrade("motor", Cat.TOOLS, "Electric motor", "No more cranking by hand! One tap squeezes a whole lemon, and it unlocks the auto-squeezer", 25.0, 1.0, 1, "A MOTOR?! My arms are SAVED! ...Wait, I don't have to crank. YOU had to crank."),

        // Luck
        Upgrade("goldluck", Cat.LUCK, "Golden luck", "More golden lemons (each one is worth 10 pitchers!)", 150.0, 1.9, 5, "Golden luck! Everything's shining!"),
        Upgrade("diamondluck", Cat.LUCK, "Diamond luck", "Super rare DIAMOND lemons can drop in. Each one is worth 30 pitchers!", 400.0, 2.1, 5, "Diamond lemons?! Those exist?!"),
        Upgrade("tipluck", Cat.LUCK, "Lucky tips", "Customers leave tips more often", 70.0, 1.8, 5, "Lucky tips! The tip jar is going to overflow!"),
        Upgrade("vipluck", Cat.LUCK, "Lucky VIPs", "VIP customers (who pay 5 times the price) show up more often", 180.0, 1.9, 5, "More VIPs! Should I start wearing a bow tie?"),

        // Lemons
        Upgrade("farm", Cat.LEMONS, "Lemon farm", "Grows a free lemon every few seconds", 35.0, 2.2, 5, "You're GROWING more of my family?! That's... kinda nice."),
        Upgrade("delivery", Cat.LEMONS, "Lemon delivery", "A delivery truck brings 5 lemons every 30 seconds", 80.0, 2.0, 5, "Lemon delivery? More family coming by MAIL?!"),
        Upgrade("greenhouse", Cat.LEMONS, "Lemon greenhouse", "Your lemon farm grows lemons 50% faster", 100.0, 1.9, 4, "A greenhouse! It's like a spa for baby lemons."),
        Upgrade("lemonrain", Cat.LEMONS, "Lemon rain", "Every 90 seconds, lemons rain from the sky (10 per level)", 150.0, 2.0, 5, "It's raining... my RELATIVES?!"),

        // Kevin's happiness
        Upgrade("music", Cat.HAPPY, "Happy music", "{N} cheers up a little every time a pitcher sells", 25.0, 2.0, 5, "Ooh, a jam! ...Not THAT kind of jam. Please."),
        Upgrade("chair", Cat.HAPPY, "Comfy chair for {N}", "{N} gets sad 10% slower", 30.0, 1.8, 5, "A comfy chair! Now I can watch in COMFORT. ...Great."),
        Upgrade("therapy", Cat.HAPPY, "Therapy for {N}", "{N} stays happy for longer", 6.0, 2.0, 5, "Thanks. My therapist says I should talk about my cousins."),
        Upgrade("puppy", Cat.HAPPY, "A puppy for {N}", "{N} pets his puppy and slowly cheers up all by himself", 90.0, 2.0, 3, "A PUPPY! Who's a good boy? YOU are!"),
        Upgrade("nap", Cat.HAPPY, "Nap time", "Every minute, {N} takes a quick nap and wakes up a little happier", 70.0, 1.9, 4, "Naps? I LOVE naps. Zzz..."),
        Upgrade("games", Cat.HAPPY, "Video games for {N}", "{N} plays games and gets sad 15% slower", 80.0, 1.8, 5, "Video games! I'm gonna beat the high score!"),
        Upgrade("hugs", Cat.HAPPY, "Hug machine", "Every 20 seconds, {N} gets a big hug and cheers up", 100.0, 1.9, 5, "A HUG MACHINE?! ...Okay, this is the best day ever."),

        // Advanced
        Upgrade("party", Cat.ADVANCED, "Lemonade party", "Every 3 minutes there's a party: 30 seconds of TRIPLE money", 1500.0, 2.5, 3, "A PARTY! Wait, what are we drinking at this party...?"),
        Upgrade("factory", Cat.ADVANCED, "Lemonade factory", "A factory sells pitchers for you all by itself, no lemons needed", 1000.0, 2.5, 5, "A FACTORY?! How many relatives does that thing use?!"),
        Upgrade("bank", Cat.ADVANCED, "Lemon bank", "Your money grows: +1% every 10 seconds", 2000.0, 3.0, 3, "Money in a BANK? Responsible. I respect it."),
        Upgrade("fanclub", Cat.ADVANCED, "{N}'s fan club", "While {N} is happy, fans pay you money every few seconds", 600.0, 2.2, 5, "I have FANS?! Hi fans! Please don't squeeze me!"),
        Upgrade("golden", Cat.ADVANCED, "Golden lemons", "Sometimes a golden lemon falls in. Worth 10 pitchers!", 250.0, 2.5, 5, "Golden cousins? Does my family have... RICH relatives?"),
        Upgrade("franchise", Cat.ADVANCED, "Lemonade franchise", "Open another stand in town: +50% money", 500.0, 3.0, 5, "There's ANOTHER stand?! Is there another ME?!"),
        Upgrade("thappy", Cat.THERAPY, "Longer happiness", "After therapy, {N} stays happy 30 seconds longer", 60.0, 1.8, 6, "More happy time?! I'll take ALL the happy time!"),
        Upgrade("tquick", Cat.THERAPY, "Quick sessions", "Therapy takes 8 seconds less, so {N} comes back sooner", 50.0, 1.8, 5, "Shorter sessions! Dr. Rind talks fast now."),
        Upgrade("lottopass", Cat.TOOLS, "A lottery booth", "Unlocks the LEMON LOTTERY: scratch cards with big prizes", 50.0, 1.0, 1, "A lottery booth! I feel lucky!"),
        Upgrade("racepass", Cat.TOOLS, "A race track", "Unlocks the LEMON RACES: bet on the fastest lemon", 75.0, 1.0, 1, "A race track! Go, little lemons, GO!"),
        Upgrade("clover", Cat.LUCK, "Four-leaf clover", "+4% luck: lottery tickets win more and your race lemon runs faster", 60.0, 1.9, 5, "A four-leaf clover! I'm feeling lucky!"),
        Upgrade("horseshoe", Cat.LUCK, "Lucky horseshoe", "+4% luck: lottery tickets win more and your race lemon runs faster", 90.0, 1.9, 5, "A lucky horseshoe! Where's the horse? Oh wait, the horse is fine."),
        Upgrade("dodgeluck", Cat.LUCK, "Lucky dodge", "Sometimes an attack does NO damage: your wall, soldiers, miners, and pirate ships get lucky", 200.0, 2.0, 5, "Lucky dodge! The bugs keep missing! Ha!"),
        Upgrade("critluck", Cat.LUCK, "Lucky arrows", "Critical hits in war (triple damage) happen more often", 160.0, 1.9, 5, "Lucky arrows! Bullseye every time!"),
        Upgrade("oreluck", Cat.LUCK, "Lucky dirt", "More ores when you dig a new spot", 120.0, 1.9, 5, "Lucky dirt! Shiny stuff everywhere down there!"),
        Upgrade("rabbitfoot", Cat.LUCK, "Lucky rabbit's foot", "+4% luck (a fake one, no rabbits were hurt!)", 120.0, 1.9, 5, "Don't worry, it's a FAKE rabbit's foot. It's made of fluff!"),
        Upgrade("wand", Cat.TOOLS, "A magic wand", "Unlocks the WIZARD TOWER: cast spells and brew potions", 120.0, 1.0, 1, "A magic wand! Abra-ca-LEMON-bra!"),
        Upgrade("saddle", Cat.TOOLS, "A saddle", "Unlocks the HORSE RIDE: gallop across the Wild West", 100.0, 1.0, 1, "A saddle! Giddy-up! ...Wait, where's the horse? Oh, there it is!"),
        Upgrade("bow", Cat.TOOLS, "A bow and arrows", "Unlocks WAR: defend the wall from bugs, rats and bosses", 40.0, 1.0, 1, "A bow?! I'm gonna be a lemon archer! Pew pew!"),
        Upgrade("pager", Cat.TOOLS, "Dr. Rind's pager", "Unlocks THERAPY: page Dr. Rind whenever Kevin needs to talk", 30.0, 1.0, 1, "Dr. Rind's pager! Now I can call him whenever I'm sad."),
        Upgrade("telescope", Cat.TOOLS, "A telescope", "Unlocks SPACE: tap the sun to fly to the planets", 100.0, 1.0, 1, "A telescope! Whoa, I can see Mars! And a STRAWBERRY?!"),
        Upgrade("map", Cat.TOOLS, "A treasure map", "Unlocks being a PIRATE: the pirate ship in the war will take you sailing", 150.0, 1.0, 1, "A treasure map! X marks the... limes?"),
        Upgrade("closetkey", Cat.TOOLS, "A closet key", "Unlocks KEVIN'S CLOSET: outfits with special powers", 30.0, 1.0, 1, "The key to my closet! Time to get FABULOUS."),
        Upgrade("shovel", Cat.TOOLS, "A shovel", "Unlocks DIGGING: a hole appears next to the stand. Dig for ores to sell!", 200.0, 1.0, 1, "A shovel! I'm gonna dig to the center of the Earth! ...Wait, what's down there?"),
        Upgrade("girlfriend", Cat.THERAPY, "A girlfriend: Lemy", "{N} gets a girlfriend named Lemy, a lemon with a pink bow. She stays by his side and he makes DOUBLE money", 100.0, 1.0, 1, "Lemy?! She's... PERFECT. My heart is going boing-boing!"),
        Upgrade("tchair", Cat.THERAPY, "Therapy on the go", "{N} gets a therapy chair at the stand. Dr. Rind calls him and slowly cheers him up", 150.0, 2.0, 3, "A therapy chair! And Dr. Rind is on speakerphone! Hi, Dr. Rind!"),
        Upgrade("squeezers", Cat.ADVANCED, "Extra squeezer", "Another squeezer that squeezes lemons all by itself (uses lemons)", 150.0, 3.0, 3, "MORE squeezers?! How many squeezers does one stand NEED?!"),
        Upgrade("ghostpay", Cat.SPOOKY, "Ghost juicer", "Squeezing a ghost in the Death Realm pays \$25 more", 60.0, 2.0, 5, "A machine JUST for squeezing ghosts?! That's... specific."),
        Upgrade("pass", Cat.SPOOKY, "Death Realm pass", "Visiting the Death Realm costs \$2 less", 30.0, 2.0, 4, "A pass to visit my family? That's actually sweet."),
        Upgrade("reunion", Cat.SPOOKY, "Family reunion", "New relatives show up in the Death Realm faster", 80.0, 3.0, 2, "MORE family in the Death Realm? That means... oh no."),
        Upgrade("snacks", Cat.SPOOKY, "Cloud snacks", "Visits to the Death Realm cheer {N} up for longer", 40.0, 2.0, 3, "Cloud snacks with Grandma! These are SO fluffy."),
        Upgrade("nightlight", Cat.SPOOKY, "Night light", "Escaping the Nightmare Realm takes 5 seconds less holding", 50.0, 2.0, 4, "A night light! Dark Kevin hates night lights."),
        Upgrade("loot", Cat.SPOOKY, "Nightmare loot", "Escaping the Nightmare Realm gives \$100 more", 70.0, 2.0, 5, "Treasure from the Nightmare Realm? I'm not touching it."),
        Upgrade("brave", Cat.SPOOKY, "Brave heart", "{N} takes 10 seconds longer to turn evil when he is all gray", 40.0, 2.0, 4, "I'm brave now. ...I'm still a little scared of the dark."),
    )

    val pressDesc = listOf(
        "Gold squeezer: every lemon fills twice as much",
        "Diamond squeezer: every lemon fills 3 times as much",
        "Ruby squeezer: every lemon fills 4 times as much",
    )

    /** These only show up after you buy the motor. */
    val needMotor = setOf("auto", "hydraulic")
}

data class LemonUpgrade(val id: String, val name: String, val desc: String, val base: Double, val grow: Double, val max: Int) {
    fun cost(level: Int): Long = (base * grow.pow(level)).roundToLong()
}

object LemonUpgrades {
    val all = listOf(
        LemonUpgrade("cap", "Lemon storage", "Hold 20 more lemons", 10.0, 1.5, 9),
        LemonUpgrade("basket", "Bigger basket", "A wider basket catches more", 8.0, 1.7, 5),
        LemonUpgrade("shake", "Shake harder", "More lemons fall per shake", 12.0, 1.8, 6),
        LemonUpgrade("helper", "Hire a helper", "Guards their own spot under the tree and shakes down 4 extra lemons every round", 25.0, 2.2, 4),
        LemonUpgrade("hspeed", "Faster helpers", "Helpers run faster", 20.0, 1.8, 5),
        LemonUpgrade("magnet", "Lemon magnet", "Falling lemons drift toward your basket", 30.0, 2.0, 5),
        LemonUpgrade("double", "Double catch", "Sometimes a caught lemon counts as 2", 35.0, 2.0, 5),
        LemonUpgrade("autoshake", "Self-shaking tree", "The tree shakes itself when a round ends", 60.0, 1.0, 1),
    )
}
