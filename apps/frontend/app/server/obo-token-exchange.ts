import { requestOboToken, validateToken } from "@navikt/oasis";

export async function exchangeTokenForBackend(token: string, audience: string): Promise<string> {
  const validationResult = await validateToken(token);

  if (validationResult.ok === false) {
    throw new Error(`Token validation feilet: ${validationResult.error.message}`);
  }

  const oboResult = await requestOboToken(token, audience);

  if (oboResult.ok === false) {
    throw new Error(`OBO token request feilet: ${oboResult.error.message}`);
  }

  return oboResult.token;
}
