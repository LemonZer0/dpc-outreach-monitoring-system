ALTER TABLE call_records
    ADD COLUMN call_outcome VARCHAR(32);

ALTER TABLE call_records
    ADD CONSTRAINT call_records_outcome_check
        CHECK (call_outcome IS NULL OR call_outcome IN (
            'SPOKE_WITH_PERSON',
            'VOICEMAIL',
            'FORWARDED_OR_AUTOMATED',
            'NO_ANSWER',
            'UNKNOWN'
        ));
