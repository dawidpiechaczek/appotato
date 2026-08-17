import { initializeApp } from "firebase-admin/app";
import { getAppCheck } from "firebase-admin/app-check";
import { onRequest } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import * as logger from "firebase-functions/logger";
import { cacheKey, readCached, writeCached } from "./cache";
import { generateRecipes, RecipeGenerationError } from "./claude";
import { clampMaxRecipes, parseIngredients, type SuggestRequestBody } from "./request";

initializeApp();

/**
 * Held in Secret Manager rather than in the environment or in the repo. It is the one thing here
 * worth stealing: whoever holds it spends money as us, which is also why the endpoint below refuses
 * anything without a valid App Check token.
 */
const anthropicApiKey = defineSecret("ANTHROPIC_API_KEY");

/** The app's users are in Europe and this is the closest region. */
const REGION = "europe-central2";

/**
 * A ceiling on concurrent generations, and therefore on how fast a bug or an abusive client can
 * spend. Raise it when there are enough users to need it, not before.
 */
const MAX_INSTANCES = 10;

/**
 * The only thing standing between this endpoint and anyone with `curl`, because the app has no user
 * accounts to authenticate against. It is enforced here rather than declared on the function so the
 * emulator stays usable without a real attestation.
 */
async function isAttested(token: string | undefined): Promise<boolean> {
  if (process.env.FUNCTIONS_EMULATOR === "true") return true;
  if (token === undefined) return false;
  try {
    await getAppCheck().verifyToken(token);
    return true;
  } catch {
    return false;
  }
}

export const suggestRecipes = onRequest(
  {
    region: REGION,
    secrets: [anthropicApiKey],
    maxInstances: MAX_INSTANCES,
    cors: false,
  },
  async (request, response) => {
    if (request.method !== "POST") {
      response.status(405).json({ error: "method_not_allowed" });
      return;
    }

    const appCheckToken = request.get("X-Firebase-AppCheck");
    if (!(await isAttested(appCheckToken))) {
      response.status(401).json({ error: "unauthenticated" });
      return;
    }

    const body = (request.body ?? {}) as SuggestRequestBody;
    const ingredients = parseIngredients(body.ingredients);
    const languageTag = typeof body.languageTag === "string" ? body.languageTag : null;
    if (ingredients === null || languageTag === null) {
      response.status(400).json({ error: "invalid_request" });
      return;
    }

    // An empty pantry is a real answer, not an error, and it is not worth a model call.
    if (ingredients.length === 0) {
      response.status(200).json({ recipes: [] });
      return;
    }

    const maxRecipes = clampMaxRecipes(body.maxRecipes);

    const key = cacheKey(ingredients, languageTag, maxRecipes);
    const cached = await readCached(key);
    if (cached !== null) {
      response.status(200).json({ recipes: cached });
      return;
    }

    try {
      const recipes = await generateRecipes(
        anthropicApiKey.value(),
        ingredients,
        languageTag,
        maxRecipes,
      );
      await writeCached(key, recipes);
      response.status(200).json({ recipes });
    } catch (error) {
      // Logged in full on our side, reduced to a code on the way out: the upstream message can
      // carry request details, and the client can do nothing with them anyway.
      if (error instanceof RecipeGenerationError) {
        logger.warn("recipe generation did not produce an answer", { reason: error.reason });
        response.status(502).json({ error: "generation_failed" });
        return;
      }
      logger.error("recipe generation threw", { error });
      response.status(502).json({ error: "upstream_unavailable" });
    }
  },
);
