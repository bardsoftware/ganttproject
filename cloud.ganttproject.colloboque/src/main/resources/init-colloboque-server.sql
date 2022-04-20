CREATE SCHEMA project_template;

CREATE TABLE project_template.TaskName(uid TEXT PRIMARY KEY, num INT NOT NULL, name TEXT NOT NULL DEFAULT '');
CREATE TABLE project_template.TaskDates(
    uid TEXT PRIMARY KEY REFERENCES project_template.TaskName,
    start_date DATE NOT NULL,
    duration_days INT NOT NULL
);
CREATE TABLE project_template.TaskIntProperties(
    uid TEXT REFERENCES project_template.TaskName,
    prop_name TEXT,
    prop_value INT
);
CREATE VIEW project_template.Task AS
    SELECT uid, num, name, start_date, duration_days AS duration,
           MAX(prop_value) FILTER(WHERE prop_name = 'completion') AS completion
    from project_template.TaskName JOIN project_template.TaskDates USING(uid)
    LEFT JOIN project_template.TaskIntProperties USING(uid)
    GROUP BY uid, TaskDates.uid;

CREATE OR REPLACE FUNCTION project_template.update_task_row() RETURNS TRIGGER AS $$
    BEGIN
        INSERT INTO project_template.TaskName(uid, num, name) SELECT NEW.uid, NEW.num, NEW.name
        ON CONFLICT(uid) DO UPDATE SET num=NEW.num, name=NEW.name;

        INSERT INTO project_template.TaskDates(uid, start_date, duration_days)
        SELECT NEW.uid, NEW.start_date, NEW.duration
        ON CONFLICT(uid) DO UPDATE SET start_date=NEW.start_date, duration_days=NEW.duration;
        RETURN NEW;
    end;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_task INSTEAD OF UPDATE ON project_template.Task FOR EACH ROW EXECUTE FUNCTION project_template.update_task_row();
CREATE TRIGGER insert_task INSTEAD OF INSERT ON project_template.Task FOR EACH ROW EXECUTE FUNCTION project_template.update_task_row();