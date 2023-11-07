# Colloboque: GanttProject real-time collaboration server

This document gives an overview of a real-time collaboration server for GanttProject documents.

## Building and running the server

Colloboque server uses a couple of libraries from GanttProject. They are available in GanttProject GitHub packages, and 
it is possible to build them locally.

### Building

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