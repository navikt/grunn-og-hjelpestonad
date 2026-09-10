import { register } from "node:module";

// Applikasjonen er ren ESM. OpenTelemetry patcher biblioteker via require-in-the-middle,
// som kun fanger CommonJS. For at import-setninger skal patches må ESM-loaderen
// registreres før noe annet lastes – derfor kjøres denne fila via `node --import`.
// Node logger en DEP0205-advarsel for module.register(). Den er forventet:
// hook.mjs er den offisielt støttede måten å registrere OpenTelemetry sin ESM-loader på.
register("@opentelemetry/instrumentation/hook.mjs", import.meta.url);

// Dynamisk import slik at SDK-en og instrumenteringene lastes etter at loaderen er aktiv.
await import("./otel.js");
