FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends libimage-exiftool-perl clamav ca-certificates python3 python3-pip \
    && python3 -m pip install --no-cache-dir maigret \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --system metascan && useradd --system --gid metascan --create-home metascan

COPY --from=build /app/target/metascan-0.0.1-SNAPSHOT.jar /app/metascan.jar

EXPOSE 8080

USER metascan

ENTRYPOINT ["java", "-jar", "/app/metascan.jar"]
