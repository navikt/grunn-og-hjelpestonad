import { ExpressInstrumentation } from "@opentelemetry/instrumentation-express";
import { HttpInstrumentation } from "@opentelemetry/instrumentation-http";
import { UndiciInstrumentation } from "@opentelemetry/instrumentation-undici";
import { defaultResource, resourceFromAttributes } from "@opentelemetry/resources";
import { NodeSDK } from "@opentelemetry/sdk-node";
import { ATTR_SERVICE_VERSION } from "@opentelemetry/semantic-conventions";
import { structuredLog } from "./structured-log.js";

const IGNORERTE_STIER = ["/isAlive", "/isReady", "/metrics"];

const erIgnorertSti = (url: string | undefined): boolean => {
  if (!url) {
    return false;
  }
  const sti = url.split("?")[0];
  return IGNORERTE_STIER.includes(sti);
};

// Nais injiserer OTEL_EXPORTER_OTLP_ENDPOINT, OTEL_SERVICE_NAME, OTEL_RESOURCE_ATTRIBUTES m.m.
// NodeSDK leser disse selv. Lokalt finnes de ikke, og da skal vi ikke starte SDK-en.
const erAktivert = Boolean(process.env.OTEL_EXPORTER_OTLP_ENDPOINT);

if (erAktivert) {
  const sdk = new NodeSDK({
    resource: defaultResource().merge(
      resourceFromAttributes({
        [ATTR_SERVICE_VERSION]: process.env.APP_VERSION,
      })
    ),
    instrumentations: [
      new HttpInstrumentation({
        ignoreIncomingRequestHook: (request) => erIgnorertSti(request.url),
      }),
      new ExpressInstrumentation(),
      // Dekker Node sin innebygde fetch(), som brukes mot backend og texas.
      new UndiciInstrumentation(),
    ],
  });

  sdk.start();

  structuredLog("info", "otel_started", {
    endpoint: process.env.OTEL_EXPORTER_OTLP_ENDPOINT,
    protocol: process.env.OTEL_EXPORTER_OTLP_PROTOCOL,
  });

  const avslutt = () => {
    void sdk.shutdown().finally(() => process.exit(0));
  };

  process.once("SIGTERM", avslutt);
  process.once("SIGINT", avslutt);
} else {
  structuredLog("info", "otel_disabled", {
    reason: "OTEL_EXPORTER_OTLP_ENDPOINT er ikke satt",
  });
}
