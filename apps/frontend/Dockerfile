FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/node:26-slim

WORKDIR /app

COPY package.json ./

COPY dist/ ./dist/

COPY build/client/ ./build/client/

ENV ENV=production

EXPOSE 8080

CMD ["dist/server.js"]