import { createHash } from "crypto";

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
    console.log(`Cache ${cachet.token} - token utløpt`);
    return null;
  }

  const minutterIgjen = Math.round((cachet.utløperVed - nå) / ETT_MINUTT);
  console.log(
    `Cache ${cachet.token.slice(0, 10)}... - bruker cachet token (utløper om ${minutterIgjen} minutter)`
  );

  return cachet.token;
};

export const lagreTokenICache = (
  brukerToken: string,
  navIdent: string,
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

  console.log(
    `Token cachet for ${navIdent} nøkkel ${nøkkel.slice(0, 10)}... - gyldig i ${minutterGyldig} minutter`
  );
};

export const tømCache = () => {
  const størrelse = tokenCache.size;
  tokenCache.clear();
  console.log(`Token cache tømt - fjernet ${størrelse} tokens`);
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
    console.log(`Token cache cleanup - fjernet ${fjernet} utløpte tokens`);
  }
};

setInterval(ryddUtløpteTokens, FEM_MINUTTER);
