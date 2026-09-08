import { reactRouter } from "@react-router/dev/vite";
import { defineConfig } from "vite";
import babel from "vite-plugin-babel";

export default defineConfig({
  resolve: {
    tsconfigPaths: true,
  },
  ssr: {
    noExternal: ["@react-router/express", "@react-router/node", "react-router"],
  },
  environments: {
    ssr: {
      build: {
        rollupOptions: {
          input: "./server/app.ts",
        },
      },
    },
  },
  plugins: [
    reactRouter(),
    babel({
      include: /\.[jt]sx?$/,
      exclude: /node_modules/,
      babelConfig: {
        presets: ["@babel/preset-typescript"],
        plugins: [["babel-plugin-react-compiler", {}]],
      },
    }),
  ],
  server: {},
});
