import type { Response } from "express";
import { trace } from "@opentelemetry/api";

export function setServerTimingHeader(response: Response): void {
  const activeSpan = trace.getActiveSpan();

  if (!activeSpan) {
    return;
  }

  const { traceId, spanId, traceFlags } = activeSpan.spanContext();
  const traceparent = `00-${traceId}-${spanId}-${traceFlags.toString(16).padStart(2, "0")}`;

  response.setHeader("Server-Timing", `traceparent;desc="${traceparent}"`);
}
