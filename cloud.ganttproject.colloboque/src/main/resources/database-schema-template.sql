----------------------------------------------------------------------------------------------------------------
-- This template schema is cloned for every "branch" of the project.

CREATE SCHEMA project_template;
SET search_path TO project_template;
CREATE TYPE "taskintpropertyname" AS ENUM ('completion', 'priority');
CREATE TYPE "tasktextpropertyname" AS ENUM ('priority', 'color', 'shape', 'web_link', 'notes');

-- Basic task data
CREATE TABLE TaskName(
                         uid VARCHAR(128) PRIMARY KEY,
                         num INT NOT NULL,
                         name VARCHAR(128) NOT NULL DEFAULT ''
);
-- Task start date and duration shall be changed as a whole
CREATE TABLE TaskDates(
                          uid           VARCHAR(128) PRIMARY KEY REFERENCES TaskName,
                          start_date    DATE NOT NULL,
                          duration_days INT NOT NULL DEFAULT 1,
                          earliest_start_date DATE
);

-- Other task properties can be changed independently, so they are stored in rows, one row corresponds to one
-- instance of the task property value

-- Integer valued properties
CREATE TABLE TaskIntProperties(
                                  uid        VARCHAR(128) REFERENCES TaskName,
                                  prop_name  "taskintpropertyname",
                                  prop_value INT,
                                  PRIMARY KEY(uid, prop_name)
);

-- Text valued properties
CREATE TABLE TaskTextProperties(
                                   uid        VARCHAR(128) REFERENCES TaskName,
                                   prop_name  "tasktextpropertyname",
                                   prop_value VARCHAR(128),
                                   PRIMARY KEY(uid, prop_name)
);

CREATE TABLE TaskCostProperties(
                                   uid VARCHAR(128) REFERENCES TaskName PRIMARY KEY,
                                   is_cost_calculated BOOLEAN,
                                   cost_manual_value NUMERIC
);

CREATE TABLE TaskClassProperties(
                                    uid VARCHAR(128) REFERENCES TaskName PRIMARY KEY,
                                    is_milestone BOOLEAN NOT NULL DEFAULT false,
                                    is_project_task BOOLEAN NOT NULL DEFAULT false
);

-- Updatable view which collects all task properties in a single row. Inserts and updates are processed
-- with INSTEAD OF triggers.
CREATE VIEW Task AS
SELECT TaskName.uid,
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
GROUP BY TaskName.uid, TaskDates.uid, TCP.uid, TCLP.uid;

CREATE TABLE ProjectFileSnapshot(
    base_txn_id BIGINT PRIMARY KEY,
    project_xml TEXT
);

-- This table keeps the logs received from the client.
-- The logs are built on top of a database state that is identified with the base transaction identifier.
-- We expect that normally there is just 1 log record that applies to each base state, however,
-- if a client goes offline, the log records may stack on top of each other and, until they are received and
-- applied on the server, they refer to the same base txn ID.
CREATE TABLE TransactionLog(
    base_txn_id BIGINT REFERENCES ProjectFileSnapshot,
    log_record_num INT CHECK(log_record_num >= 0),
    log_record_json VARCHAR(65535),
    PRIMARY KEY (base_txn_id, log_record_num)
);

