FROM mcr.microsoft.com/playwright/java:v1.49.0-jammy

WORKDIR /app

COPY build/libs/clicker-0.1.0.jar app.jar

ENV JAVA_OPTS="-Xmx4g -Xms2g"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
