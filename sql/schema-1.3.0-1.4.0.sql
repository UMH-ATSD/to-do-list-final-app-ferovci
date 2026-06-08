-- Migration: Add description column to equipos table
-- Version: 1.3.0 -> 1.4.0

ALTER TABLE equipos ADD COLUMN descripcion VARCHAR(500);

-- Populate existing teams with NULL description (optional fields default to NULL)
-- No data migration needed since this is a new optional field
