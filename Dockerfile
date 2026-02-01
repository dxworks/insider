FROM eclipse-temurin:11-jre

WORKDIR /app

# Non-root user for security
RUN useradd -r -u 10001 -g root appuser

COPY ./build/libs/insider-*.jar /app/insider.jar
COPY config/ /app/config/
COPY bin/insider.sh /app/insider.sh

RUN chmod +x /app/insider.sh \
 && chown -R 10001:0 /app

USER 10001

ENTRYPOINT ["/app/insider.sh"]

