import assert from "node:assert/strict";
import { test } from "node:test";
import { createClient } from "./generated/client";
import { hentMellomlagretBrev, mellomlagreBrev, sendTilBeslutter } from "./generated/sdk.gen";
import type { BrevRequest } from "./generated/types.gen";

const path = { behandlingId: "00000000-0000-4000-8000-000000000001" };
const body: BrevRequest = {
  brevmal: {
    tittel: "Vedtak",
    informasjonOmBruker: { navn: "Test", fnr: "12345678901" },
    fastTekstAvslutning: [],
  },
  fritekstbolker: [],
};

test("tomt 200-svar fra mellomlagring regnes som vellykket", async () => {
  const client = createClient({
    baseUrl: "http://localhost",
    fetch: async () => new Response(null, { status: 200 }),
  });

  await assert.doesNotReject(mellomlagreBrev({ client, path, body, throwOnError: true }));
});

test("204-svar fra henting av brev beholder HTTP-statusen", async () => {
  const client = createClient({
    baseUrl: "http://localhost",
    fetch: async () => new Response(null, { status: 204 }),
  });

  const { response } = await hentMellomlagretBrev({ client, path, throwOnError: true });

  assert.equal(response.status, 204);
});

test("send til beslutter beholder JSON-svaret", async () => {
  const client = createClient({
    baseUrl: "http://localhost",
    fetch: async () => Response.json({ status: "OK" }),
  });

  const { data } = await sendTilBeslutter({ client, path, throwOnError: true });

  assert.deepEqual(data, { status: "OK" });
});

test("feilmeldingen fra backend beholdes ved avvist sending", async () => {
  const feil = {
    melding: "Behandling må være i status UTREDES for å kunne sendes til beslutter",
    status: 400,
  };
  const client = createClient({
    baseUrl: "http://localhost",
    fetch: async () => Response.json(feil, { status: 400 }),
  });

  await assert.rejects(
    sendTilBeslutter({ client, path, throwOnError: true }),
    (avvist: unknown) => {
      assert.deepEqual(avvist, feil);
      return true;
    }
  );
});

test("tomt feilsvar fra mellomlagring regnes ikke som vellykket", async () => {
  const client = createClient({
    baseUrl: "http://localhost",
    fetch: async () => new Response(null, { status: 500 }),
  });

  await assert.rejects(mellomlagreBrev({ client, path, body, throwOnError: true }));
});
