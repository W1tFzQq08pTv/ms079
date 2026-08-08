FROM eclipse-temurin:8-jdk@sha256:c8b86f2c0a9bd6cc1b7fabc5d1a097501a166eea7cad3b2764e90e96178bea9b AS build

WORKDIR /build
RUN apt-get update \
    && apt-get install --yes --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

COPY .mvn ./.mvn
COPY mvnw pom.xml ./
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests dependency:go-offline

COPY src ./src
RUN ./mvnw --batch-mode --no-transfer-progress -DskipTests process-classes dependency:copy-dependencies \
        -DincludeScope=runtime \
        -DoutputDirectory=/build/target/dependency

FROM eclipse-temurin:8-jre@sha256:058a4f63a2338710054e905f88cd50e843173124b50a921df83e77506af6fc82 AS runtime

WORKDIR /app
ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai

RUN mkdir -p /app/config

COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/dependency ./lib

VOLUME ["/app/wz", "/app/scripts", "/app/logs"]

ENTRYPOINT ["java", "-server", "-Dfile.encoding=UTF-8", "-Dwz.path=wz", "-cp", "classes:lib/*", "com.github.mrzhqiang.maplestory.MapleStoryApplication"]
