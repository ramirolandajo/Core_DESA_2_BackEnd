# Etapa de build
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn -q -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package \
    && sh -c 'if [ -f target/app.jar ]; then echo "Found target/app.jar"; \
               elif ls target/*-SNAPSHOT.jar >/dev/null 2>&1; then JAR=$(ls target/*-SNAPSHOT.jar | grep -v original | head -n1) && echo "Using $JAR" && cp "$JAR" target/app.jar; \
               else echo "No JAR built in target/" && ls -l target || true && exit 1; fi' \
    && ls -l target

# Etapa de runtime
FROM eclipse-temurin:17-jre
ENV JAVA_OPTS=""
ENV SERVER_PORT=8082
ENV SPRING_PROFILES_ACTIVE=default
# Kafka y DB se configuran por variables de entorno de Spring
# Ej: -e SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
#     -e DB_DEV=host.docker.internal:3306 -e USR=root -e PASSWORD=secret
WORKDIR /app
COPY --from=build /app/target/app.jar app.jar
EXPOSE 8082
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=$SERVER_PORT -jar app.jar"]
