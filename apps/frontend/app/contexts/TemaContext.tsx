import React, { createContext, useContext, useState, useEffect, type ReactNode } from "react";

interface TemaContextType {
  mørktTema: boolean;
  byttTema: () => void;
}

const TemaContext = createContext<TemaContextType | null>(null);

export function useTemaContext() {
  const context = useContext(TemaContext);

  if (!context) {
    throw new Error("useTemaContext må brukes innenfor TemaProvider");
  }

  return context;
}

export function TemaProvider({ children }: { children: ReactNode }) {
  const [mørktTema, settMørktTema] = useState(() => {
    if (typeof window === "undefined") return false;
    return localStorage.getItem("tema") === "mørkt";
  });

  useEffect(() => {
    document.documentElement.classList.toggle("dark", mørktTema);
    localStorage.setItem("tema", mørktTema ? "mørkt" : "lyst");
  }, [mørktTema]);

  const byttTema = () => settMørktTema((forrige) => !forrige);

  return (
    <TemaContext.Provider value={{ mørktTema, byttTema }}>
      {children}
    </TemaContext.Provider>
  );
}
