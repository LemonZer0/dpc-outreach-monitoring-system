-- Hibernate's generated enum check constraint can remain stale after adding
-- an enum value when ddl-auto=update is used. The column is VARCHAR, so the
-- application enum is the source of valid values; remove only the stale
-- check constraint tied to verification_status.
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    IF to_regclass('public.call_records') IS NOT NULL THEN
        FOR constraint_record IN
            SELECT con.conname
            FROM pg_constraint con
            WHERE con.conrelid = 'public.call_records'::regclass
              AND con.contype = 'c'
              AND pg_get_constraintdef(con.oid) ILIKE '%verification_status%'
        LOOP
            EXECUTE format('ALTER TABLE public.call_records DROP CONSTRAINT %I', constraint_record.conname);
        END LOOP;
    END IF;
END $$;
