package com.kevin.lemonade

import androidx.compose.ui.graphics.Color

/** One relative who falls into the squeezer on a given world. Same text as the original's `family` arrays. */
data class SpaceFamily(val drop: String, val squish: String, val after: String)

/** A planet (or moon/sun) you can fly to and squeeze fruit on. Matches the original's `worlds` object. */
data class SpaceWorld(
    val id: String,
    val place: String,
    val fruit: String,
    val kevinName: String,
    val juice: Color,
    val price: Int,
    val sky0: Color,
    val sky1: Color,
    val ground: Color,
    val stars: Boolean = false,
    val bands: Boolean = false,
    val rings: Boolean = false,
    val swamp: Boolean = false,
    val family: List<SpaceFamily>,
    val full: List<String>,
    val poke: List<String>,
    val hello: String,
)

/** id order the "Visit all 10 worlds" mission checks against (the original's ALL_WORLDS). */
val ALL_WORLDS = listOf("sun", "moon", "mars", "jupiter", "saturn", "turtlanda", "venus", "mercury", "neptune", "pluto")

/** every world you can fly to, keyed by id. Same names, prices, and dialogue as the original. */
val SPACE_WORLDS: Map<String, SpaceWorld> = listOf(
    SpaceWorld(
        id = "sun", place = "the Sun", fruit = "pineapple", kevinName = "Pineapple Kevin",
        juice = Color(0xFFFFD54A), price = 4, sky0 = Color(0xFFFF7A1A), sky1 = Color(0xFFFFD15C), ground = Color(0xFFE2541B),
        family = listOf(
            SpaceFamily("Is that Grandpa Spiky? He's the pokiest pineapple I know!", "GRANDPA SPIKY, NO!", "He poked the squeezer on the way down. Good for him."),
            SpaceFamily("Uncle Tiki?! He was supposed to be at a luau!", "Uncle Tiki got squished mid-hula!", "Aloha, Uncle Tiki. Aloha forever."),
            SpaceFamily("That's my cousin Prickles! Prickles, roll away!", "PRICKLES!", "Prickles said he'd make it big. Now he's a whole pitcher."),
            SpaceFamily("Oh no, my sister Goldie! She JUST trimmed her crown!", "Not Goldie!", "Her crown was SO fancy. What a waste of a good haircut."),
            SpaceFamily("Is that Pizza Pete? Nobody liked him on pizza.", "Pizza Pete went SPLAT!", "Honestly? He's way better as juice than on pizza."),
            SpaceFamily("Wait, that's my dad! Dad, this is NOT the beach!", "DAD!", "Dad always said the sun was too hot. He was right."),
        ),
        full = listOf("A full pitcher of tropical relatives!", "Sun-squeezed pineapple juice! It's extra warm."),
        poke = listOf("I'm Kevin. Pineapple Kevin. The spikiest Kevin.", "Careful! I'm pointy!", "It's SO hot on the sun. My crown is melting.", "Do you put me on pizza? Be honest."),
        hello = "Hi, I'm Pineapple Kevin! Please don't squeeze my family!",
    ),
    SpaceWorld(
        id = "moon", place = "the Moon", fruit = "blueberry", kevinName = "Blueberry Kevin",
        juice = Color(0xFF5B4BC9), price = 3, sky0 = Color(0xFF05081C), sky1 = Color(0xFF1B2350), ground = Color(0xFFB9BCC6), stars = true,
        family = listOf(
            SpaceFamily("Is that my cousin Bluey? He's so tiny!", "BLUEY, NO!", "He was the bluest berry in the whole bush."),
            SpaceFamily("Grandma Muffin?! She's supposed to be IN a muffin!", "Grandma Muffin got squished!", "She always wanted to be in a muffin. Now she's in a cup."),
            SpaceFamily("That's my best friend Moonpie! Moonpie, float away!", "MOONPIE!", "There's no gravity up here... how did he fall in?!"),
            SpaceFamily("Is that baby Berry? He's only one day old!", "Not baby Berry!", "He didn't even get to see Earth."),
            SpaceFamily("Uncle Bramble?! He's the grumpiest berry ever.", "Uncle Bramble went SPLAT!", "Grumpy juice. It's probably extra sour."),
            SpaceFamily("Wait... that's my astronaut cousin, Captain Blue!", "CAPTAIN BLUE!", "One small squish for a berry. One giant pitcher!"),
        ),
        full = listOf("A full pitcher of moon-berry juice! It glows a little.", "Blueberry juice, made on the MOON. Fancy."),
        poke = listOf("I'm Blueberry Kevin. I'm small, but I'm brave.", "It's cold on the moon. Hug me.", "Boing! Low-gravity pokes!", "Don't eat me, I'm a FRIEND."),
        hello = "Hi! I'm Blueberry Kevin. Why'd you bring a squeezer to the Moon?",
    ),
    SpaceWorld(
        id = "mars", place = "Mars", fruit = "strawberry", kevinName = "Strawberry Kevin",
        juice = Color(0xFFFF4D6D), price = 3, sky0 = Color(0xFFE9A27A), sky1 = Color(0xFFC65A3A), ground = Color(0xFFA8442A),
        family = listOf(
            SpaceFamily("Is that Auntie Seedy? She has SO many seeds!", "AUNTIE SEEDY!", "I'm gonna be finding her seeds in this juice for WEEKS."),
            SpaceFamily("That's my cousin Shortcake! Shortcake, run!", "Not Shortcake!", "He wanted to be a dessert. Now he's a drink."),
            SpaceFamily("Grandpa Jam?! He's lived on Mars for 100 years!", "GRANDPA JAM!", "He survived a hundred dust storms. But not the squeezer."),
            SpaceFamily("Is that the alien strawberry? He has THREE eyes!", "The alien went SPLOOSH!", "Three eyes, one pitcher. Rest in juice, little alien."),
            SpaceFamily("That's my sister Berrylicious! Prettiest berry on Mars!", "BERRYLICIOUS, NO!", "She'd be so mad. She JUST got her leaves done."),
            SpaceFamily("Is that a red rock? No... it's Rocky the Strawberry!", "Rocky got crushed!", "I knew he wasn't a rock. Rocks don't scream."),
        ),
        full = listOf("A full pitcher of Martian strawberry juice!", "Red planet, red juice. Makes sense."),
        poke = listOf("I'm Strawberry Kevin. Red planet, red berry.", "Hey! Watch the seeds!", "Mars is so dusty. ACHOO!", "I'm the reddest Kevin. Don't squeeze me!"),
        hello = "Greetings, Earthling! I'm Strawberry Kevin. Is that a squeezer?!",
    ),
    SpaceWorld(
        id = "jupiter", place = "Jupiter", fruit = "watermelon", kevinName = "Watermelon Kevin",
        juice = Color(0xFFFF6B81), price = 5, sky0 = Color(0xFFD9A066), sky1 = Color(0xFFF2D3A2), ground = Color(0xFFB07A4A), bands = true,
        family = listOf(
            SpaceFamily("Is that Big Uncle Melvin? He's HUGE!", "UNCLE MELVIN!", "That's not a pitcher. That's a swimming pool."),
            SpaceFamily("My cousin Slice?! He's only HALF a watermelon!", "Slice got squished!", "Well... now he's NO watermelon."),
            SpaceFamily("Grandma Rind?! She's 90 years old!", "NOT GRANDMA RIND!", "Her juice is gonna taste like old-lady perfume."),
            SpaceFamily("That's Seedless Steve! He doesn't have a single seed!", "STEVE!", "At least nobody has to spit out his seeds."),
            SpaceFamily("Is that baby Melon? He's so round!", "Baby Melon, NOOO!", "He rolled right in. Babies never look where they go."),
        ),
        full = listOf("A full pitcher of watermelon juice! GIANT-sized.", "Watermelon juice! Perfect for a Jupiter summer."),
        poke = listOf("I'm Watermelon Kevin. The BIGGEST Kevin.", "Knock knock! Yep, I'm ripe.", "Jupiter has huge storms. I'm scared!", "Don't drop me, I'll split!"),
        hello = "I'm Watermelon Kevin! Everything here is BIG. Even my worries.",
    ),
    SpaceWorld(
        id = "saturn", place = "Saturn", fruit = "grape", kevinName = "Grape Kevin",
        juice = Color(0xFF7A2E8E), price = 4, sky0 = Color(0xFF2A1F4E), sky1 = Color(0xFF5A4A8E), ground = Color(0xFF8E7CB8), rings = true,
        family = listOf(
            SpaceFamily("Is that my bunch? MY WHOLE BUNCH?!", "MY BUNCH!", "I was the only one who let go of the vine in time."),
            SpaceFamily("Grandpa Raisin?! He's all wrinkly already!", "Grandpa Raisin got squished!", "There wasn't much juice in him. He's mostly wrinkles."),
            SpaceFamily("That's my cousin Vinny from the vineyard!", "VINNY!", "Vinny said he'd make it big. Now he's fancy juice."),
            SpaceFamily("Is that Princess Purple? She lives on Saturn's rings!", "Not the princess!", "The rings will never be the same without her."),
            SpaceFamily("That's my twin, Grapey! We're IDENTICAL!", "GRAPEY!", "Wait... how do they know which one of us was Grapey?"),
        ),
        full = listOf("A full pitcher of Saturn grape juice! It's SO purple.", "Grape juice from the ring planet! Fancy."),
        poke = listOf("I'm Grape Kevin. I was part of a bunch...", "Squishy! Don't do that!", "I like to roll around on Saturn's rings.", "Purple is the best color. Fight me."),
        hello = "Ahoy! I'm Grape Kevin. Please leave my bunch alone.",
    ),
    SpaceWorld(
        id = "turtlanda", place = "Turtlanda", fruit = "turtle", kevinName = "Shelby",
        juice = Color(0xFF7BC950), price = 6, sky0 = Color(0xFF7FD6C8), sky1 = Color(0xFFD8F5E8), ground = Color(0xFF4E9A4A), swamp = true,
        family = listOf(
            SpaceFamily("Is that Grandpa Shellington? He's 200 years old!", "GRANDPA SHELLINGTON, NO!", "200 years of wisdom. Now it's 200 years of juice."),
            SpaceFamily("My cousin Speedy! RUN, Speedy! ...Oh right. You can't.", "Speedy got squished!", "For once, Speedy was FAST. Fast into the squeezer."),
            SpaceFamily("Uncle Snapper?! He bites EVERYTHING!", "UNCLE SNAPPER!", "He tried to bite the squeezer. The squeezer bit back."),
            SpaceFamily("Aunt Myrtle?! She was in the middle of a nap!", "Not Aunt Myrtle!", "She always slept like a log. Now she sleeps like a drink."),
            SpaceFamily("Is that baby Pebble? He JUST hatched!", "Baby Pebble, NOOOO!", "He was so tiny. His shell was still soft."),
            SpaceFamily("That's Tank the tortoise! He's not even a turtle!", "TANK!", "Tortoise juice. I didn't know that was a thing."),
            SpaceFamily("My best friend Flipper?! He was going to the beach!", "FLIPPER!", "He never made it to the beach. Now he's in a cup."),
        ),
        full = listOf("A full pitcher of Turtlanda turtle juice! It's... green.", "Turtle juice! Slow to make, fast to sell."),
        poke = listOf("I'm Shelby the turtle. Please don't poke my shell.", "I'm hiding in my shell now. ...Not really. I'm stuck.", "Turtlanda is the best planet. Slow and peaceful.", "I have no arms. I can't stop the squeezer. I just watch.", "Smell my shell! The flowers are fresh today."),
        hello = "Hi... I'm... Shelby. Like my flowers? Wait... is that a SQUEEZER?!",
    ),
    SpaceWorld(
        id = "venus", place = "Venus", fruit = "cherry", kevinName = "Cherry Kevin",
        juice = Color(0xFFC8102E), price = 7, sky0 = Color(0xFFF7B267), sky1 = Color(0xFFF79D65), ground = Color(0xFFC8553D),
        family = listOf(
            SpaceFamily("Is that my twin brother Cherrie? We're ALWAYS together!", "CHERRIE, NOOO!", "We were a pair. Now I'm... half a pair."),
            SpaceFamily("Grandma Pit?! Watch out for her pit, squeezer!", "Grandma Pit got squished!", "Her pit was harder than the squeezer. She'd be proud."),
            SpaceFamily("That's Cousin Maraschino! He lives on top of ice cream sundaes!", "MARASCHINO!", "No more sundaes for him. Only juice."),
            SpaceFamily("Is that Uncle Stem? He's mostly stem!", "Uncle Stem went SPLAT!", "Well, at least the stem survived."),
            SpaceFamily("That's Coach Bing! He coaches the cherry soccer team!", "COACH BING!", "Who's gonna yell 'PIT IT TO WIN IT' now?"),
        ),
        full = listOf("A full pitcher of cherry juice! It's so red!", "Cherry juice from Venus! Hot AND sweet."),
        poke = listOf("I'm Cherry Kevin! There are two of us. Well, there were.", "Hey! I bruise easy!", "Venus is SO hot. I'm basically a cherry pie.", "Pit-y me, I'm stuck on Venus."),
        hello = "Hi! I'm Cherry Kevin! Welcome to Venus! It's 900 degrees. Please don't squeeze us.",
    ),
    SpaceWorld(
        id = "mercury", place = "Mercury", fruit = "pepper", kevinName = "Pepper Kevin",
        juice = Color(0xFFE8483B), price = 7, sky0 = Color(0xFF5A3A2E), sky1 = Color(0xFFA8693A), ground = Color(0xFF7A5A4A), stars = true,
        family = listOf(
            SpaceFamily("Is that Grandpa Jalapeno? He's SO spicy!", "GRANDPA JALAPENO!", "That's the spiciest juice in the whole solar system."),
            SpaceFamily("Auntie Habanero?! Nobody can handle her!", "AUNTIE HABANERO, NO!", "My eyes! The juice is burning my EYES!"),
            SpaceFamily("That's my cousin Bell! He's not spicy at all!", "BELL!", "Bell was the nice one. Mild, but nice."),
            SpaceFamily("Is that Ghost Pepper? He's the scariest pepper ever!", "GHOST PEPPER!", "Now he really IS a ghost. Too soon?"),
            SpaceFamily("That's Little Chili! He's only three days old!", "Not Little Chili!", "He was so small, but SO spicy."),
        ),
        full = listOf("A full pitcher of pepper juice! DON'T drink it!", "Spicy juice! Customers are crying. Happy tears, I think."),
        poke = listOf("I'm Pepper Kevin! Don't touch me, I'm SPICY!", "Ow! Now your finger is spicy!", "Mercury is so close to the sun. Everything's roasted.", "Hot hot HOT!"),
        hello = "Hey! I'm Pepper Kevin! Welcome to Mercury, the hottest place to be. Literally.",
    ),
    SpaceWorld(
        id = "neptune", place = "Neptune", fruit = "coconut", kevinName = "Coconut Kevin",
        juice = Color(0xFFF4F7F9), price = 8, sky0 = Color(0xFF1E3A8A), sky1 = Color(0xFF3D7BE0), ground = Color(0xFF2E5EA8), stars = true,
        family = listOf(
            SpaceFamily("Is that Grandpa Husk? He's the hairiest coconut ever!", "GRANDPA HUSK!", "All that hair, and it didn't help one bit."),
            SpaceFamily("That's my sister Cocoa! She does karate!", "COCOA, NO!", "She could break boards. But not the squeezer."),
            SpaceFamily("Is that Uncle Shell? He's SO tough!", "Uncle Shell CRACKED!", "I didn't think anything could crack him."),
            SpaceFamily("That's Baby Coco! He fell off the palm tree!", "BABY COCO!", "He fell from a tree and landed in a squeezer. Bad day."),
            SpaceFamily("Is that my best friend Milky? He's full of coconut milk!", "MILKY!", "Coconut milk and coconut juice. Two drinks in one."),
        ),
        full = listOf("A full pitcher of coconut water! So refreshing.", "Neptune coconut juice! Ice cold."),
        poke = listOf("I'm Coconut Kevin. Knock knock! It's me, I'm the coconut.", "Hey! I'm hairy, not huggy.", "Neptune is SO cold. I need a sweater. Or a beach.", "Bonk! Ow."),
        hello = "Brrr! I'm Coconut Kevin. Welcome to Neptune, the coldest beach in space.",
    ),
    SpaceWorld(
        id = "pluto", place = "Pluto", fruit = "kiwi", kevinName = "Kiwi Kevin",
        juice = Color(0xFF7BC043), price = 9, sky0 = Color(0xFF2A1F4E), sky1 = Color(0xFF6A5ACD), ground = Color(0xFFB8B0D8), stars = true,
        family = listOf(
            SpaceFamily("Is that my cousin Fuzzy? He's SO fuzzy!", "FUZZY, NO!", "Now he's not fuzzy anymore. Just juicy."),
            SpaceFamily("Grandma Kiwi?! She's the oldest kiwi on Pluto!", "GRANDMA KIWI!", "She remembered when Pluto was still a planet."),
            SpaceFamily("That's Little Seedy! He has SO many tiny seeds!", "LITTLE SEEDY!", "Hundreds of tiny seeds. Hundreds of tiny tears."),
            SpaceFamily("Is that Uncle Golden Kiwi? He's the fancy one!", "UNCLE GOLDEN!", "Fancy juice. Very fancy. Very sad."),
            SpaceFamily("That's my pen pal Kiki from Earth! She came to visit!", "KIKI!", "She came all this way... for THIS?"),
        ),
        full = listOf("A full pitcher of kiwi juice! So green!", "Pluto juice! From the planet that isn't a planet."),
        poke = listOf("I'm Kiwi Kevin. Is Pluto a planet? YES. Next question.", "Hey! My fuzz!", "It's so lonely way out here on Pluto.", "Pluto is a PLANET and I'm a KIWI. Both facts."),
        hello = "Hi! I'm Kiwi Kevin! You came all the way to Pluto? Nobody visits Pluto!",
    ),
).associateBy { it.id }

/** a body orbiting the sun on the map screen (the original's `orbit` array). Earth itself has no fruit world. */
data class OrbitBody(val id: String, val r: Float, val size: Float, val speed: Float, val startAngle: Float, val color: Color, val label: String)

val SPACE_ORBIT = listOf(
    OrbitBody("earth", 130f, 16f, 0.10f, 0.6f, Color(0xFF3D7BE0), "Earth (home)"),
    OrbitBody("mars", 190f, 13f, 0.07f, 2.4f, Color(0xFFD2553A), "Mars: strawberries"),
    OrbitBody("jupiter", 255f, 26f, 0.045f, 4.2f, Color(0xFFD9A066), "Jupiter: watermelons"),
    OrbitBody("saturn", 315f, 21f, 0.03f, 5.5f, Color(0xFFC9B27A), "Saturn: grapes"),
    OrbitBody("turtlanda", 350f, 18f, 0.022f, 1.2f, Color(0xFF4FBF7A), "Turtlanda: turtles"),
    OrbitBody("mercury", 80f, 8f, 0.16f, 3.6f, Color(0xFFA8693A), "Mercury: peppers"),
    OrbitBody("venus", 104f, 12f, 0.12f, 5.0f, Color(0xFFF7B267), "Venus: cherries"),
    OrbitBody("neptune", 378f, 17f, 0.016f, 3.9f, Color(0xFF3D7BE0), "Neptune: coconuts"),
    OrbitBody("pluto", 398f, 7f, 0.012f, 0.3f, Color(0xFFB8B0D8), "Pluto: kiwis"),
)

/** a space mission: text shown, money reward, and when it's considered done (checked against SpaceState). */
data class SpaceMission(val text: String, val reward: Long, val done: (SpaceState) -> Boolean)

val SPACE_MISSIONS = listOf(
    SpaceMission("Visit 3 different planets", 100) { it.visited.size >= 3 },
    SpaceMission("Sell 3 pitchers of space juice", 200) { it.totalSold() >= 3 },
    SpaceMission("Find the Space Station (top left!)", 150) { it.stationVisited },
    SpaceMission("Buy something at the Space Station", 250) { it.stationBought },
    SpaceMission("Visit all 10 worlds", 600) { st -> ALL_WORLDS.all { st.visited.contains(it) } },
    SpaceMission("Defeat Darth Lime at the Lime Star (bottom right)", 3000) { it.darthDefeated },
)

/** what Kevin says the first time you arrive on each world (the original's ARRIVE map). */
val SPACE_ARRIVE = mapOf(
    "sun" to "Whoa, it's HOT! Pineapple Kevin, you're sweating juice!",
    "moon" to "The Moon! One small step for a lemon...",
    "mars" to "Mars! Strawberry Kevin and me have SO much in common. Red faces, squeezed families...",
    "jupiter" to "Everything is HUGE here. Even the squeezer looks nervous.",
    "saturn" to "Saturn has rings! I want a ring. ...For Lemy.",
    "turtlanda" to "Shelby! You're so pink and pretty. I'm sorry about your friends.",
    "venus" to "Venus! Cherry Kevin comes in a PAIR. Double the Kevins!",
    "mercury" to "Mercury is SPICY. Pepper Kevin, why are you so red?!",
    "neptune" to "Neptune is freezing! Coconut Kevin needs a sweater.",
    "pluto" to "Pluto! Is it a planet? Kiwi Kevin says YES, so it's a yes.",
)

/** Kevin's idle co-pilot chatter on the map screen (the original's CHATTER). */
val SPACE_CHATTER = listOf(
    "Space is SO big. Like, really big.", "Are we there yet?", "I saw a shooting star! I wished for no more squeezers.",
    "May the juice be with you.", "I've got a good feeling about this.", "That's no moon... oh wait, yes it is. It's the moon.",
    "Do you think every planet has a Kevin? Probably.", "Fun fact: in space, nobody can hear you squeeze.", "Beep boop. That's me pretending to be a robot.",
    "I hope Lemy is okay back home.", "This is the best road trip EVER. Space trip. Whatever.",
)

/** what Kevin says sometimes after squeezing a world's fruit family (the original's SQUEEZE_TALK). */
val SPACE_SQUEEZE_TALK = listOf("Poor guy. I know how you feel, buddy.", "Every planet has a squeezer. Every. Single. One.", "Hang in there, other Kevin!", "Sorry, cousin from another planet!")

/** what Kevin says after an alien customer buys a pitcher (the original's inline lines). */
val SPACE_ALIEN_TALK = listOf("Aliens! At least they aren't humans.", "An alien customer! They have SO many eyes.", "Aliens tip way better than humans.")

/** LM-3PO, the Space Station's droid shopkeeper (the original's DROID_LINES). */
val SPACE_DROID_LINES = listOf(
    "LM-3PO: Greetings! I am fluent in over six million forms of lemonade.",
    "LM-3PO: We're doomed! ...Oh, sorry. Just a joke. Buy something!",
    "LM-3PO: The Lime Star? Oh dear. Oh dear oh dear. You'll want a Deflector shield.",
    "LM-3PO: These ARE the upgrades you're looking for.",
)
val SPACE_DROID_BUY_LINES = listOf(
    "LM-3PO: An excellent purchase! The odds of you regretting it are approximately 3,720 to 1.",
    "LM-3PO: Thank the maker! A customer!",
    "LM-3PO: Wonderful! That will serve you well against the sour side.",
)

/** Darth Lime's intro at the Lime Star (the original's DUEL_TALK; {N} becomes Kevin's name). */
val SPACE_DUEL_TALK = listOf(
    "Darth Lime" to "So, {N}... we meet at last.",
    "{N}" to "Who ARE you?!",
    "Darth Lime" to "{N}... I am your COUSIN!",
    "{N}" to "NOOOOO! ...Wait, which side of the family?",
    "Darth Lime" to "Join me, and together we will rule the galaxy as lemon and lime!",
    "{N}" to "NEVER! May the juice be with me!",
)

/** rocket paint jobs sold at the Space Station (the original's ROCKET_PAINTS). */
data class RocketPaint(val name: String, val color: Color, val cost: Long)
val SPACE_PAINTS = listOf(
    RocketPaint("Classic white", Color(0xFFE8EEF2), 0),
    RocketPaint("Lemon yellow", Color(0xFFFFE14A), 50),
    RocketPaint("Fire red", Color(0xFFFF5A5A), 50),
    RocketPaint("Ocean blue", Color(0xFF4AA8FF), 50),
    RocketPaint("Galaxy purple", Color(0xFFA06BFF), 100),
    RocketPaint("Solid gold", Color(0xFFE9B630), 250),
)

/** the Space Station's upgrade shop (the original's spaceShop / sLvl). Same ids, base, grow, max. */
data class SpaceUpgrade(val id: String, val name: String, val desc: String, val base: Double, val grow: Double, val max: Int) {
    fun cost(level: Int): Long = Math.round(base * Math.pow(grow, level.toDouble()))
}
val SPACE_SHOP = listOf(
    SpaceUpgrade("rocket", "Rocket squeezer", "Every space fruit fills the pitcher twice as much", 80.0, 4.0, 2),
    SpaceUpgrade("galaxy", "Galaxy prices", "+50% money for space juice", 60.0, 2.2, 5),
    SpaceUpgrade("robot", "Space robot", "A robot squeezes fruit for you while you visit a planet", 120.0, 2.5, 4),
    SpaceUpgrade("moonjuice", "Alien customers", "Aliens sometimes pay double for a pitcher", 90.0, 2.2, 5),
    SpaceUpgrade("hyper", "Hyperdrive juicer", "Space pitchers need 1 less fruit", 300.0, 3.0, 2),
    SpaceUpgrade("tractor", "Tractor beam", "Pulls in customers from other galaxies: +25% space money", 150.0, 2.0, 5),
    SpaceUpgrade("saber", "Lemon saber", "Your arrows and Sgt. Kevin hit 50% harder in war. Use the juice!", 500.0, 1.0, 1),
    SpaceUpgrade("shield", "Deflector shield", "Beating Darth Lime takes fewer saber swings", 200.0, 2.0, 3),
)
