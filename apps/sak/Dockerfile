FROM gcr.io/distroless/java25-debian13:nonroot
ENV TZ="Europe/Oslo"
COPY target/grunn-og-hjelpestonad.jar /app/app.jar
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"
CMD ["-jar", "/app/app.jar"]
