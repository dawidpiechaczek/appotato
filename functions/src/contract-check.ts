/**
 * Fails the build when the wire format drifts between this project and the Kotlin client.
 *
 * The two sides describe the same JSON in two languages, and nothing in either compiler can see
 * the other. This closes that gap: it reads the fixtures out of the Kotlin test — which is the
 * single source of truth for the format, because `commonTest` runs on iOS and cannot read files
 * from disk — and checks them against the two things on this side that actually decide the shape:
 * the JSON schema the model is constrained with, and the parser the handler runs on the request.
 *
 * Run with `npm run contract:check`. It is not a test framework and deliberately pulls in nothing:
 * the whole point is that it runs in CI before a deploy without any extra dependency.
 */
import { readFileSync } from "node:fs";
import { join } from "node:path";
import { RECIPES_SCHEMA } from "./claude";
import { parseIngredients, clampMaxRecipes } from "./request";

const KOTLIN_CONTRACT = join(
  __dirname,
  "..",
  "..",
  "shared",
  "recipe-source",
  "implementation",
  "src",
  "commonTest",
  "kotlin",
  "com",
  "appotato",
  "shared",
  "recipe",
  "source",
  "implementation",
  "RecipeContractTest.kt",
);

const failures: string[] = [];

function check(condition: boolean, message: string): void {
  if (!condition) failures.push(message);
}

function sameSet(actual: readonly string[], expected: readonly string[]): boolean {
  const a = [...actual].sort();
  const b = [...expected].sort();
  return a.length === b.length && a.every((value, index) => value === b[index]);
}

/**
 * Pulls a `const val NAME: String = """ ... """` literal out of the Kotlin source. Deliberately
 * strict: if the shape of that declaration changes, this throws rather than silently checking
 * nothing, which would be worse than failing.
 */
function fixture(source: string, name: string): unknown {
  const pattern = new RegExp(`const val ${name}: String = """([\\s\\S]*?)"""`);
  const match = pattern.exec(source);
  if (match?.[1] === undefined) {
    throw new Error(
      `Could not find '${name}' in ${KOTLIN_CONTRACT}. ` +
        `The contract check extracts it by name — if it was renamed or reformatted, update this script.`,
    );
  }
  return JSON.parse(match[1]) as unknown;
}

const kotlinSource = readFileSync(KOTLIN_CONTRACT, "utf8");

// --- Response: the Kotlin fixture must match the schema the model is held to -------------------

const responseFixture = fixture(kotlinSource, "RESPONSE_FIXTURE") as {
  recipes: Record<string, unknown>[];
};

check(
  sameSet(Object.keys(responseFixture), Object.keys(RECIPES_SCHEMA.properties)),
  `Response fixture keys ${JSON.stringify(Object.keys(responseFixture))} ` +
    `do not match the schema's ${JSON.stringify(Object.keys(RECIPES_SCHEMA.properties))}`,
);

const schemaRecipeKeys = Object.keys(RECIPES_SCHEMA.properties.recipes.items.properties);
const fixtureRecipe = responseFixture.recipes[0];

check(fixtureRecipe !== undefined, "Response fixture has no recipe to check");

if (fixtureRecipe !== undefined) {
  check(
    sameSet(Object.keys(fixtureRecipe), schemaRecipeKeys),
    `Recipe fixture keys ${JSON.stringify(Object.keys(fixtureRecipe))} ` +
      `do not match the schema's ${JSON.stringify(schemaRecipeKeys)}`,
  );
}

// Everything the schema declares is also required of the model, so a field the client reads but
// the schema does not force is a field that can silently arrive missing.
check(
  sameSet(RECIPES_SCHEMA.properties.recipes.items.required, schemaRecipeKeys),
  "The schema declares recipe properties it does not require; the client would read them as blank",
);

// --- Request: the function's own parser must accept what the Kotlin client sends ---------------

const requestFixture = fixture(kotlinSource, "REQUEST_FIXTURE") as Record<string, unknown>;

const parsed = parseIngredients(requestFixture.ingredients);
check(parsed !== null, "The function's parser rejected the request fixture the client sends");

if (parsed !== null) {
  check(parsed.length === 2, `Expected 2 ingredients from the fixture, parsed ${parsed.length}`);

  const resolved = parsed[0];
  check(resolved?.code === "milk", `Lost 'code' parsing the fixture: got ${resolved?.code}`);
  check(
    resolved?.displayName === "Mleko UHT 3,2%",
    `Lost 'displayName' parsing the fixture: got ${resolved?.displayName}`,
  );
  check(
    resolved?.daysUntilExpiry === 2,
    `Lost 'daysUntilExpiry' parsing the fixture: got ${resolved?.daysUntilExpiry}`,
  );

  // The client drops a null code from the body entirely (`explicitNulls = false`), so an absent
  // key has to mean the same thing as an explicit null on this side.
  const unresolved = parsed[1];
  check(
    unresolved?.code === null,
    `An absent 'code' should parse as null, got ${JSON.stringify(unresolved?.code)}`,
  );
  check(
    unresolved?.displayName === "Zestaw upominkowy",
    "An ingredient with no code lost its displayName",
  );
}

check(
  typeof requestFixture.languageTag === "string" && requestFixture.languageTag.length > 0,
  "The request fixture has no languageTag; the function requires one",
);
check(
  clampMaxRecipes(requestFixture.maxRecipes) === 3,
  `'maxRecipes' from the fixture did not survive clamping: got ${clampMaxRecipes(requestFixture.maxRecipes)}`,
);

// ----------------------------------------------------------------------------------------------

if (failures.length > 0) {
  console.error(`Wire contract drifted between functions/ and :shared:recipe-source:\n`);
  for (const failure of failures) console.error(`  - ${failure}`);
  console.error(
    `\nBoth sides describe the same JSON. Update ${KOTLIN_CONTRACT} and functions/src together.`,
  );
  process.exit(1);
}

console.log(`Wire contract OK — ${schemaRecipeKeys.length} recipe fields agreed on both sides.`);
