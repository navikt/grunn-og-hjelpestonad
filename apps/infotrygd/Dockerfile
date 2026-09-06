FROM gcr.io/distroless/java21-debian12:nonroot
ENV TZ="Europe/Oslo"
COPY target/grunn-og-hjelpestonad-infotrygd.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75 -Djdk.tls.client.protocols=TLSv1.2"
CMD ["-jar", "/app/app.jar"]
