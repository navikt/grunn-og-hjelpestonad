import { useLesevisningsContext } from "~/contexts/LesevisningsContext";

export function useErLesevisning() {
  const { erLesevisning } = useLesevisningsContext();

  return erLesevisning;
}
