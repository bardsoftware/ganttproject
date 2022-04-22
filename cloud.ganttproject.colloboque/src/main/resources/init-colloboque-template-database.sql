CREATE DATABASE project_database_template OWNER postgres IS_TEMPLATE=true;
\connect project_database_template;

-- Function: clone_schema(text, text)

-- DROP FUNCTION clone_schema(text, text);

CREATE OR REPLACE FUNCTION clone_schema(
    source_schema text,
    dest_schema text,
    include_recs boolean)
  RETURNS void AS
$BODY$

--  This function will clone all sequences, tables, triggers, data, views & functions from any existing schema to a new one
-- SAMPLE CALL:
-- SELECT clone_schema('public', 'new_schema', TRUE);

DECLARE
src_oid          oid;
  tbl_oid          oid;
  func_oid         oid;
  object           text;
  buffer           text;
  srctbl           text;
  default_         text;
  column_          text;
  qry              text;
  dest_qry         text;
  v_def            text;
  seqval           bigint;
  sq_last_value    bigint;
  sq_max_value     bigint;
  sq_start_value   bigint;
  sq_increment_by  bigint;
  sq_min_value     bigint;
  sq_cache_value   bigint;
  sq_log_cnt       bigint;
  sq_is_called     boolean;
  sq_is_cycled     boolean;
  sq_cycled        char(10);
  trigger_name_ text;
  trigger_timing_ text;
  trigger_events_ text;
  trigger_orientation_ text;
  trigger_action_ text;
BEGIN
    -- Check that dest_schema does not yet exist
    PERFORM nspname
    FROM pg_namespace
    WHERE nspname = quote_ident(dest_schema);
    IF FOUND
    THEN
        RAISE NOTICE 'dest schema % already exists!', dest_schema;
        RETURN ;
    END IF;

    -- Check that source_schema exists
    SELECT oid INTO src_oid
    FROM pg_namespace
    WHERE nspname = quote_ident(source_schema);
    IF NOT FOUND
        THEN
        RAISE EXCEPTION 'source schema % does not exist!', source_schema;
    END IF;

    EXECUTE 'CREATE SCHEMA ' || quote_ident(dest_schema) ;

    RAISE NOTICE 'Creating sequences...';
    -- Create sequences
    -- TODO: Find a way to make this sequence's owner is the correct table.
    FOR object IN
    SELECT sequence_name::text
    FROM information_schema.sequences
    WHERE sequence_schema = quote_ident(source_schema)
    LOOP
        EXECUTE 'CREATE SEQUENCE ' || quote_ident(dest_schema) || '.' || quote_ident(object);
        srctbl := quote_ident(source_schema) || '.' || quote_ident(object);

        EXECUTE 'SELECT last_value, max_value, start_value, increment_by, min_value, cache_value, log_cnt, is_cycled, is_called
                  FROM ' || quote_ident(source_schema) || '.' || quote_ident(object) || ';'
        INTO sq_last_value, sq_max_value, sq_start_value, sq_increment_by, sq_min_value, sq_cache_value, sq_log_cnt, sq_is_cycled, sq_is_called ;

        IF sq_is_cycled THEN
            sq_cycled := 'CYCLE';
        ELSE
            sq_cycled := 'NO CYCLE';
        END IF;

        EXECUTE 'ALTER SEQUENCE '   || quote_ident(dest_schema) || '.' || quote_ident(object)
            || ' INCREMENT BY ' || sq_increment_by
            || ' MINVALUE '     || sq_min_value
            || ' MAXVALUE '     || sq_max_value
            || ' START WITH '   || sq_start_value
            || ' RESTART '      || sq_min_value
            || ' CACHE '        || sq_cache_value
            || sq_cycled || ' ;' ;

        buffer := quote_ident(dest_schema) || '.' || quote_ident(object);
        IF include_recs THEN
            EXECUTE 'SELECT setval( ''' || buffer || ''', ' || sq_last_value || ', ' || sq_is_called || ');' ;
        ELSE
            EXECUTE 'SELECT setval( ''' || buffer || ''', ' || sq_start_value || ', ' || sq_is_called || ');' ;
        END IF;
    END LOOP;
    RAISE NOTICE '... done';

    -- Create enums
--     RAISE NOTICE 'Creating enums...';
--     FOR object IN
--     SELECT 'CREATE TYPE ' || quote_ident(dest_schema) || '.' || typname || ' AS ENUM (''' ||  string_agg(enumlabel, ''',''' ORDER BY enumsortorder) || ''')'
--     FROM pg_type JOIN pg_enum ON pg_type.oid=enumtypid
--     GROUP BY typname
--     LOOP
--         execute object;
--     END LOOP;
--     RAISE NOTICE '... done';

    -- Create tables
    RAISE NOTICE 'Creating tables...';
    FOR object IN
    SELECT TABLE_NAME::text
    FROM information_schema.tables
    WHERE table_schema = quote_ident(source_schema)
      AND table_type = 'BASE TABLE'

    LOOP
        buffer := dest_schema || '.' || quote_ident(object);
        RAISE NOTICE '... %', buffer;
        EXECUTE 'CREATE TABLE ' || buffer || ' (LIKE ' || quote_ident(source_schema) || '.' || quote_ident(object) || ' INCLUDING ALL)';

        IF include_recs THEN
            -- Insert records from source table
            EXECUTE 'INSERT INTO ' || buffer || ' SELECT * FROM ' || quote_ident(source_schema) || '.' || quote_ident(object) || ';';
        END IF;

        FOR column_, default_ IN
        SELECT column_name::text,
            REPLACE(column_default::text, source_schema, dest_schema)
        FROM information_schema.COLUMNS
        WHERE table_schema = dest_schema
          AND TABLE_NAME = object
          AND column_default LIKE 'nextval(%' || quote_ident(source_schema) || '%::regclass)'
        LOOP
            EXECUTE 'ALTER TABLE ' || buffer || ' ALTER COLUMN ' || column_ || ' SET DEFAULT ' || default_;
        END LOOP;
    END LOOP;
    RAISE NOTICE '... done';

    --  add FK constraint
    FOR qry IN
    SELECT 'ALTER TABLE ' || quote_ident(dest_schema) || '.' || quote_ident(rn.relname) || ' ADD CONSTRAINT ' || quote_ident(ct.conname) || ' ' || replace(pg_get_constraintdef(ct.oid), source_schema, dest_schema) || ';'
    FROM pg_constraint ct
             JOIN pg_class rn ON rn.oid = ct.conrelid
    WHERE connamespace = src_oid
      AND rn.relkind = 'r'
      AND ct.contype = 'f'

    LOOP
        EXECUTE qry;
    END LOOP;


    -- Create views
    FOR object IN
    SELECT table_name::text,
        view_definition
    FROM information_schema.views
    WHERE table_schema = quote_ident(source_schema)

    LOOP
        buffer := dest_schema || '.' || quote_ident(object);
        SELECT view_definition INTO v_def
        FROM information_schema.views
        WHERE table_schema = quote_ident(source_schema)
          AND table_name = quote_ident(object);

        EXECUTE 'CREATE OR REPLACE VIEW ' || buffer || ' AS ' || replace(v_def, source_schema, dest_schema) || ';' ;

    END LOOP;

    -- Create functions
    FOR func_oid IN
    SELECT oid
    FROM pg_proc
    WHERE pronamespace = src_oid

    LOOP
        SELECT pg_get_functiondef(func_oid) INTO qry;
        SELECT replace(qry, source_schema, dest_schema) INTO dest_qry;
        EXECUTE dest_qry;
    END LOOP;

    -- Create triggers
    -- Source: https://dev.to/renatosuero/clone-schema-in-postgresql-4fpc
    FOR trigger_name_, trigger_timing_, trigger_events_, trigger_orientation_, trigger_action_ IN
    SELECT trigger_name::text, action_timing::text, string_agg(event_manipulation::text, ' OR '), action_orientation::text, action_statement::text FROM information_schema.TRIGGERS
    WHERE event_object_schema=source_schema and event_object_table=object
    GROUP BY trigger_name, action_timing, action_orientation, action_statement
    LOOP
       EXECUTE 'CREATE TRIGGER ' || trigger_name_ || ' ' || trigger_timing_ || ' ' || trigger_events_ || ' ON ' || buffer || ' FOR EACH ' || trigger_orientation_ || ' ' || replace(trigger_action_, source_schema, dest_schema);
    END LOOP;

  RETURN;
END;

$BODY$
LANGUAGE plpgsql VOLATILE COST 100;

ALTER FUNCTION clone_schema(text, text, boolean) OWNER TO postgres;

CREATE SCHEMA project_model_metadata;
SET search_path TO project_model_metadata;
CREATE TYPE TaskIntPropertyName AS ENUM ('completion', 'priority');
CREATE TYPE TaskTextPropertyName AS ENUM ('priority', 'color', 'shape', 'web_link', 'notes');


----------------------------------------------------------------------------------------------------------------
-- This template schema is cloned for every "branch" of the project.

CREATE SCHEMA project_template;
SET search_path TO project_template;

-- Basic task data
CREATE TABLE TaskName(
    uid TEXT PRIMARY KEY,
    num INT NOT NULL,
    name TEXT NOT NULL DEFAULT ''
);
-- Task start date and duration shall be changed as a whole
CREATE TABLE TaskDates(
    uid           TEXT PRIMARY KEY REFERENCES TaskName,
    start_date    DATE NOT NULL,
    duration_days INT NOT NULL DEFAULT 1,
    earliest_start_date DATE
);

-- Other task properties can be changed independently, so they are stored in rows, one row corresponds to one
-- instance of the task property value

-- Integer valued properties
CREATE TABLE TaskIntProperties(
    uid        TEXT REFERENCES TaskName,
    prop_name  project_model_metadata.TaskIntPropertyName,
    prop_value INT,
    PRIMARY KEY(uid, prop_name)
);

-- Text valued properties
CREATE TABLE TaskTextProperties(
    uid TEXT REFERENCES TaskName,
    prop_name project_model_metadata.TaskTextPropertyName,
    prop_value TEXT,
    PRIMARY KEY(uid, prop_name)
);

CREATE TABLE TaskCostProperties(
    uid TEXT REFERENCES TaskName PRIMARY KEY,
    is_cost_calculated BOOLEAN,
    cost_manual_value NUMERIC
);

CREATE TABLE TaskClassProperties(
    uid TEXT REFERENCES TaskName PRIMARY KEY,
    is_milestone BOOLEAN NOT NULL DEFAULT false,
    is_project_task BOOLEAN NOT NULL DEFAULT false
);

-- Updatable view which collects all task properties in a single row. Inserts and updates are processed
-- with INSTEAD OF triggers.
CREATE VIEW Task AS
SELECT uid,
       num,
       name,
       start_date,
       duration_days AS duration,
       earliest_start_date,
       is_cost_calculated,
       cost_manual_value,
       is_milestone,
       is_project_task,
       MAX(TIP.prop_value) FILTER (WHERE TIP.prop_name = 'completion') AS completion,
       MAX(TTP.prop_value) FILTER (WHERE TTP.prop_name = 'priority') AS priority,
       MAX(TTP.prop_value) FILTER (WHERE TTP.prop_name = 'color') AS color,
       MAX(TTP.prop_value) FILTER (WHERE TTP.prop_name = 'shape') AS shape,
       MAX(TTP.prop_value) FILTER (WHERE TTP.prop_name = 'web_link') AS web_link,
       MAX(TTP.prop_value) FILTER (WHERE TTP.prop_name = 'notes') AS notes
from      TaskName
JOIN      TaskDates USING(uid)
LEFT JOIN TaskIntProperties TIP USING(uid)
LEFT JOIN TaskTextProperties TTP USING(uid)
LEFT JOIN TaskCostProperties TCP USING(uid)
LEFT JOIN TaskClassProperties TCLP USING(uid)
GROUP BY uid, TaskDates.uid, TCP.uid, TCLP.uid;

CREATE OR REPLACE PROCEDURE update_int_task_property(task_uid TEXT, prop_name_ project_model_metadata.TaskIntPropertyName, prop_value_ INT)
LANGUAGE SQL AS $$
    INSERT INTO project_template.TaskIntProperties(uid, prop_name, prop_value) VALUES(task_uid, prop_name_, prop_value_)
    ON CONFLICT(uid, prop_name) DO UPDATE SET prop_value=prop_value_;
$$;

CREATE OR REPLACE PROCEDURE update_text_task_property(task_uid TEXT, prop_name_ project_model_metadata.TaskTextPropertyName, prop_value_ TEXT)
LANGUAGE SQL AS $$
    INSERT INTO project_template.TaskTextProperties(uid, prop_name, prop_value) VALUES(task_uid, prop_name_, prop_value_)
    ON CONFLICT(uid, prop_name) DO UPDATE SET prop_value=prop_value_;
$$;

CREATE OR REPLACE FUNCTION update_task_row() RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO TaskName(uid, num, name) VALUES (NEW.uid, NEW.num, NEW.name)
    ON CONFLICT(uid) DO UPDATE
    SET num=NEW.num,
        name=NEW.name;

    INSERT INTO TaskDates(uid, start_date, duration_days, earliest_start_date)
    VALUES(NEW.uid, NEW.start_date, NEW.duration, NEW.earliest_start_date)
    ON CONFLICT(uid) DO UPDATE
    SET start_date = NEW.start_date,
        duration_days = NEW.duration,
        earliest_start_date=NEW.earliest_start_date;

    INSERT INTO TaskCostProperties(uid, is_cost_calculated, cost_manual_value)
    VALUES(NEW.uid, NEW.is_cost_calculated, NEW.cost_manual_value)
    ON CONFLICT(uid) DO UPDATE
    SET is_cost_calculated = NEW.is_cost_calculated,
        cost_manual_value = NEW.cost_manual_value;

    INSERT INTO TaskClassProperties(uid, is_milestone, is_project_task)
    VALUES(NEW.uid, COALESCE(NEW.is_milestone, false), COALESCE(NEW.is_project_task, false))
    ON CONFLICT(uid) DO UPDATE
        SET is_milestone = COALESCE(NEW.is_milestone, false),
            is_project_task = COALESCE(NEW.is_project_task, false);

    IF NEW.completion IS NOT NULL THEN
      CALL update_int_task_property(NEW.uid, 'completion', NEW.completion);
    END IF;

    IF NEW.priority IS NOT NULL THEN
        CALL update_text_task_property(NEW.uid, 'priority', NEW.priority);
    END IF;
    IF NEW.color IS NOT NULL THEN
        CALL update_text_task_property(NEW.uid, 'color', NEW.color);
    END IF;
    IF NEW.shape IS NOT NULL THEN
        CALL update_text_task_property(NEW.uid, 'shape', NEW.shape);
    END IF;
    IF NEW.web_link IS NOT NULL THEN
        CALL update_text_task_property(NEW.uid, 'web_link', NEW.web_link);
    END IF;
    IF NEW.notes IS NOT NULL THEN
        CALL update_text_task_property(NEW.uid, 'notes', NEW.notes);
    END IF;

    RETURN NEW;
END;

$$ LANGUAGE plpgsql;

CREATE TRIGGER update_task INSTEAD OF UPDATE ON Task FOR EACH ROW EXECUTE FUNCTION update_task_row();
CREATE TRIGGER insert_task INSTEAD OF INSERT ON Task FOR EACH ROW EXECUTE FUNCTION update_task_row();