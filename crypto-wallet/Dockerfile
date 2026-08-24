# Slika za crypto-wallet mikroservis.
# JAR se prethodno pravi Maven-om (mvn clean package), pa se ovde samo pakuje
# u laganu runtime sliku - zbog toga je izgradnja slika brza.
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Pokretanje pod korisnikom bez root privilegija.
RUN addgroup -S soas && adduser -S soas -G soas

COPY target/crypto-wallet-1.0.0.jar app.jar
RUN chown soas:soas /app/app.jar

USER soas

EXPOSE 8300

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10   CMD wget --quiet --tries=1 --spider http://localhost:8300/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
