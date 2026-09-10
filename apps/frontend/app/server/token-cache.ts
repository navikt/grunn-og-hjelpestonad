import { createHash } from "crypto";
import { structuredLog } from "./structured-log.js";

interface CachetToken {
  token: string;
  utløperVed: number;
}

const tokenCache = new Map<string, CachetToken>();

const ETT_MINUTT = 60 * 1000;
const FEM_MINUTTER = 5 * ETT_MINUTT;

const lagCacheNøkkel = (brukerToken: string): string => {
  return createHash("sha256").update(brukerToken).digest("hex");
};

export const hentCachetToken = (brukerToken: string): string | null => {
  const nøkkel = lagCacheNøkkel(brukerToken);
  const cachet = tokenCache.get(nøkkel);

  if (!cachet) {
    return null;
  }

  const nå = Date.now();

  if (cachet.utløperVed <= nå) {
    tokenCache.delete(nøkkel);
    structuredLog("info", "token_cache_expired");
    return null;
  }

  return cachet.token;
};

export const lagreTokenICache = (
  brukerToken: string,
  _navIdent: string,
  oboToken: string,
  utløperOmSekunder: number
) => {
  const utløpsbuffer = FEM_MINUTTER;

  const nøkkel = lagCacheNøkkel(brukerToken);
  const utløperVed = Date.now() + utløperOmSekunder * 1000 - utløpsbuffer;

  tokenCache.set(nøkkel, {
    token: oboToken,
    utløperVed,
  });

  const minutterGyldig = Math.round((utløperVed - Date.now()) / ETT_MINUTT);

  structuredLog("info", "token_cached", {
    valid_minutes: minutterGyldig,
  });
};

export const tømCache = () => {
  const størrelse = tokenCache.size;
  tokenCache.clear();
  structuredLog("info", "token_cache_cleared", {
    removed_count: størrelse,
  });
};

export const ryddUtløpteTokens = () => {
  const nå = Date.now();
  let fjernet = 0;

  for (const [nøkkel, cachet] of tokenCache.entries()) {
    if (cachet.utløperVed <= nå) {
      tokenCache.delete(nøkkel);
      fjernet++;
    }
  }

  if (fjernet > 0) {
    structuredLog("info", "token_cache_cleanup", {
      removed_count: fjernet,
    });
  }
};

setInterval(ryddUtløpteTokens, FEM_MINUTTER);
