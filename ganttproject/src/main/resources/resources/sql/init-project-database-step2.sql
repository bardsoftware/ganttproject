CREATE ALIAS IF NOT EXISTS TASK_COST FOR "net.sourceforge.ganttproject.storage.H2FunctionsKt.taskCost";
CREATE ALIAS IF NOT EXISTS TASK_END_DATE FOR "net.sourceforge.ganttproject.storage.H2FunctionsKt.taskEndDate";

DROP VIEW TaskViewForComputedColumns;
DROP TABLE Task CASCADE ;
create table if not exists Task (
    uid                     varchar                 not null,
    num                     integer                 not null,
    name                    varchar                 not null,
    color                   varchar                     null,
    shape                   varchar                     null,
    is_milestone            boolean                 not null DEFAULT false,
    is_project_task         boolean                 not null DEFAULT false,
    start_date              date                    not null,
    end_date                date GENERATED ALWAYS AS TASK_END_DATE(num),
    duration                integer                 not null,
    completion              integer                     null,
    earliest_start_date     date                        null,
    priority                varchar                 not null DEFAULT '1',
    web_link                varchar                     null,
    cost_manual_value       numeric(1000, 2)          null,
    is_cost_calculated      boolean                     null,
    notes                   varchar                     null,
    cost                    numeric(1000, 2) GENERATED ALWAYS AS TASK_COST(num),

    primary key (uid)
);

ALTER TABLE TaskDependency ADD CONSTRAINT dependee_fk FOREIGN KEY (dependee_uid) REFERENCES Task ON DELETE CASCADE ;
ALTER TABLE TaskDependency ADD CONSTRAINT dependant_fk FOREIGN KEY (dependant_uid) REFERENCES Task ON DELETE CASCADE ;