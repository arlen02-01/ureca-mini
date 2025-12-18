FROM eclipse-temurin:21-jdk-alpine

# 타임존 설정
ENV TZ=Asia/Seoul

# 프로파일 설정 (기본값 prod)
ENV SPRING_PROFILES_ACTIVE=prod

ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]