package com.appotato.shared.ingredients

/**
 * One thing a recipe can be built out of, as this app names it.
 *
 * [code] is written into the database and into the key a suggestion request is cached under, so it
 * **must never change** once it has shipped — the same jar of milk has to resolve to the same code
 * whether it was scanned in Warsaw or typed in Berlin, or the cache degenerates into one entry per
 * spelling. The name the user actually sees is their own pantry row's, never this.
 *
 * There is deliberately no display name and no translation here: this type exists to be matched on,
 * not rendered.
 */
internal data class Ingredient(
    val code: String,
    /**
     * Matched as substrings of a whole Open Food Facts category tag, so `milk` also catches
     * `en:whole-milks`. Language-prefixed tags are a machine vocabulary and are always English.
     */
    val tagWords: List<String>,
    /**
     * Word **beginnings**, not whole words: `mlek` covers mleko/mleka/mleku/mleczko, and `cheese`
     * covers cheeses. Written without diacritics, because the text being matched is folded first.
     *
     * A stem must be long enough not to prefix an unrelated word — `ser` is fine (it does not start
     * `deser`), a bare `ry` would not be.
     */
    val stems: List<String>
)
