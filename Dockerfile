FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS="-Xms1g -Xmx1g"

COPY build/libs/*.jar app.jar

EXPOSE 80

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
