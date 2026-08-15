-- Flyway SQL callback (naming convention: afterMigrate.sql), runs after every successful
-- migrate under the schema-owner connection. app_runtime picks up DML rights on this table
-- too via ALTER DEFAULT PRIVILEGES (docker/postgres-init/01-create-app-role.sh) since it's
-- created by the same owner role — revoke that here, the app has no business writing to
-- Flyway's own migration history. Guarded: app_runtime doesn't exist in single-role setups
-- (tests, simple local runs without the postgres-init script), where this is a no-op.
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'app_runtime') THEN
        REVOKE INSERT, UPDATE, DELETE ON flyway_schema_history FROM app_runtime;
    END IF;
END
$$;
