CREATE ALIAS TASK_COST FOR "net.sourceforge.ganttproject.storage.H2FunctionsKt.taskCost";
CREATE ALIAS TASK_END_DATE FOR "net.sourceforge.ganttproject.storage.H2FunctionsKt.taskEndDate";
DROP VIEW TaskViewForComputedColumns;
CREATE VIEW TaskViewForComputedColumns AS
SELECT
    uid,
    num as id,
    name,
    color,
    is_milestone,
    is_project_task,
    start_date,
    TASK_END_DATE(num) AS end_date,
    duration,
    completion,
    earliest_start_date,
    priority,
    web_link,
    cost_manual_value,
    is_cost_calculated,
    notes,
    TASK_COST(num) AS cost
FROM Task;
