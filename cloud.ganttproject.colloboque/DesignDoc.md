## Goal and Restrictions

The goal of Colloboque project is to create a protocol and software for real-time collaboration on 
relational structured data with the option for creating offline data branches with subsequent automated or 
assisted merging.

Suppose that our project data is stored on GanttProject Cloud in a task table:

| id | start      | duration | color |
|----|------------|----------|-------|
| 1  | 2023-09-01 | 1d       | red   |

and there are two GanttProject client apps working with that project. One of the client apps is online and 
the user changes the task color to yellow, while another is offline, and the user changes the task start date to "2023-10-01", adds a new task with id=2, and goes online in a few hours after that.

We want our table to look like this when both changes are merged:

| id | start      | duration | color  |
|----|------------|----------|--------|
| 1  | 2023-10-01 | 1d       | yellow |
| 2  | 2023-09-01 | 1d       | red    |

It doesn't seem to be a trivial problem, because concurrent update of the same row would've been an issue even if 
we had a centralized database with both users being online at the moment of change. In our case any user may go offline
for quite long time, and we want a reliable protocol that lets them receive the updates or merge the changes that they created when they were offline.

## Alternatives and Related Technologies

There is a number of related technologies and possible alternatives.

### Logical Replication
### RAFT 
### PostgreSQL Multimaster
https://pgconf.ru/en/2021/290216

### Database branching and version control
https://neon.tech/docs/introduction/branching
https://github.com/dolthub/dolt

### CRDT

### Text data merging
