create table if not exists Task (
    id                      integer     not null    primary key,
    name                    varchar     not null,
    color                   varchar     null,
    shape                   varchar     null,
    is_milestone            boolean     not null,
    is_project_task         boolean     not null,
    start_date              date        not null,
    duration                integer     not null,
    completion              integer     null,
    earliest_start_date     date        null,
    third_date_constraint   integer     null,
    priority                varchar     null,
    web_link                varchar     null,
    cost_manual_value       varchar     null,
    is_cost_calculated      boolean     null,
    notes                   varchar     null
);

create table if not exists TaskDependency (
    dependee_id     integer     not null,
    dependant_id    integer     not null,
    type            varchar     not null,
    lag             integer     not null,
    hardness        varchar     not null,

    primary key (dependee_id, dependant_id),
    foreign key (dependee_id)  references Task(id),
    foreign key (dependant_id) references Task(id),
    check (dependee_id <> dependant_id)
);

create table if not exists CustomProperty (
    property_id     varchar     not null,
    task_id         integer     not null,
    property_value  varchar     null,

    primary key (task_id, property_id),
    foreign key (task_id) references Task(id)
);
