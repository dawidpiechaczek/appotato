import Anthropic from "@anthropic-ai/sdk";

/**
 * Haiku is the right tier here and the cost case rests on it: one suggestion round trip is roughly
 * 600 tokens in and 1200 out, which is fractions of a cent. A larger model buys nothing a recipe
 * suggestion can use.
 *
 * Note it takes neither `effort` nor adaptive `thinking` — passing either is an error on this
 * model — so the request below deliberately sets neither.
 */
const MODEL = "claude-haiku-4-5";

/** Generous enough for three recipes with steps, low enough to bound a runaway generation. */
const MAX_TOKENS = 4096;

export interface RequestedIngredient {
  readonly code: string | null;
  readonly displayName: string;
  readonly daysUntilExpiry: number;
}

export interface Recipe {
  readonly title: string;
  readonly summary: string;
  readonly usesIngredients: string[];
  readonly missingIngredients: string[];
  readonly steps: string[];
  readonly minutes: number | null;
}

/**
 * Structured outputs rather than "reply with JSON and mean it": the shape is enforced by the API,
 * so there is no brace-counting, no repair pass and no retry loop here.
 *
 * The schema stays inside what structured outputs actually support — no `minItems`, no `minimum`,
 * and `anyOf` rather than a `["integer", "null"]` type union. Every object sets
 * `additionalProperties: false`, which is required rather than optional.
 */
export const RECIPES_SCHEMA = {
  type: "object",
  properties: {
    recipes: {
      type: "array",
      items: {
        type: "object",
        properties: {
          title: { type: "string" },
          summary: { type: "string" },
          usesIngredients: { type: "array", items: { type: "string" } },
          missingIngredients: { type: "array", items: { type: "string" } },
          steps: { type: "array", items: { type: "string" } },
          minutes: { anyOf: [{ type: "integer" }, { type: "null" }] },
        },
        required: [
          "title",
          "summary",
          "usesIngredients",
          "missingIngredients",
          "steps",
          "minutes",
        ],
        additionalProperties: false,
      },
    },
  },
  required: ["recipes"],
  additionalProperties: false,
} as const;

const SYSTEM_PROMPT = [
  "You suggest home recipes built around food that is about to go off.",
  "",
  "Rules:",
  "- Write everything — titles, summaries, steps, ingredient names — in the requested language.",
  "  The pantry names may be in a different language than the one requested; translate them.",
  "- Build each recipe around the items expiring soonest. An item whose daysUntilExpiry is",
  "  negative is already past its date: do not build a recipe around it and do not tell the user",
  "  to eat it. Ignore it.",
  "- In usesIngredients, echo the pantry item's displayName exactly as it was given to you, so the",
  "  caller can match it back. Anything the user would have to buy goes in missingIngredients",
  "  instead, named in the requested language.",
  "- Assume ordinary staples (salt, pepper, water, cooking oil) are on hand and do not list them",
  "  as missing.",
  "- Do not claim the user has an ingredient that was not in the list.",
  "- Keep steps short and in order. Give minutes as a whole number, or null if you would be",
  "  guessing.",
].join("\n");

function userPrompt(
  ingredients: readonly RequestedIngredient[],
  languageTag: string,
  maxRecipes: number,
): string {
  const lines = ingredients.map((item) => {
    const when =
      item.daysUntilExpiry < 0
        ? `${-item.daysUntilExpiry} days past its date`
        : `expires in ${item.daysUntilExpiry} days`;
    // The code is sent alongside the name so two spellings of one food read as one thing.
    const code = item.code === null ? "unrecognised" : item.code;
    return `- ${item.displayName} (${code}, ${when})`;
  });

  return [
    `Language for the response: ${languageTag}`,
    `Suggest at most ${maxRecipes} recipes.`,
    "",
    "Pantry:",
    ...lines,
  ].join("\n");
}

/** Raised for anything the caller should see as "the question could not be answered". */
export class RecipeGenerationError extends Error {
  constructor(public readonly reason: string) {
    super(reason);
    this.name = "RecipeGenerationError";
  }
}

export async function generateRecipes(
  apiKey: string,
  ingredients: readonly RequestedIngredient[],
  languageTag: string,
  maxRecipes: number,
): Promise<Recipe[]> {
  const client = new Anthropic({ apiKey });

  const response = await client.messages.create({
    model: MODEL,
    max_tokens: MAX_TOKENS,
    system: SYSTEM_PROMPT,
    output_config: { format: { type: "json_schema", schema: RECIPES_SCHEMA } },
    messages: [
      { role: "user", content: userPrompt(ingredients, languageTag, maxRecipes) },
    ],
  });

  // Both of these mean the body is not the schema-shaped JSON the caller is owed, and neither is
  // worth retrying with the same input: a refusal will refuse again, and a truncated generation
  // needs a smaller pantry, not another attempt.
  if (response.stop_reason === "refusal") {
    throw new RecipeGenerationError("refused");
  }
  if (response.stop_reason === "max_tokens") {
    throw new RecipeGenerationError("truncated");
  }

  const text = response.content.find((block) => block.type === "text");
  if (text === undefined || text.type !== "text") {
    throw new RecipeGenerationError("empty");
  }

  const parsed = JSON.parse(text.text) as { recipes?: Recipe[] };
  return parsed.recipes ?? [];
}
