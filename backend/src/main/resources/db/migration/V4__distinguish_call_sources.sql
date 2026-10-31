ALTER TABLE call_records
    ADD COLUMN call_source VARCHAR(16) NOT NULL DEFAULT 'APP',
    ADD COLUMN call_type VARCHAR(24);

ALTER TABLE call_records
    ADD CONSTRAINT call_records_source_check
        CHECK (call_source IN ('APP', 'SIMULATION')),
    ADD CONSTRAINT call_records_type_check
        CHECK (call_type IS NULL OR call_type IN ('DIALER', 'MANUAL_METADATA')),
    ADD CONSTRAINT call_records_source_type_check
        CHECK (call_source = 'APP' OR call_type IS NULL);
