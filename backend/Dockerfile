FROM maven:3.9-eclipse-temurin-17

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# Debug: List what's in target directory
RUN ls -la target/

EXPOSE 8080

# Use a more robust approach to find and run the jar
CMD ["sh", "-c", "java -jar target/*.jar"]
