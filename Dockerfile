FROM openjdk:8-alpine

COPY target/uberjar/lum.jar /lum/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/lum/app.jar"]
