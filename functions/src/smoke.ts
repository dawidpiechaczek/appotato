/**
 * Calls the model exactly the way the deployed function does, without Firebase in the way.
 *
 * `generateRecipes` is a plain function, so the part of this backend that is actually uncertain —
 * the key, the prompt, the structured-output schema and the parsing — can be exercised without the
 * emulator, App Check or Firestore. Useful when the emulator is broken, and useful afterwards
 * whenever the prompt or the schema changes.
 *
 * Run with `npm run smoke`. It spends real tokens: roughly a third of a cent per run.
 */
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { generateRecipes, type Recipe, type RequestedIngredient } from "./claude";

const SECRET_FILE = join(__dirname, "..", ".secret.local");

/**
 * The deployed function gets this from Secret Manager, which is not reachable from here. Falls back
 * to the emulator's secret file so there is exactly one place to put the key locally.
 */
function apiKey(): string {
  const fromEnv = process.env.ANTHROPIC_API_KEY;
  if (fromEnv !== undefined && fromEnv.trim() !== "") return fromEnv.trim();

  let contents: string;
  try {
    contents = readFileSync(SECRET_FILE, "utf8");
  } catch {
    throw new Error(
      `No ANTHROPIC_API_KEY in the environment and no ${SECRET_FILE} to read it from.`,
    );
  }

  for (const line of contents.split("\n")) {
    const trimmed = line.trim();
    if (trimmed === "" || trimmed.startsWith("#")) continue;
    const separator = trimmed.indexOf("=");
    if (separator === -1) continue;
    if (trimmed.slice(0, separator).trim() !== "ANTHROPIC_API_KEY") continue;
    return trimmed
      .slice(separator + 1)
      .trim()
      .replace(/^["']|["']$/g, "");
  }

  throw new Error(`${SECRET_FILE} has no ANTHROPIC_API_KEY line.`);
}

/** Deliberately mixed: a resolved code, an unresolved one, and something already past its date. */
const PANTRY: RequestedIngredient[] = [
  { code: "milk", displayName: "Mleko UHT 3,2%", daysUntilExpiry: 2 },
  { code: "egg", displayName: "Jajka wiejskie", daysUntilExpiry: 5 },
  { code: "cheese", displayName: "Ser żółty Gouda", daysUntilExpiry: 9 },
  { code: null, displayName: "Zestaw upominkowy", daysUntilExpiry: 30 },
  { code: "chicken", displayName: "Filet z kurczaka", daysUntilExpiry: -1 },
];

const LANGUAGE = "pl";

function report(recipes: Recipe[]): string[] {
  const problems: string[] = [];
  const known = new Set(PANTRY.map((item) => item.displayName));
  const expired = PANTRY.filter((item) => item.daysUntilExpiry < 0).map((i) => i.displayName);

  if (recipes.length === 0) problems.push("No recipes came back at all.");

  recipes.forEach((recipe, index) => {
    const where = `recipe ${index + 1}`;
    if (recipe.title.trim() === "") problems.push(`${where}: empty title`);
    if (recipe.steps.length === 0) problems.push(`${where}: no steps`);

    // The client matches suggestions back to pantry rows on these strings, so an invented or
    // translated name here would break highlighting even though the recipe reads fine.
    for (const used of recipe.usesIngredients) {
      if (!known.has(used)) {
        problems.push(`${where}: uses "${used}", which is not a pantry item as sent`);
      }
    }

    // The prompt says not to build on something already past its date. This is the check that
    // matters most, because getting it wrong is a food safety problem rather than a cosmetic one.
    for (const gone of expired) {
      if (recipe.usesIngredients.includes(gone)) {
        problems.push(`${where}: builds on "${gone}", which is already expired`);
      }
    }
  });

  return problems;
}

async function main(): Promise<void> {
  console.log(`Asking for recipes in "${LANGUAGE}" from ${PANTRY.length} pantry items...\n`);

  const recipes = await generateRecipes(apiKey(), PANTRY, LANGUAGE, 3);

  recipes.forEach((recipe, index) => {
    console.log(`${index + 1}. ${recipe.title}${recipe.minutes === null ? "" : ` (${recipe.minutes} min)`}`);
    console.log(`   ${recipe.summary}`);
    console.log(`   uses:    ${recipe.usesIngredients.join(", ") || "—"}`);
    console.log(`   missing: ${recipe.missingIngredients.join(", ") || "—"}`);
    recipe.steps.forEach((step, stepIndex) => console.log(`   ${stepIndex + 1}) ${step}`));
    console.log();
  });

  const problems = report(recipes);
  if (problems.length > 0) {
    console.error("Problems:");
    for (const problem of problems) console.error(`  - ${problem}`);
    process.exit(1);
  }

  console.log(`OK — ${recipes.length} recipes, all fields populated, nothing built on expired food.`);
}

main().catch((error: unknown) => {
  // The key must never reach the log, and an SDK error can carry the request that contained it.
  const message = error instanceof Error ? error.message : String(error);
  console.error(`Failed: ${message.replace(/sk-ant-[A-Za-z0-9_-]+/g, "sk-ant-***")}`);
  process.exit(1);
});
