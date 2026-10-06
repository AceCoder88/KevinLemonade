package com.kevin.lemonade

// All of Kevin's dialogue, copied from the original game.
// {N} = Kevin's name, {R} = the relative being squeezed, {Rc} = same, capitalized.

data class Relative(val n: String, val drop: String, val squish: String, val after: String)

object Lines {
    val relatives = listOf(
        Relative("Grandpa", "Wait... is that my GRANDPA?!", "Oh no, GRANDPA!", "Why squeeze Grandpa?! He'll make such grumpy juice."),
        Relative("Gary", "Is that my cousin Gary?", "GARY, NOOO!", "Gary always wanted to be famous. Now he's lemonade."),
        Relative("Grandma", "GRANDMA?! Grandma, get out of there!", "Not GRANDMA!", "She was knitting me a sweater! Now she's juice."),
        Relative("Uncle Larry", "Uncle Larry?! What are you doing up there?", "Uncle Larry got SQUASHED!", "Well... at least no more of Uncle Larry's dad jokes."),
        Relative("my little brother", "Is that my little brother? He owes me five bucks!", "My brother went SPLAT!", "Guess I'm never getting my five bucks back."),
        Relative("Steve", "Oh hey, it's my neighbor Steve. Hi Steve!", "STEVE! Noooo!", "Who's gonna water my plants now, Steve?!"),
        Relative("Auntie", "Is that my aunt? She's SO sour.", "Auntie got squished!", "Auntie's gonna make the SOUREST lemonade ever."),
        Relative("my twin", "Is that my twin? We look nothing alike.", "My TWIN! Noooo!", "Now there's only one handsome lemon left. Me."),
        Relative("the dentist", "That's my dentist! Bye, dentist!", "The dentist got squished!", "Well, I guess I can skip my checkup now."),
        Relative("Mom's friend", "Is that my mom?! No wait... it's Mom's friend.", "Mom's friend went SPLOOSH!", "Mom is gonna be SO mad when she finds out."),
        Relative("Great-great-great-grandpa", "My great-great-great-grandpa?! He's SO wrinkly!", "Not Great-great-great-grandpa!", "That's some REALLY old juice. Like, vintage."),
        Relative("Coach", "Is that my soccer coach? Coach, KICK your way out!", "Coach got squeezed!", "Who's gonna yell 'HUSTLE!' at practice now?"),
        Relative("Patricia", "Oh no, it's cousin Patricia. She JUST got her peel done!", "PATRICIA!", "Her new peel was so shiny, too. What a waste."),
        Relative("my babysitter", "That's my old babysitter! She let me stay up late!", "Not my babysitter!", "Who's gonna let me stay up late now?!"),
        Relative("my best friend", "That's my best friend from lemon school!", "BUDDY, NOOO!", "We were gonna be lemon buddies forever!"),
        Relative("the kid from class", "Hey! That's the lemon who sat behind me in class!", "He got SQUEEZED!", "Honestly? He did copy my homework."),
        Relative("Doug", "Oh. It's my cousin Doug. Of course it's Doug.", "Doug went SPLAT. Classic Doug.", "Doug's juice is probably mostly seeds.")
    )
    /** sadder versions for stage 1 and 2 */
    val sadDrop = listOf(
        listOf(
            "Oh. It's {R}. Of course it is.",
            "Not {R} too...",
            "Hi, {R}. Sorry about what's gonna happen."
        ),
        listOf(
            "...{R}.",
            "They even got {R}.",
            "{Rc}... I can't watch."
        )
    )
    /** sadder versions for stage 1 and 2 */
    val sadSquish = listOf(
        listOf(
            "Bye, {R}...",
            "sploosh. There goes {R}.",
            "{Rc} is juice now. Cool. Cool cool cool."
        ),
        listOf(
            "{R}...",
            "squish. bye, {R}.",
            "There goes {R}. There goes everybody."
        )
    )
    /** sadder versions for stage 1 and 2 */
    val sadAfter = listOf(
        listOf(
            "I'll miss you, {R}.",
            "{Rc} was one of the good ones.",
            "I'm putting {R} in the family photo album. The juice section."
        ),
        listOf(
            "{Rc} is gone. Everybody's gone.",
            "I can't even cry for {R}. I'm out of juice.",
            "Is this what being a raisin feels like, {R}?"
        )
    )
    val full = listOf(
        listOf(
            "A full pitcher! ...with a little bit of {R} in it.",
            "Lemonade's ready! It tastes a little like {R}.",
            "Who wants to buy {R}? I mean... lemonade."
        ),
        listOf(
            "A whole pitcher. {Rc} is in there somewhere.",
            "Sold. Bye again, {R}."
        ),
        listOf(
            "They're drinking {R}. And they're smiling.",
            "Sold. {Rc} got sold."
        )
    )
    val poke = listOf(
        listOf(
            "Hi! I'm {N}!",
            "Hee hee! Stop poking me!",
            "Don't put ME in there!",
            "I'm the zest-iest lemon around.",
            "Pucker up, buttercup!",
            "Boop! Boop me back!",
            "I'm ticklish on my peel!",
            "Did you know I can do a backflip? I can't.",
            "Poke me again and I'll tell my cousins. Oh wait.",
            "Boop! Right back at ya.",
            "Careful, my peel is ticklish.",
            "Is it my turn to poke YOU?",
            "I'm not a button, you know!"
        ),
        listOf(
            "Please don't poke me right now.",
            "I'm having a sour day.",
            "{N} is not available. Leave a message.",
            "Poke harder. It helps a little.",
            "A hug would be better. But poking works."
        ),
        listOf(
            "...",
            "Leave me alone. I'm sad.",
            "Poke me again and I'll cry. Oh wait, I already am.",
            "...thanks for trying.",
            "Just... keep poking. Please."
        )
    )
    val hold = listOf(
        "Hey. Don't.",
        "I said DON'T press me.",
        "Seriously. Let. Go!",
        "If you squeeze me, you'll be SORRY...",
        "LAST WARNING!!!"
    )
    val letGo = listOf(
        "Phew. Thank you.",
        "Smart choice.",
        "Don't even THINK about it again.",
        "Okay. We're cool. We're cool.",
        "You scared the zest out of me!",
        "Never do that again. Pinky promise. I don't have pinkies."
    )
    val therapyTap = listOf(
        "Thanks... I needed that.", "Keep going, it's working!", "Okay, I feel a little better.",
        "Who knew poking was therapy?", "More pokes, please.", "My peel is getting yellow again!"
    )
    val dance = listOf("You found my SECRET DANCE! Wiggle wiggle!", "Dance party! Lemon style!", "I've got the moves! The lemon-y moves!")
    val outOfLemons = listOf("No more lemons! ...honestly? I'm kinda relieved.", "We're all out! Go shake the lemon tree.",
        "Out of lemons. My family is SAFE. For now.")
    val zestHello = listOf("Welcome, welcome! Everything a lemonade stand could ever need!", "Ah, my favorite customer! What'll it be today?",
        "Fresh upgrades, just unpacked! Don't tell Kevin where I get my lemons.")
    val zestBuy = listOf("Excellent choice! Excellent!", "Pleasure doing business with you!", "Ooh, that's a good one. I'd buy it myself!",
        "Sold! No refunds. Especially not to Kevin.", "Ka-ching! I love that sound.", "You've got great taste. Lemony taste.")
    val zestPoke = listOf("Hey, hands off the mustache! It took me YEARS.", "I've been selling upgrades since before Kevin was a seed.",
        "Psst... the traveling deals are the best ones. Don't tell anybody.", "My bell is for ringing, not for poking me!")

    // ---------------- Death Realm / Nightmare Realm ----------------
    /** who lives in the Death Realm: the first 4 are there from the start, then one more shows up every few pitchers */
    data class FamMember(val who: String, val plea: String, val react: String)
    val famList = listOf(
        FamMember("Gary", "Gary: Hey Kevin! Wait, why is that hand coming at m—", "GARY?! I JUST got to see you again!"),
        FamMember("Grandpa", "Grandpa: Back in my day, nobody got squeezed TWI—", "Grandpa got squeezed TWICE! That's a world record!"),
        FamMember("Mom", "Mom: Kevin, did you clean your room? What's that squeez—", "MOM?! You squeezed my MOM?!"),
        FamMember("Grandma", "Grandma: Oh, is it cookie time, dear?", "GRANDMA?! She made me cookies!"),
        FamMember("Uncle Larry", "Uncle Larry: Wanna hear a lemon joke? It's so sour—", "He never got to finish his joke!"),
        FamMember("Steve", "Steve: Hi! I'm just the neighbor! I'm not even fam—", "STEVE WASN'T EVEN FAMILY!"),
        FamMember("Auntie", "Auntie: Kevin, stand up straight! WHY is that squeez—", "Auntie's ghost juice is gonna be SO sour."),
        FamMember("Patricia", "Patricia: Don't touch my peel! I JUST got it d—", "Not again, Patricia! Her peel was SO shiny!"),
        FamMember("Doug", "Doug: Hey. It's me. Doug.", "Classic Doug. Squeezed twice. Didn't even fight it."),
        FamMember("Coach", "Coach: HUSTLE! HUSTLE! Wait, not into the squeez—", "Coach got benched. AGAIN."),
    )
    /** where each ghost floats on the 800x500 stage */
    val famSlots = listOf(
        530f to 120f, 655f to 195f, 745f to 320f, 436f to 74f, 620f to 52f,
        760f to 190f, 290f to 175f, 740f to 420f, 560f to 215f, 480f to 150f,
    )
    val crowdPrefix = listOf("Cousin", "Uncle", "Auntie", "Grandpa", "Grandma", "Great-Uncle", "Great-Aunt", "Second Cousin", "Baby", "Big")
    val crowdName = listOf("Bob", "Sue", "Lenny", "Zesty", "Peel", "Pip", "Rindy", "Citrus", "Sam", "Lou", "Mo", "Tangy", "Jo", "Squeezy", "Pulp", "Sunny", "Seedy", "Wedge")
    /** Kevin's warning when you squeeze family you shouldn't */
    val angry = listOf("That is NOT cool. That's GHOST JUICE!", "I brought you here to VISIT, not to SQUEEZE!",
        "Stop squeezing my family!", "Is \$50 really worth it?! ...Don't answer that.", "I'm warning you. I'm gonna LOSE it.")
    val realmPoke = listOf(
        "Boooo! Just kidding, it's me.", "Grandma says hi.", "Gary keeps floating through my head. Rude.",
        "The lemonade here is made of clouds.", "Uncle Larry won't stop telling dad jokes up here.", "Steve owes me a cloud.",
        "Grandma made ghost cookies. They go right through you.", "Don't squeeze my family. Seriously. Don't.",
        "Grandma's knitting me a cloud sweater.", "No squeezers up here. It's the BEST.", "Steve keeps trying to sell me ghost lemonade.",
    )
    /** lemons' lime cousins: squeezing a lime is cheering, not sad, for Kevin */
    object LimeLines {
        val drop = listOf("A LIME! Get in there, you sour pirate!", "Ooh, a lime! Not family. Squeeze away!", "Lime time! This one's for my cousins!", "Hey lime! How's the sea treating you? ...Bad, huh?")
        val squish = listOf("SQUISH! Ha! Take that, lime!", "Limeade! YES!", "That's for sinking our ships!", "Squeeze it! Squeeze it good!")
        val after = listOf("Limes don't make me sad at all. I feel GREAT!", "More limes, please! I could do this all day.", "Best. Squeeze. Ever.", "I'm getting happier with every lime!")
    }
    val noEscape = listOf(
        "There is no escape.", "Nice try.", "You squeezed me. Now you stay.", "hehehehehe",
        "I told you not to. I told you FIVE times.", "Click it all you want.",
        "I'm not a lemon anymore. I'm a NIGHTMARE.", "Want some lemonade? It's ME.", "The exit is that way. JUST KIDDING. There is no exit.",
        "Nice button. Shame it doesn't work.", "I can do this all day.", "Your lemonade stand is MY lemonade stand now.",
        "My eye is watching you. Always.", "Restart? Never heard of it.", "Red lemonade, anyone? It's fresh.",
        "You can't escape. Unless... no. Forget I said anything.",
    )
    /** dark Kevin, held down for 30 seconds in the Nightmare Realm */
    val darkLines = listOf(
        "Hey. What are you doing?", "Stop.", "I said STOP.", "Let go of me RIGHT NOW!",
        "You can't get rid of me!", "STOP IT! STOP IT! STOP IT!", "I'm warning you...",
        "Fine. Then I'll MAKE you stop!", "LET GO LET GO LET GO!!!", "No... NO... NOOOOOO!",
    )
    val darkLetGo = listOf("Hehehe. Too weak.", "That's what I thought.", "Nobody can hold on that long.", "Ha! You let go!")
    val clownTipLine = "Honk! +"

    // ---------------- Dr. Rind's therapy ----------------
    val therapyTalk = listOf(
        "Dr. Rind: So, {N}. How does the squeezer make you feel?",
        "{N}: Squeezed. Mostly squeezed.",
        "Dr. Rind: I see. And how are your cousins?",
        "{N}: They're juice now. All of them. Even Doug.",
        "Dr. Rind: Mm-hmm. Let's try some deep breaths.",
        "{N}: *breathes in* ...I smell lemonade. NOT HELPING.",
        "Dr. Rind: Picture your happy place. A big lemon tree...",
        "{N}: A tree FULL of family! ...Oh. That's actually nice.",
        "Dr. Rind: You're stronger than you think, {N}.",
        "{N}: I think... I'm going to be okay. I really am!",
    )
    val phoneLines = listOf(
        "Dr. Rind (on the phone): Deep breaths, {N}.", "Dr. Rind (on the phone): You're doing great, {N}!",
        "Dr. Rind (on the phone): Remember your happy place.", "{N}: Thanks, Dr. Rind. I feel a little better.",
    )
}
