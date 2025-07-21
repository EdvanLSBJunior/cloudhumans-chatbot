# Imagem base com JDK 17
FROM eclipse-temurin:17-jdk-alpine

# Diretório de trabalho no container
WORKDIR /app

# Copia o JAR gerado para dentro do container
COPY target/chatbot.jar app.jar

# Expõe a porta usada pela aplicação
EXPOSE 8080

# Comando para iniciar o Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
