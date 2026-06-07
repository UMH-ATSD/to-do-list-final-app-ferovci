# README_GitFlow_Teamwork - Version 1.3.0

## Project links

- GitHub repository: [to-do-list-final-app-ferovci](https://github.com/UMH-ATSD/to-do-list-final-app-ferovci.git)
- Docker Hub repository: [todolist-ferovci](https://hub.docker.com/r/hanasl1/todolist-ferovci)
- Final Docker image: [hanasl1/todolist-ferovci:1.3.0](https://hub.docker.com/repository/docker/hanasl1/todolist-ferovci/tags/1.3.0)
- Snapshot Docker image: [todolist-ferovci:1.3.0-snapshot](https://hub.docker.com/repository/docker/hanasl1/todolist-ferovci/tags/1.3.0-snapshot)

---

## 1) Scope and objective

This document describes the work done for version `1.3.0` of the ToDoList application. The objective of this exercise was not only to add new application features, but also to adapt the project to a team workflow and prepare it for a production-like deployment with Docker and PostgreSQL.

The development was done using GitFlow. The `main` branch was kept as the stable branch, while `develop` was used as the integration branch for new work. New functionality was developed in feature branches and merged into `develop` through pull requests. The final version was prepared in the `release-1.3.0` branch and then merged into both `main` and `develop`.

The application changes in this version are:

* display the number of members for each team,
* display a generated description preview on the team detail page,
* validate task title length with a limit of 200 characters,
* add a live character counter to task creation and editing forms.

The deployment-related changes are:

* update the Docker image so runtime parameters can be passed to the application,
* run the application and PostgreSQL as two containers connected by a Docker network,
* add a `postgres-prod` profile for production execution,
* export database schemas,
* create a migration script,
* create a database backup,
* publish the final Docker image.

---

## 2) Team workflow and GitFlow

The team repository was organized around two main branches:

```text
main
develop
```

`main` contains stable released versions. `develop` contains completed work that is ready to be part of the next release, but is not yet released.

For each issue, a separate branch was created from `develop`. After the work was finished, the branch was pushed to GitHub and a pull request was created. Pull requests were reviewed before merging. This was important because different team members were working on the same codebase and we needed to avoid direct changes to the shared branches.

For the release, we created:

```text
release-1.3.0
```

This branch was created from `develop`. It was used only for release-specific work: changing the version number, generating schema files, creating the migration script, checking the production deployment and preparing the final Docker image.

After the release branch was tested, it was merged into `main` and tagged as:

```text
v1.3.0
```

The same release branch was also merged back into `develop`, so that future development starts from the same state as the released version.


## 3) Docker image configuration

The application was already containerized, but the original Dockerfile had a limitation: the Java execution command was fixed. Because of that, we could not easily pass Spring Boot parameters when starting the container, for example the active profile or the PostgreSQL host.

The Dockerfile was changed so that arguments passed to `docker run` are forwarded to the Java command:

```dockerfile
ENTRYPOINT ["sh","-c","java -Djava.security.egd=file:/dev/urandom -jar /app.jar ${0} ${@}"]
```

This makes the Docker image reusable. The same image can be used for development, production-like testing and final deployment, because the Spring profile and database parameters are provided at runtime.

The PostgreSQL profile was also changed to use configurable values:

```properties
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
DB_USER=atsd
DB_PASSWD=atsd

spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/atsd
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWD}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

This means that the database location is not hardcoded. Locally, the application can connect to `localhost`, while inside Docker it can connect to the PostgreSQL container using the Docker network alias `postgres`.

The application was packaged and the Docker image was built with:

```bash
mvn clean package
docker build -t hanasl1/todolist-ferovci:1.3.0-snapshot .
```

To check that the runtime parameters were really being passed into the container, we ran the image with a fake database host:

```bash
docker run --rm hanasl1/todolist-ferovci:1.3.0-snapshot --spring.profiles.active=postgres --POSTGRES_HOST=host-prueba
```

The application loaded the `postgres` profile and then failed because `host-prueba` was not a real PostgreSQL host. This was the expected result and confirmed that the Docker entrypoint and profile parameters worked correctly.

The final image for the release is:

```text
hanasl1/todolist-ferovci:1.3.0
```

---

## 4) Production deployment with PostgreSQL

For the production deployment, the application was executed using two Docker containers:

```text
team-db                  PostgreSQL database container
todolist-ferovci          Spring Boot application container
```

Both containers were connected to the same Docker network:

```bash
docker network create team-network
```

The database container was started with the name `team-db` and with the network alias `postgres`:

```bash
docker run -d --network team-network --network-alias postgres -v "${PWD}:/my-host" --name team-db -e POSTGRES_USER=atsd -e POSTGRES_PASSWORD=atsd -e POSTGRES_DB=atsd postgres:13
```

The important part is the network alias:

```text
postgres
```

When the application runs inside another Docker container on the same network, it does not connect to `localhost`. In that case, `localhost` would refer to the application container itself. Instead, the application connects to:

```text
postgres:5432
```

The volume mount:

```text
-v "${PWD}:/my-host"
```

was used to share files between the host machine and the PostgreSQL container. We used this shared directory for schema exports, migration files and backups.

The application container was started in the same network:

```bash
docker run --rm --network team-network -p 8080:8080 hanasl1/todolist-ferovci:1.3.0 --spring.profiles.active=postgres --POSTGRES_HOST=postgres --POSTGRES_PORT=5432
```

The app was then available at:

```text
http://localhost:8080
```

To verify persistence, we created data through the web application, stopped the application container and started it again. The data was still present because it was stored in the PostgreSQL container, not inside the application container.

We also connected directly to the database to check the tables:

```bash
docker exec -it team-db psql -U atsd -d atsd
```

Inside `psql`, we used:

```sql
\dt
SELECT * FROM usuarios;
```

This confirmed that the application was writing data to the PostgreSQL production database.



## 5) Production profile and database maintenance

The application has a normal `postgres` profile used for development with PostgreSQL. That profile allows Hibernate to create or update tables automatically. This is useful during development, but it is not safe for production because schema changes should be explicit and controlled.

For production, we added a separate profile:

```text
postgres-prod
```

The important difference is:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

With this setting, Hibernate does not create or update tables. It only checks that the existing database schema matches the JPA entities. If something is missing, the application fails during startup. This is safer because the production database is not changed automatically.

The production profile keeps the PostgreSQL configuration variable-based:

```properties
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
DB_USER=atsd
DB_PASSWD=atsd

spring.datasource.url=jdbc:postgresql://${POSTGRES_HOST}:${POSTGRES_PORT}/atsd
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWD}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=validate
```

The application was tested with the production profile using:

```bash
docker run --rm --network team-network -p 8080:8080 hanasl1/todolist-ferovci:1.3.0 --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres --POSTGRES_PORT=5432
```

When the database schema was missing or incomplete, the application failed to start, which is the expected behavior. After restoring the correct schema and applying the migration script, the application started successfully.

On one local Windows setup, PostgreSQL was exposed on host port `5433` because port `5432` was already used by another container. In that case, local Maven execution used:

```text
localhost:5433 -> PostgreSQL container:5432
```

For Docker-to-Docker execution this mapping is not relevant, because the application uses:

```text
postgres:5432
```

---

## 6) Database schema, migration and backup

The production database files are stored in the `sql` directory.

The schema for the previous version is:

```text
sql/schema-1.2.0.sql
```

The schema for the new version is:

```text
sql/schema-1.3.0.sql
```

The schema files were generated with `pg_dump` using the schema-only option:

```bash
pg_dump -U atsd -s atsd > /my-host/sql/schema-1.3.0.sql
```

The migration script is:

```text
sql/schema-1.2.0-1.3.0.sql
```

This script contains the SQL changes needed to update an existing `1.2.0` production database so it matches the `1.3.0` application schema.

In this release, the new `memberCount` and `descriptionPreview` values are calculated in the service layer and stored only in DTOs. They are not new columns in the database. The task title limit is also application-level validation, implemented in the DTO/controller/template layer. Because of that, these changes do not necessarily require physical database schema changes.

If the generated `schema-1.2.0.sql` and `schema-1.3.0.sql` are the same, the migration script can be a no-op with a comment:

```sql
-- Migration from version 1.2.0 to 1.3.0
-- No database schema changes are required for this release.

```

To test the migration, we started an empty production database, restored the previous backup or schema, applied the migration script and then launched the application with the `postgres-prod` profile.

Migration command:

```bash
psql -U atsd atsd < /my-host/sql/schema-1.2.0-1.3.0.sql
```

After applying the migration, the application started correctly with `ddl-auto=validate`, which confirmed that the database schema matched the application model.

### Backup

A database backup was also created after testing the production deployment:

```text
sql/backup06062026.sql
```

Backup command:

```bash
pg_dump -U atsd --clean atsd > /my-host/sql/backup06062026.sql
```

Restore command:

```bash
psql -U atsd atsd < /my-host/sql/backup06062026.sql
```

The backup is included so the tested database state can be restored later if needed.

---

## 7) Development of version 1.3.0 features

## 7.1 Member count for teams

The teams list page was updated to show how many users belong to each team. Before this change, the page showed the team name and available actions, but not the number of members.

The DTO `EquipoData` was extended with:

```java
private Long memberCount;
```

This value is calculated, not persisted. The repository uses a JPQL query with `LEFT JOIN` so teams with no users are also included:

```java
@Query("SELECT e.id AS teamId, COUNT(u) AS memberCount FROM Equipo e LEFT JOIN e.usuarios u GROUP BY e.id")
List<TeamMemberCount> countMembersByTeam();
```

In `EquipoService`, the counts are loaded once and then mapped to the corresponding `EquipoData` objects. If a team does not have any users, the count is set to `0`.

The template `listaEquipos.html` now contains a new column:

```html
<th>Members</th>
<td th:text="${equipo.memberCount != null ? equipo.memberCount : 0}"></td>
```

This makes the teams overview more useful because the user can see the size of each team without opening the detail page.

---

## 7.2 Description preview on team detail page

The team detail page was extended with a small generated preview. The preview combines the team name and the number of members, for example:

```text
Backend Team - 1 member
Frontend Team - 2 members
Empty Team - 0 members
```

The DTO `EquipoData` was extended with:

```java
private String descriptionPreview;
```

The preview is generated in the service layer. This keeps formatting logic out of the Thymeleaf template and makes it easier to test.

```java
private String generateDescriptionPreview(String nombre, Long memberCount) {
    if (nombre == null || nombre.trim().isEmpty()) {
        return null;
    }
    if (memberCount == null) {
        memberCount = 0L;
    }
    String memberText = memberCount == 1 ? "member" : "members";
    return nombre + " - " + memberCount + " " + memberText;
}
```

The method handles singular and plural text and also avoids generating a preview when the team name is empty. The preview is filled when retrieving a team and also when listing teams.

In `descripcionEquipo.html`, the preview is only displayed when it exists:

```html
<div th:if="${equipo.descriptionPreview != null && equipo.descriptionPreview != ''}" class="text-muted mb-2">
    <small th:text="${equipo.descriptionPreview}">Team description preview</small>
</div>
```

This avoids an empty visual block on the page.

---

## 7.3 Task title validation and character counter

The task creation and editing forms were improved by limiting the task title to 200 characters. This was added because very long task titles can make the UI harder to read and should not be accepted by the application.

Validation was added in `TareaData`:

```java
@NotBlank(message = "El titulo es obligatorio")
@Size(max = 200, message = "El titulo no puede superar 200 caracteres")
private String titulo;
```

The controller methods for creating and editing tasks were updated to use `@Valid` and `BindingResult`. If validation fails, the controller returns the same form instead of creating or modifying the task.

For a new task, the controller keeps the user data in the model and returns:

```java
return "formNuevaTarea";
```

For editing a task, it returns:

```java
return "formEditarTarea";
```

Both forms were updated with `maxlength="200"` and a visible counter:

```html
<input class="form-control" id="titulo" maxlength="200" name="titulo" required th:field="*{titulo}" type="text"/>
<small class="form-text text-muted"><span id="tituloCounter">0</span>/200</small>
<small class="text-danger" th:if="${#fields.hasErrors('titulo')}" th:errors="*{titulo}"></small>
```

The counter is updated with JavaScript when the user types. This gives feedback before submitting the form, while server-side validation still protects the application if the user bypasses the HTML limit.

---

## 8) Tests

### Member count for teams

Repository tests verify that the member count query returns the correct values for different cases:

* a team with two members,
* a team with one member,
* a team with zero members.

The web test verifies that the teams list page renders the `Members` column and displays the expected values. This checks both the data passed by the controller and the final HTML rendered to the user.

### Description preview

Service tests verify that the preview is generated correctly for:

* one member,
* multiple members,
* zero members.

The tests also check that the preview is included when teams are retrieved through the team list method.

Controller tests verify the behavior in the rendered team detail page. When the preview exists, it is shown in the page. When the preview is `null`, the preview block is not rendered.

### Task title validation

Controller tests cover both create and edit forms. The tests check that the character counter is present in the HTML and that the JavaScript function is included.

Additional tests submit a title with 201 characters. In both create and edit cases, the application stays on the form and displays the validation message:

```text
El titulo no puede superar 200 caracteres
```

This confirms that the validation is applied on the server side, not only in the browser.

### Docker and production tests

The Docker image was tested by passing runtime parameters to the container. The application correctly loaded the PostgreSQL profile and attempted to connect to the configured host.

The production deployment was tested with two containers connected through `team-network`. The application connected to `team-db` using `postgres:5432`.

The `postgres-prod` profile was tested by starting the application against a production schema. The app only started when the schema matched the JPA entities.

### GitHub Actions

GitHub Actions were used after pushing feature branches and release changes. The final release was checked with the automated test workflow before merging.


---

## 9) Release 1.3.0

The final version was prepared in the branch:

```text
release-1.3.0
```

In this branch, the version was changed from:

```text
1.3.0-SNAPSHOT
```

to:

```text
1.3.0
```

The version was updated in `pom.xml` and on the About page. The About page also contains the release date:

```text
06/06/2026
```

During the release branch work, the database schema for the new version was generated and saved as:

```text
sql/schema-1.3.0.sql
```

It was compared with:

```text
sql/schema-1.2.0.sql
```

The migration file was prepared as:

```text
sql/schema-1.2.0-1.3.0.sql
```

After testing the migration and production profile, the release branch was pushed to GitHub and opened as a pull request into `main`. After the pull request was merged, the release was tagged as:

```text
v1.3.0
```

A GitHub release page was also created from this tag.

The release branch was then merged back into `develop`, so both long-lived branches contained the final release state.

The final Docker image was built and pushed to Docker Hub:

```text
hanasl1/todolist-ferovci:1.3.0
```


## 10) Final summary

Version `1.3.0` adds three small user-facing improvements and the production deployment setup required for the final project.

The new application features improve the team and task pages. The teams list now shows member counts, the team detail page shows a generated preview, and task forms validate title length with a live counter.

The deployment work makes the application easier to run in different environments. The Docker image accepts runtime parameters, the app can connect to PostgreSQL through a Docker network, and the `postgres-prod` profile prevents automatic schema changes in production.

The database state is documented through schema files, a migration script and a backup. The final image is published on Docker Hub and the release is tagged as `v1.3.0`.
