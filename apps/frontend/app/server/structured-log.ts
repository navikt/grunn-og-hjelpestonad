import { trace } from "@opentelemetry/api";

type LogLevel = "info" | "warn" | "error";
type LogFields = Record<string, boolean | number | string | undefined>;

export function structuredLog(level: LogLevel, event: string, fields: LogFields = {}): void {
  const spanContext = trace.getActiveSpan()?.spanContext();
  const traceFields = spanContext
    ? {
        trace_id: spanContext.traceId,
        span_id: spanContext.spanId,
      }
    : {};

  console[level](
    JSON.stringify({
      event,
      ...fields,
      ...traceFields,
    })
  );
}
