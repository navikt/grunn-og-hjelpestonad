const HENDELSE_NAVN = "tilgang:mangler-tilgang";

export const varsleManglerTilgang = () => {
  window.dispatchEvent(new Event(HENDELSE_NAVN));
};

export const lyttPåManglerTilgang = (callback: () => void) => {
  window.addEventListener(HENDELSE_NAVN, callback);
  return () => window.removeEventListener(HENDELSE_NAVN, callback);
};
