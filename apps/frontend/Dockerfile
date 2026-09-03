FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:24-slim

WORKDIR /app

COPY package.json package-lock.json ./

COPY dist/ ./dist/

COPY build/ ./build/
COPY node_modules/ ./node_modules/

ENV ENV=production

EXPOSE 8080

CMD ["dist/server.js"]