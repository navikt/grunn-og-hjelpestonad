const HENDELSE_NAVN = "endringshistorikk:oppdater";

// TODO: Ved ferdigstilling, så skal vi gå gjennom dette. Usikker på om dette er beste vei til mål.
export const oppdaterEndringshistorikk = () => {
  window.dispatchEvent(new Event(HENDELSE_NAVN));
};

export const lyttPåEndringshistorikk = (callback: () => void) => {
  window.addEventListener(HENDELSE_NAVN, callback);
  return () => window.removeEventListener(HENDELSE_NAVN, callback);
};
