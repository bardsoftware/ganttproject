create table if not exists Task (
    id                      integer                 not null,
    name                    varchar                 not null,
    color                   varchar                     null,
    shape                   varchar                     null,
    is_milestone            boolean                 not null,
    is_project_task         boolean                 not null,
    start_date              timestamp(0)            not null,
    duration                integer                 not null,
    completion              integer                     null,
    earliest_start_date     timestamp(0)                null,
    priority                varchar                 not null,
    web_link                varchar                     null,
    cost_manual_value       numeric(100000, 2)          null,
    is_cost_calculated      boolean                     null,
    notes                   varchar                     null,

    primary key (id)
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
