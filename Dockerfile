# ================================
# 1) Build Stage (Gradle로 JAR 빌드)
# ================================
FROM gradle:8.5-jdk17 AS builder
WORKDIR /app

# 프로젝트 전체 복사
COPY . .

# dependency 캐싱 + build
RUN gradle clean build -x test


# ================================
# 2) Run Stage (최종 JAR 실행)
# ================================
FROM eclipse-temurin:17-jdk
WORKDIR /app

# builder에서 생성된 jar 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 8080 포트 오픈
EXPOSE 8080

# Spring Boot 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
