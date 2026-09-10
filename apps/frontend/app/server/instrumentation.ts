import { register } from "node:module";

// Applikasjonen er ren ESM. OpenTelemetry patcher biblioteker via require-in-the-middle,
// som kun fanger CommonJS. For at import-setninger skal patches må ESM-loaderen
// registreres før noe annet lastes – derfor kjøres denne fila via `node --import`.
// Node logger en DEP0205-advarsel for module.register(). Den er forventet:
// hook.mjs er den offisielt støttede måten å registrere OpenTelemetry sin ESM-loader på.

// Uten include-liste wrapper loaderen *hver eneste* ESM-modul som lastes, også hele
// SSR-bundlen og React-treet. Det koster både minne og oppstartstid vi ikke har.
// Lista må holdes i synk med instrumenteringene i otel.ts.
const INSTRUMENTERTE_MODULER = [
  "http",
  "https",
  "express",
  "undici",
  // Instrumenteringene patcher også interne filer i pakkene, ikke bare hovedeksporten.
  /node_modules\/(express|undici)\//,
];

register("@opentelemetry/instrumentation/hook.mjs", import.meta.url, {
  data: { include: INSTRUMENTERTE_MODULER },
});

// Dynamisk import slik at SDK-en og instrumenteringene lastes etter at loaderen er aktiv.
await import("./otel.js");
