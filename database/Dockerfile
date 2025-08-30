FROM postgres:16

COPY database/db-init/schema.sql /docker-entrypoint-initdb.d/01_schema.sql
COPY database/db-init/data.sql /docker-entrypoint-initdb.d/02_data.sql