import { useEffect } from "react";
import { faro, getWebInstrumentations, initializeFaro } from "@grafana/faro-web-sdk";
import { TracingInstrumentation } from "@grafana/faro-web-tracing";

const DEFAULT_COLLECTOR_URL = "https://telemetry.nav.no/collect";

type FaroProps = {
  collectorUrl?: string;
};

const escapeRegExp = (value: string): string => value.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export default function Faro({ collectorUrl }: FaroProps) {
  useEffect(() => {
    if (faro.api) {
      return;
    }

    const apiUrl = `${window.location.origin}/api`;
    const apiUrlPattern = new RegExp(`^${escapeRegExp(apiUrl)}(?:/|$)`);
    const isLocalhost = ["localhost", "127.0.0.1"].includes(window.location.hostname);

    try {
      initializeFaro({
        url: collectorUrl ?? DEFAULT_COLLECTOR_URL,
        paused: isLocalhost,
        app: {
          name: "grunn-og-hjelpestonad-frontend",
          namespace: "grunn-og-hjelp",
        },
        instrumentations: [
          ...getWebInstrumentations(),
          new TracingInstrumentation({
            instrumentationOptions: {
              propagateTraceHeaderCorsUrls: [apiUrlPattern],
            },
          }),
        ],
      });
    } catch (error) {
      console.warn("Faro initialization failed", error);
    }
  }, [collectorUrl]);

  return null;
}
