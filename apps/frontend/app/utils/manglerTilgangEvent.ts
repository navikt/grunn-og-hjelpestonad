const HENDELSE_NAVN = "tilgang:mangler-tilgang";

export const varsleManglerTilgang = (sti: string) => {
  window.dispatchEvent(new CustomEvent<string>(HENDELSE_NAVN, { detail: sti }));
};

export const lyttPåManglerTilgang = (callback: (sti: string) => void) => {
  const håndterHendelse = (hendelse: Event) => {
    if (hendelse instanceof CustomEvent && typeof hendelse.detail === "string") {
      callback(hendelse.detail);
    }
  };
  window.addEventListener(HENDELSE_NAVN, håndterHendelse);
  return () => window.removeEventListener(HENDELSE_NAVN, håndterHendelse);
};
