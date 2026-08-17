import type { RequestedIngredient } from "./claude";

/** Bounds the tokens a single request can buy. A pantry longer than this is truncated, not refused. */
export const MAX_INGREDIENTS = 40;
export const MAX_RECIPES_CEILING = 5;
export const DEFAULT_MAX_RECIPES = 3;

export interface SuggestRequestBody {
  ingredients?: unknown;
  languageTag?: unknown;
  maxRecipes?: unknown;
}

/**
 * Kept in its own module, free of side effects, so the contract check can import it without
 * booting the Firebase admin app that `index.ts` initialises at import time.
 *
 * Every field name here is half of a wire contract whose other half is `SuggestRequestDto` in
 * :shared:recipe-source:implementation. Renaming one without the other is the failure this module
 * and `contract-check.ts` exist to make loud.
 */
export function parseIngredients(raw: unknown): RequestedIngredient[] | null {
  if (!Array.isArray(raw)) return null;
  const parsed: RequestedIngredient[] = [];
  for (const entry of raw.slice(0, MAX_INGREDIENTS)) {
    if (typeof entry !== "object" || entry === null) return null;
    const item = entry as Record<string, unknown>;
    if (typeof item.displayName !== "string" || item.displayName.trim() === "") return null;
    if (typeof item.daysUntilExpiry !== "number" || !Number.isFinite(item.daysUntilExpiry)) {
      return null;
    }
    const code = item.code;
    if (code !== null && code !== undefined && typeof code !== "string") return null;
    parsed.push({
      code: typeof code === "string" ? code : null,
      displayName: item.displayName,
      daysUntilExpiry: Math.trunc(item.daysUntilExpiry),
    });
  }
  return parsed;
}

export function clampMaxRecipes(raw: unknown): number {
  const requested = typeof raw === "number" ? Math.trunc(raw) : DEFAULT_MAX_RECIPES;
  return Math.min(Math.max(requested, 1), MAX_RECIPES_CEILING);
}
