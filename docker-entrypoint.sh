#!/bin/sh
set -e

# DATABASE_URL arriva come postgresql://user:password@host[:port]/database[?parametri]
# (Neon, oppure un altro PostgreSQL). Il driver JDBC vuole jdbc:postgresql://host[:port]/database,
# con credenziali separate (DB_USER e DB_PASSWORD, lette da application.properties).
# I parametri dell'URL (es. sslmode=require di Neon) vengono tolti: le impostazioni TLS stanno nel
# profilo Spring (application-neon.properties) e non devono essere sovrascritte dall'URL.
if [ -n "$DATABASE_URL" ]; then
  case "$DATABASE_URL" in
    postgres://*|postgresql://*) ;;
    *)
      # Non stampare il valore: può contenere la password (es. ...?user=...&password=... del formato JDBC).
      echo "DATABASE_URL deve iniziare con postgresql:// (formato postgresql://utente:password@host/database)." >&2
      echo "Su Neon: Connect -> \"Connection string\", non la scheda Java/JDBC." >&2
      exit 1
      ;;
  esac
  rest="${DATABASE_URL#*://}"
  host_and_db="${rest#*@}"
  export SPRING_DATASOURCE_URL="jdbc:postgresql://${host_and_db%%\?*}"
  # Utente e password dall'URL solo se non sono già impostati separatamente.
  case "$rest" in
    *@*)
      credentials="${rest%%@*}"
      : "${DB_USER:=${credentials%%:*}}"
      : "${DB_PASSWORD:=${credentials#*:}}"
      export DB_USER DB_PASSWORD
      ;;
  esac
fi

# Render imposta PORT (default 10000); 8080 per l'uso locale.
exec java -Dserver.port="${PORT:-8080}" -jar /app/app.jar
