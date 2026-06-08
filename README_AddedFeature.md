# Team Description Feature

- GitHub repository: [to-do-list-final-app-ferovci](https://github.com/UMH-ATSD/to-do-list-final-app-ferovci.git)
- Docker Hub repository: [todolist-ferovci](https://hub.docker.com/r/hanasl1/todolist-ferovci)
- Snapshot Docker image: [hanasl1/todolist-ferovci:1.4.0-snapshot](https://hub.docker.com/repository/docker/hanasl1/todolist-ferovci/tags/1.4.0-snapshot)


---
## Feature Overview

This update extends the Teams module with an optional team description. Before this change, teams only had a name and a list of members. With this feature, users can add a short description when creating a team so that other users can understand the team's purpose before joining it.

The feature is part of the `1.4.0-SNAPSHOT` development version and introduces a database schema change.

Docker image:

```text
hanasl1/todolist-ferovci:1.4.0-snapshot
```

Migration script:

```text
sql/schema-1.3.0-1.4.0.sql
```

Backup generated after migration testing:

```text
sql/backup-1.4.0-snapshot.sql
```

## Functional Behavior

The feature adds the following user-facing behavior:

- The New Team form includes a new optional description field.
- Users can still create teams using only a team name.
- If a description is provided, it is saved with the team.
- If the description is empty or contains only spaces, it is stored as `NULL`.
- The teams list page displays a short description preview.
- The team detail page displays the full team description.
- Existing teams without descriptions continue working normally.
- Existing team actions such as join, leave, add member and remove member are not affected.

Validation rules:

- Team name is required.
- Team name must remain unique.
- Team description is optional.
- Blank descriptions are not displayed in the UI.
- Teams without descriptions must not show an empty description block.

## Database Migration

The feature adds a new nullable column to the `equipos` table.

Migration file:

```text
sql/schema-1.3.0-1.4.0.sql
```

Migration content:

```sql
ALTER TABLE public.equipos
ADD COLUMN descripcion character varying(500);
```

The column is nullable, so existing teams can remain unchanged. After applying the migration, the `equipos` table contains the new column:

```text
descripcion character varying(500)
```

This schema change is required before running the application with the `postgres-prod` profile, because that profile validates the database schema instead of automatically updating it.

## Backend Implementation

### Entity: `Equipo.java`

The `Equipo` entity was extended with the new `descripcion` attribute:

```java
private String descripcion;
```

Getter and setter:

```java
public String getDescripcion() {
    return descripcion;
}

public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
}
```

The field is mapped by Hibernate to the new `descripcion` column in the `equipos` table.

### DTO: `EquipoData.java`

The DTO was also extended with the same field:

```java
private String descripcion;
```

Getter and setter:

```java
public String getDescripcion() {
    return descripcion;
}

public void setDescripcion(String descripcion) {
    this.descripcion = descripcion;
}
```

This allows the description to be passed between the service layer, controller and Thymeleaf templates.

### Service: `EquipoService.java`

Team creation was updated to support an optional description.

The main creation method now accepts both `nombre` and `descripcion`:

```java
public EquipoData crearEquipo(String nombre, String descripcion) {
    if (nombre == null || nombre.trim().isEmpty())
        throw new EquipoServiceException("El equipo no tiene nombre");

    Optional<Equipo> equipoBD = equipoRepository.findByNombre(nombre);
    if (equipoBD.isPresent())
        throw new EquipoServiceException("El equipo " + nombre + " ya está registrado");

    Equipo equipoNuevo = new Equipo(nombre);

    if (descripcion != null && !descripcion.trim().isEmpty()) {
        equipoNuevo.setDescripcion(descripcion.trim());
    }

    equipoNuevo = equipoRepository.save(equipoNuevo);
    return modelMapper.map(equipoNuevo, EquipoData.class);
}
```

The service keeps the previous validation rules for team names and adds description-specific handling:

- `null` descriptions are allowed,
- blank descriptions are treated as empty and not stored,
- non-empty descriptions are trimmed before saving.

For compatibility with existing code and tests, the previous method signature can be preserved as an overload:

```java
public EquipoData crearEquipo(String nombre) {
    return crearEquipo(nombre, null);
}
```

### Controller: `EquipoController.java`

The controller now receives the `descripcion` parameter from the New Team form and passes it to the service:

```java
@PostMapping("/equipos/nuevo")
public String crearEquipo(String nombre, String descripcion, RedirectAttributes flash) {
    equipoService.crearEquipo(nombre, descripcion);
    return "redirect:/equipos";
}
```

This allows the form field named `descripcion` to be handled without changing the route structure.

## Frontend Implementation

### New Team Form: `formNuevoEquipo.html`

The team creation form now includes a textarea for the optional description:

```html
<div class="form-group">
    <label for="descripcion">Description (optional)</label>
    <textarea id="descripcion"
              name="descripcion"
              class="form-control"
              rows="3"
              placeholder="Brief description of the team's purpose..."></textarea>
</div>
```

The field is optional, so users can submit the form without entering a description.

### Teams List: `listaEquipos.html`

The teams list page now displays a short description preview. If the description is longer than 50 characters, it is shortened and followed by `...`.

Example Thymeleaf logic:

```html
<td>
    <small th:if="${equipo.descripcion != null && equipo.descripcion != ''}"
           th:text="${#strings.length(equipo.descripcion) > 50 ?
           #strings.substring(equipo.descripcion, 0, 50) + '...' : equipo.descripcion}">
    </small>
</td>
```

This gives users a quick overview of the team purpose without opening the detail page.

### Team Detail Page: `descripcionEquipo.html`

The team detail page displays the full description below the team name:

```html
<div th:if="${equipo.descripcion != null && equipo.descripcion != ''}"
     class="text-muted mb-3">
    <p th:text="${equipo.descripcion}">Team description</p>
</div>
```

The block is only rendered when the team has a non-empty description.

## Removed Previous Computed Preview

The previous implementation generated a computed preview from the team name and member count, for example:

```text
Backend - 2 members
```

This was replaced with a real user-provided description.

Removed elements:

- `descriptionPreview` field from `EquipoData`,
- `generateDescriptionPreview()` method from `EquipoService`,
- automatic preview generation based on team name and member count.

The application now stores and displays an actual description written by the user.

## Tests

### Service Tests

`EquipoServiceTest.java` was updated to verify service-level behavior:

- creating a team with a description,
- creating a team without a description,
- creating a team with a blank description,
- retrieving a team and verifying that the description is preserved,
- maintaining existing behavior for team creation and validation.

Example:

```java
@Test
public void crearEquipoConDescripcion() {
    String nombre = "Backend Team";
    String descripcion = "Backend development team";

    EquipoData equipoCreado = equipoService.crearEquipo(nombre, descripcion);

    assertThat(equipoCreado).isNotNull();
    assertThat(equipoCreado.getNombre()).isEqualTo(nombre);
    assertThat(equipoCreado.getDescripcion()).isEqualTo(descripcion);
}
```

### Web Tests

`EquipoWebTest.java` was updated to verify UI behavior:

- the team detail page renders a description when available,
- no empty description section is displayed when the description is missing,
- the teams list displays the description preview,
- the New Team form supports the description field.

Example:

```java
@Test
public void descripcionEquipoMuestraDescripcionCuandoEstaDisponible() throws Exception {
    EquipoData equipo = new EquipoData();
    equipo.setId(1L);
    equipo.setNombre("Backend");
    equipo.setDescripcion("Backend development team");

    when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
    when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());

    this.mockMvc.perform(get("/equipos/1"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Backend development team")));
}
```

Run tests:

```powershell
mvn test
```

Run a full build:

```powershell
mvn clean package
```


## Running the Application


To verify that the database can be migrated from version `1.3.0` to `1.4.0` the application can be started as follows:

1. Start a PostgreSQL container.
2. Restore a `1.3.0` database backup, or use an existing `1.3.0` database state.
3. Apply the migration script:

```text
sql/schema-1.3.0-1.4.0.sql
```

5. Start the application using the `postgres-prod` profile.
6. Confirm that the application starts without schema validation errors.

Migration command inside the PostgreSQL container:

```bash
psql -U atsd atsd < /my-host/sql/schema-1.3.0-1.4.0.sql
```

Schema verification:

```sql
\d equipos
```

Expected column:

```text
descripcion character varying(500)
```

Application command with production profile:

```powershell
docker run -d --name ferovci-app --network team-network -p 8080:8080 hanasl1/todolist-ferovci:1.4.0-snapshot --spring.profiles.active=postgres-prod --POSTGRES_HOST=postgres --POSTGRES_PORT=5432
```


## Backup After Migration

After testing the migration and inserting test data through the application, a backup was created:

```text
sql/backup-1.4.0-snapshot.sql
```

This backup contains a database state after the migration to `1.4.0-SNAPSHOT`.

It can be restored with:

```bash
psql -U atsd atsd < /my-host/sql/backup-1.4.0-snapshot.sql
```

This is useful for quickly reproducing the migrated database state without manually repeating the full migration process.




