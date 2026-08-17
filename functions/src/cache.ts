import { createHash } from "node:crypto";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import type { Recipe, RequestedIngredient } from "./claude";

const COLLECTION = "recipeSuggestions";

/**
 * Long enough that a pantry which barely changes costs one generation a week, short enough that the
 * same user is not handed the identical three recipes forever.
 */
const TTL_DAYS = 7;

/**
 * Two pantries holding the same foods get the same answer, and pantries overlap heavily — milk,
 * eggs, cheese. That overlap is the whole cost argument for generating rather than licensing, so
 * the key is built to collide on purpose.
 *
 * It is built from resolved codes where there is one and the lower-cased display name where there
 * is not, sorted so order cannot matter, and it deliberately leaves out the expiry dates: caching
 * per day would defeat the point, and which of two items expires first rarely changes the dish.
 */
export function cacheKey(
  ingredients: readonly RequestedIngredient[],
  languageTag: string,
  maxRecipes: number,
): string {
  const parts = ingredients
    .map((item) => item.code ?? item.displayName.trim().toLowerCase())
    .sort();
  const payload = JSON.stringify([languageTag, maxRecipes, parts]);
  return createHash("sha256").update(payload).digest("hex");
}

interface CachedEntry {
  readonly recipes: Recipe[];
  readonly expiresAt: Timestamp;
}

/**
 * A cache miss and a broken cache are the same thing to the caller: generate. A read that throws
 * must never fail the request — the answer is still obtainable, just not for free.
 */
export async function readCached(key: string): Promise<Recipe[] | null> {
  try {
    const snapshot = await getFirestore().collection(COLLECTION).doc(key).get();
    const entry = snapshot.data() as CachedEntry | undefined;
    if (entry === undefined) return null;
    if (entry.expiresAt.toMillis() <= Date.now()) return null;
    return entry.recipes;
  } catch {
    return null;
  }
}

/** Best effort for the same reason: a write that fails costs the next caller a generation. */
export async function writeCached(key: string, recipes: Recipe[]): Promise<void> {
  const expiresAt = Timestamp.fromMillis(Date.now() + TTL_DAYS * 24 * 60 * 60 * 1000);
  try {
    await getFirestore().collection(COLLECTION).doc(key).set({ recipes, expiresAt });
  } catch {
    // Deliberately swallowed — see above.
  }
}
