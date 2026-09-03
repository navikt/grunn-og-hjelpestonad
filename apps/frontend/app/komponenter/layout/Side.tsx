import React from "react";
import styles from "./Side.module.css";

export const Side: React.FC<{
  children?: React.ReactNode;
}> = ({ children }) => {
  return <main className={styles.layout}>{children}</main>;
};
