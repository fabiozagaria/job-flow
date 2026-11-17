#!/bin/sh
set -e

# Render fornisce DATABASE_URL come postgresql://user:password@host[:port]/database.
# Il driver JDBC vuole jdbc:postgresql://host[:port]/database, con credenziali separate
# (DB_USER e DB_PASSWORD, lette da application.properties).
if [ -n "$DATABASE_URL" ]; then
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DATABASE_URL##*@}"
fi

# Render imposta PORT (default 10000); 8080 per l'uso locale.
exec java -Dserver.port="${PORT:-8080}" -jar /app/app.jar
