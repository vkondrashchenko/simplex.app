# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml pom.xml
COPY backend/core/pom.xml core/pom.xml
COPY backend/lambda/pom.xml lambda/pom.xml
# Download dependencies separately for layer caching
RUN mvn dependency:go-offline -pl core,lambda -am -q
COPY backend/core/src core/src
COPY backend/lambda/src lambda/src
RUN mvn package -pl lambda -am -DskipTests -q

# ---- Runtime stage ----
FROM public.ecr.aws/lambda/java:21
COPY --from=build /app/lambda/target/quarkus-app/lib/ ${LAMBDA_TASK_ROOT}/lib/
COPY --from=build /app/lambda/target/quarkus-app/*.jar ${LAMBDA_TASK_ROOT}/
COPY --from=build /app/lambda/target/quarkus-app/app/ ${LAMBDA_TASK_ROOT}/app/
COPY --from=build /app/lambda/target/quarkus-app/quarkus/ ${LAMBDA_TASK_ROOT}/quarkus/

CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]
