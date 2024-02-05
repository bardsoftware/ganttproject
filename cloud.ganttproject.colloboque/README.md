# Colloboque: GanttProject real-time collaboration server

This document gives an overview of a real-time collaboration server for GanttProject documents.

## Building and running the server

Colloboque server uses a couple of libraries from GanttProject. They are available in GanttProject GitHub packages, and 
it is possible to build them locally.

### Building libraries from sources

GanttProject and Colloboque are tightly coupled and share quite a lot of code. Sometimes you may need to change the library code and immediately 
use it in Colloboque. For these purposes, you may want to build the libs locally and access them from the local Maven
repository. To build the libraries, run `cd .. && gradle publishtomavenlocal`. The libraries have version numbers composed
from the current date, using YY.MM.DD pattern (e.g. 24.01.04 if a library was built on Jan 4, 2024).

### Building Colloboque

Colloboque can be built with the standard `gradle build` command. 
If you want to use libs from GitHub Packages, you need to pass your GitHub username and Personal Access Token to the 
Gradle when building Colloboque using project properties:

```bash
gradle -Pgpr.user=<GH_USERNAME> -Pgpr.key=<GH_PAT> build
```

You can save `gpr.user` and `gpr.key` options into the local `gradle.properties` file (don't commit it into the version control). The remaining gradle command in this doc assume that the access to the required libs/repositories is already configured.

Please refer to [GitHub Docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
for details on Personal Access Token

  
### Environment

Colloboque requires an initialized PostgreSQL database. You can run a PostgreSQL instance using Docker:

```
docker run -d -p 5432:5432 --name dev-postgres -e POSTGRES_HOST_AUTH_METHOD=trust postgres
```

This will start Postgres on port 5432 with `postgres` super user and empty password. Initialize it using 

```
cd src/main/resources/
psql -h localhost -U postgres -f ./init-colloboque-server.sql
```

### Running the server

The following command will show the available command line arguments:

```
gradle run --args='--help'
```

The default values are okay for the local development, so you can run the server with 

```
gradle run
```

It should print to the console something like

```
> Task :run
14:49:38.771 DEBUG [Startup @ main] - Starting dev Colloboque server on port 9000
14:49:38.883 DEBUG [Startup @ main] - Connected to the database. PostgreSQL 15.3 (Debian 15.3-1.pgdg120+1) on x86_64-pc-linux-gnu, compiled by gcc (Debian 12.2.0-14) 12.2.0, 64-bit
<=
```

## Building and running the client

Please refer to [GanttProject Docs](../README.md) for instructions on building and running GanttProject. 
For Colloboque development purposes, running GanttProject with `gradle run` is okay: it will try to connect to 
Colloboque server running on `localhost:9000` and will open a test document with a single task. 

It is expected that two GanttProject instances will send their updates to the server, will receive the updates
from the server and immediately apply them. At the moment, the following update types are supported:

- adding a new task