#!/bin/bash
# Runs once when the postgres container's data volume is first initialized.
# POSTGRES_USER is created as a superuser by the base image and is used only
# for Flyway DDL migrations. This creates a separate, non-superuser role with
# DML-only rights (via default privileges, so it also covers tables created
# later by Flyway) for the application's own runtime connection pool.
set -euo pipefail

: "${APP_DB_PASSWORD:?APP_DB_PASSWORD must be set for the app_runtime role}"

psql -v ON_ERROR_STOP=1 -v app_password="$APP_DB_PASSWORD" \
     --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-'EOSQL'
    -- :DBNAME and :USER are set automatically by psql from the active connection
    -- (i.e. $POSTGRES_DB / $POSTGRES_USER), not passed in via -v.
    CREATE ROLE app_runtime WITH LOGIN PASSWORD :'app_password';
    GRANT CONNECT ON DATABASE :"DBNAME" TO app_runtime;
    GRANT USAGE ON SCHEMA public TO app_runtime;
    ALTER DEFAULT PRIVILEGES FOR ROLE :"USER" IN SCHEMA public
        GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO app_runtime;
    ALTER DEFAULT PRIVILEGES FOR ROLE :"USER" IN SCHEMA public
        GRANT USAGE, SELECT ON SEQUENCES TO app_runtime;
EOSQL
