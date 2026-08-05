FROM maven:3.8.8-eclipse-temurin-8 AS build

WORKDIR /build
COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests process-classes dependency:copy-dependencies \
        -DincludeScope=runtime \
        -DoutputDirectory=/build/target/dependency

FROM eclipse-temurin:8-jre AS runtime

WORKDIR /app
ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai

COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/dependency ./lib

VOLUME ["/app/wz", "/app/脚本", "/app/logs"]

ENTRYPOINT ["java", "-server", "-Dfile.encoding=UTF-8", "-Dwzpath=wz", "-cp", "classes:lib/*", "com.github.mrzhqiang.maplestory.MapleStoryApplication"]
