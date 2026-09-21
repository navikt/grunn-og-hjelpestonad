import { useLesevisningsContext } from "~/fellesContext/LesevisningsContext";

export function useErLesevisning() {
  const { erLesevisning } = useLesevisningsContext();

  return erLesevisning;
}
