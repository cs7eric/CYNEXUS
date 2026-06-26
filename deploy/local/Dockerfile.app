FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

ARG MAVEN_PROJECT
ARG JAR_NAME
COPY ${MAVEN_PROJECT}/target/${JAR_NAME} /app/app.jar

ENV JAVA_OPTS=""

EXPOSE 18000 18080 18081 18082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
