package com.appotato.shared.ingredients

/**
 * Turns whatever the app knows about a pantry item into a stable, language-neutral ingredient code.
 *
 * Two entry points because there are two kinds of item and they carry different evidence: a scanned
 * one has Open Food Facts' machine tags, a hand-typed one has nothing but the name the user chose.
 * Both are deliberately coarse and both return `null` rather than guess — an unresolved item is
 * still sent to the recipe source under its display name, so a miss costs nothing but a colder
 * cache, while a wrong code silently suggests recipes for a food the user does not own.
 */

/**
 * Reads the source's tags most-specific-first, exactly as the pantry's category mapper does: the
 * taxonomy runs general to specific, so an apple is `en:plant-based-foods-and-beverages` before it
 * is `en:apples`, and reading left to right files it under plants.
 *
 * Within a single tag the **longest** matching word wins, which is what keeps `en:milk-chocolates`
 * out of [Ingredient] `milk` — `chocolate` is the longer claim on that tag and the truer one.
 */
public fun ingredientFromTags(tags: List<String>): String? =
    tags.asReversed().firstNotNullOfOrNull(::ingredientForTag)

/**
 * Matches the name a token at a time, so a stem only ever matches at a word boundary: `ser` finds
 * *serek* and leaves *deser* alone. The **longest** matching stem wins, which is what makes the
 * vocabulary's order irrelevant and keeps *makaron* out of `flour`, whose `maka` also prefixes it.
 *
 * The text is lower-cased and stripped of Polish diacritics first, so a user who types "smietana"
 * gets the same answer as one who types "śmietana".
 */
public fun ingredientFromName(name: String): String? {
    val tokens = tokenize(name)
    return longestMatch(Ingredient::stems) { stem ->
        tokens.any { token -> token.startsWith(stem) }
    }
}

private fun ingredientForTag(tag: String): String? =
    longestMatch(Ingredient::tagWords) { word -> word in tag }

/**
 * One pass over the vocabulary keeping the longest word that [matches]. Length stands in for
 * specificity: between two words that both match, the longer one is making the narrower claim.
 */
private fun longestMatch(words: (Ingredient) -> List<String>, matches: (String) -> Boolean): String? {
    var best: String? = null
    var bestLength = 0
    Vocabulary.forEach { ingredient ->
        words(ingredient).forEach { word ->
            if (word.length > bestLength && matches(word)) {
                best = ingredient.code
                bestLength = word.length
            }
        }
    }
    return best
}

/** Splits on anything that is not a letter or digit, so "Mleko UHT 3,2%" yields three tokens. */
private fun tokenize(text: String): List<String> = text
    .lowercase()
    .map { char -> if (char.isLetterOrDigit()) foldDiacritic(char) else ' ' }
    .joinToString(separator = "")
    .split(' ')
    .filter(String::isNotEmpty)

/**
 * Only the Polish set. `lowercase()` already handles the upper-case forms, and the stems in
 * [Vocabulary] are written folded so the two sides always meet in the same alphabet.
 */
private fun foldDiacritic(char: Char): Char = when (char) {
    'ą' -> 'a'
    'ć' -> 'c'
    'ę' -> 'e'
    'ł' -> 'l'
    'ń' -> 'n'
    'ó' -> 'o'
    'ś' -> 's'
    'ź', 'ż' -> 'z'
    else -> char
}

/**
 * Deliberately short and deliberately incomplete: it covers what people actually put an expiry date
 * on, and anything it misses resolves to `null` rather than to something close-but-wrong.
 *
 * Order carries no meaning — both matchers pick the longest match, not the first — so new entries
 * go wherever they read best. Codes, on the other hand, are stored, and are frozen once shipped.
 */
@Suppress("MaxLineLength")
internal val Vocabulary: List<Ingredient> = listOf(
    // Dairy and eggs
    // Two Polish stems, not one: the k/cz alternation means *mleko* and *mleczko* share no prefix.
    Ingredient("milk", listOf("milk"), listOf("milk", "mlek", "mlecz")),
    Ingredient("cheese", listOf("cheese"), listOf("cheese", "ser", "twarog", "mozzarell", "parmez")),
    Ingredient("yogurt", listOf("yogurt", "yoghurt"), listOf("yogurt", "yoghurt", "jogurt")),
    Ingredient("butter", listOf("butter"), listOf("butter", "maslo", "masla")),
    Ingredient("cream", listOf("cream"), listOf("cream", "smietan")),
    Ingredient("egg", listOf("egg"), listOf("egg", "jaj")),
    // Meat and fish
    Ingredient("chicken", listOf("chicken", "poultry"), listOf("chicken", "kurcz", "drob")),
    Ingredient("beef", listOf("beef"), listOf("beef", "wolow")),
    Ingredient("pork", listOf("pork"), listOf("pork", "wieprz", "schab", "karkow")),
    Ingredient("sausage", listOf("sausage"), listOf("sausage", "kielbas", "parowk")),
    Ingredient("ham", listOf("ham"), listOf("ham", "szynk")),
    Ingredient("bacon", listOf("bacon"), listOf("bacon", "boczek", "boczk")),
    Ingredient("fish", listOf("fish", "seafood"), listOf("fish", "ryba", "ryby", "rybn")),
    Ingredient("salmon", listOf("salmon"), listOf("salmon", "losos")),
    Ingredient("tuna", listOf("tuna"), listOf("tuna", "tunczyk")),
    // Vegetables
    Ingredient("tomato", listOf("tomato"), listOf("tomato", "pomidor")),
    Ingredient("potato", listOf("potato"), listOf("potato", "ziemniak", "kartofl")),
    Ingredient("onion", listOf("onion"), listOf("onion", "cebul")),
    Ingredient("garlic", listOf("garlic"), listOf("garlic", "czosn")),
    Ingredient("carrot", listOf("carrot"), listOf("carrot", "marchew", "marchw")),
    Ingredient("pepper", listOf("pepper"), listOf("pepper", "papryk")),
    Ingredient("cucumber", listOf("cucumber"), listOf("cucumber", "ogor")),
    Ingredient("cabbage", listOf("cabbage"), listOf("cabbage", "kapust")),
    Ingredient("broccoli", listOf("broccoli"), listOf("broccoli", "brokul")),
    Ingredient("spinach", listOf("spinach"), listOf("spinach", "szpinak")),
    Ingredient("mushroom", listOf("mushroom"), listOf("mushroom", "pieczar", "grzyb")),
    Ingredient("lettuce", listOf("lettuce", "salad"), listOf("lettuce", "salat")),
    Ingredient("zucchini", listOf("zucchini", "courgette"), listOf("zucchini", "cukini")),
    Ingredient("corn", listOf("corn", "maize"), listOf("corn", "kukurydz")),
    Ingredient("peas", listOf("pea"), listOf("peas", "groszek", "groch")),
    Ingredient("beans", listOf("bean"), listOf("beans", "fasol")),
    // Fruit
    Ingredient("apple", listOf("apple"), listOf("apple", "jablk")),
    Ingredient("banana", listOf("banana"), listOf("banana", "banan")),
    Ingredient("orange", listOf("orange"), listOf("orange", "pomarancz")),
    Ingredient("lemon", listOf("lemon"), listOf("lemon", "cytryn")),
    Ingredient("strawberry", listOf("strawberr"), listOf("strawberr", "truskaw")),
    Ingredient("blueberry", listOf("blueberr"), listOf("blueberr", "borowk", "jagod")),
    Ingredient("grape", listOf("grape"), listOf("grape", "winogron")),
    Ingredient("pear", listOf("pear"), listOf("pear", "grusz")),
    Ingredient("peach", listOf("peach"), listOf("peach", "brzoskwin")),
    Ingredient("avocado", listOf("avocado"), listOf("avocado", "awokado")),
    // Staples
    Ingredient("rice", listOf("rice"), listOf("rice", "ryz")),
    Ingredient("pasta", listOf("pasta", "spaghetti", "noodle"), listOf("pasta", "makaron", "spaghetti", "penne", "noodle")),
    Ingredient("bread", listOf("bread"), listOf("bread", "chleb", "bulk")),
    Ingredient("flour", listOf("flour"), listOf("flour", "maka", "maki")),
    Ingredient("oats", listOf("oat"), listOf("oats", "platk", "owsian")),
    Ingredient("tortilla", listOf("tortilla"), listOf("tortilla")),
    // Store cupboard
    Ingredient("oil", listOf("oil"), listOf("oil", "olej", "oliw")),
    Ingredient("sugar", listOf("sugar"), listOf("sugar", "cukier", "cukru")),
    Ingredient("honey", listOf("honey"), listOf("honey", "miod")),
    Ingredient("mustard", listOf("mustard"), listOf("mustard", "musztard")),
    Ingredient("ketchup", listOf("ketchup"), listOf("ketchup", "keczup")),
    Ingredient("mayonnaise", listOf("mayonnaise"), listOf("mayonnaise", "majonez")),
    Ingredient("nuts", listOf("nut"), listOf("orzech", "almond", "migdal", "walnut")),
    Ingredient("chocolate", listOf("chocolate"), listOf("chocolate", "czekolad")),
    Ingredient("tofu", listOf("tofu"), listOf("tofu")),
    Ingredient("hummus", listOf("hummus"), listOf("hummus", "humus")),
    // Drinks
    Ingredient("juice", listOf("juice"), listOf("juice", "sok")),
    Ingredient("beer", listOf("beer"), listOf("beer", "piwo", "piwa")),
    Ingredient("wine", listOf("wine"), listOf("wine", "wino", "wina")),
    Ingredient("coffee", listOf("coffee"), listOf("coffee", "kawa", "kawy")),
    Ingredient("tea", listOf("tea"), listOf("tea", "herbat"))
)
