#!/bin/sh
set -eu

export POSTGRES_DB=dpc_outreach
password_file="${PGDATA:-/var/lib/postgresql/data}/.dpc-db-password"
if [ -s "$password_file" ]; then
  DB_PASSWORD="$(cat "$password_file")"
else
  DB_PASSWORD="$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')"
fi
export DB_PASSWORD
export POSTGRES_PASSWORD="${DB_PASSWORD}"

if [ -z "${ADMIN_PASSWORD:-}" ]; then
  ADMIN_PASSWORD="$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')"
  export ADMIN_PASSWORD
  echo "Generated local demo admin credentials:"
  echo "  username: ${ADMIN_USERNAME}"
  echo "  password: ${ADMIN_PASSWORD}"
fi

docker-entrypoint.sh postgres -c listen_addresses=127.0.0.1 &
postgres_pid=$!

stop_services() {
  if [ -n "${app_pid:-}" ]; then
    kill "$app_pid" 2>/dev/null || true
    wait "$app_pid" 2>/dev/null || true
  fi
  kill "$postgres_pid" 2>/dev/null || true
  wait "$postgres_pid" 2>/dev/null || true
}
trap stop_services INT TERM

until pg_isready -h 127.0.0.1 -U postgres -d dpc_outreach >/dev/null 2>&1; do
  if ! kill -0 "$postgres_pid" 2>/dev/null; then
    echo "PostgreSQL exited before becoming ready." >&2
    wait "$postgres_pid"
  fi
  sleep 1
done
if [ ! -s "$password_file" ]; then
  printf '%s' "$DB_PASSWORD" > "$password_file"
fi

java -XX:MaxRAMPercentage=75 -jar /app/app.jar --server.port="${PORT:-8080}" &
app_pid=$!
set +e
wait "$app_pid"
app_status=$?
set -e
kill "$postgres_pid" 2>/dev/null || true
wait "$postgres_pid" 2>/dev/null || true
exit "$app_status"
