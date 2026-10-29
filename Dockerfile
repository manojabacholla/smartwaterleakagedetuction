
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY WaterLeakApp.java .
RUN javac --add-modules jdk.httpserver WaterLeakApp.java
CMD ["java", "--add-modules", "jdk.httpserver", "WaterLeakApp"]
