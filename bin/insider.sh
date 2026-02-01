#!/bin/sh
set -eu

# Default max heap; override with -e JAVA_XMX=2g if needed
JAVA_XMX="${JAVA_XMX:-4g}"

# exec so Java is PID 1 and receives SIGTERM/SIGINT properly
exec java -Xmx"${JAVA_XMX}" -jar /app/insider.jar "$@"
