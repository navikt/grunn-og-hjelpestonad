FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-25
ENV TZ="Europe/Oslo"
COPY target/grunn-og-hjelp-infotrygd.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djdk.tls.client.protocols=TLSv1.2"
CMD ["-jar", "/app/app.jar"]
