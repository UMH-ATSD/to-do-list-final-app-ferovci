# ToDoList App - Technical Documentation (v1.2.0)

ToDoList app using Spring Boot, Thymeleaf templates, Spring Data JPA, and H2 database.

Project owner: **Nikola Jamic**.

Developer-facing documentation for the v1.2.0 delivery of the Spring Boot ToDoList project.

## Repository and Delivery URLs

- GitHub repository: https://github.com/Njamic-27/p2-todolist-app
- DockerHub repository: https://hub.docker.com/r/njamic/p2-todolistapp
- DockerHub tags page: https://hub.docker.com/r/njamic/p2-todolistapp/tags
- Trello board: https://trello.com/invite/b/69b8392e314edc5e78153591/ATTIb9faf682c49f33f3b63bc01d06bd8e6865DEB8FB/e2-to-do-list-app

## Docker Images (Published Tags)

The following links point to the real published tags in DockerHub:

| Tag | DockerHub link | Pull command |
|---|---|---|
| `latest` | https://hub.docker.com/r/njamic/p2-todolistapp/tags?name=latest | `docker pull njamic/p2-todolistapp:latest` |
| `1.2.0` | https://hub.docker.com/r/njamic/p2-todolistapp/tags?name=1.2.0 | `docker pull njamic/p2-todolistapp:1.2.0` |
| `1.1.0` | https://hub.docker.com/r/njamic/p2-todolistapp/tags?name=1.1.0 | `docker pull njamic/p2-todolistapp:1.1.0` |
| `1.0.1` | https://hub.docker.com/r/njamic/p2-todolistapp/tags?name=1.0.1 | `docker pull njamic/p2-todolistapp:1.0.1` |

## How to run the Docker container

The image supports two runtime configurations. Both `1.2.0` and `latest` tags point to the same image.

### Option A: Run with H2 in-memory database

For quick testing and development:

```powershell
docker run --rm -p 8080:8080 njamic/p2-todolistapp:latest
```

### Option B: Run with PostgreSQL database

For production or when you need persistent data storage:

```powershell
docker run --rm -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=postgres `
  -e POSTGRES_HOST=host.docker.internal `
  -e POSTGRES_PORT=5433 `
  -e POSTGRES_USER=atsd `
  -e POSTGRES_PASSWORD=atsd `
  njamic/p2-todolistapp:latest
```

Then open `http://localhost:8080` in your browser.

## Technical Evolution Summary

Version 1.2.0 extends the project with team-management features and a refreshed delivery state while preserving the previous published Docker tags. The objective of this release is to keep the app aligned with the completed stories and to document the current DockerHub images clearly.

Previous releases remain available on DockerHub as `1.1.0` and `1.0.1`, while the new release is published as both `1.2.0` and `latest`.

The earlier 1.1.0 baseline introduced navigation standardization and administrator workflows. That work implemented the mandatory requirements (shared menu bar, registered users list, and user description page) and optional requirements (single admin registration, admin-only protection for user listing/description, and admin block/unblock operations). Implementation touched all layers: Thymeleaf templates, controllers, service logic, repository queries, and automated tests.

At the presentation layer, navbar behavior is centralized in `src/main/resources/templates/fragments.html` using `guestNavbar` and `userNavbar(usuarioId, usuarioNombre)`. This reduced duplication and made the About page dynamic: guests see login/register links, while logged-in users see ToDoList, Tasks, and account/logout options. The two new user-management views are `src/main/resources/templates/listaUsuarios.html` and `src/main/resources/templates/descripcionUsuario.html`. Both render the common user navbar and are focused on admin actions. `listaUsuarios.html` includes enable/disable actions and links to each user description; `descripcionUsuario.html` intentionally excludes passwords.

In the controller layer, `src/main/java/todolist/controller/UsuarioController.java` owns `/registered`, `/registered/{id}`, `/registered/{id}/block`, and `/registered/{id}/unblock`. Access control is enforced in `comprobarAdministradorLogeado()`, which rejects anonymous users and non-admin users with HTTP 401 through custom exceptions. A helper (`anadirDatosNavbar(Model)`) injects the navbar model attributes so protected pages have consistent navigation context.

`src/main/java/todolist/controller/LoginController.java` was extended so successful admin login redirects to `/registered` instead of the personal tasks route. Registration handling now supports optional admin creation through the `isAdmin` form field, while still preserving validation and duplicate-email checks. The registration page displays the admin checkbox only when `usuarioService.existsAdmin()` is false, enforcing the single-admin rule at UI level as well.

Service and repository evolution is centered in `src/main/java/todolist/service/UsuarioService.java` and `src/main/java/todolist/repository/UsuarioRepository.java`. New/updated service methods include `registrar(UsuarioData, boolean)`, `existsAdmin()`, `getAdmin()`, `esAdministrador(Long)`, `bloquearUsuario(Long)`, `desbloquearUsuario(Long)`, and `findAllUsuarios()`. Repository support includes `findByIsAdminTrue()` and blocked-state lookups. Domain behavior now includes explicit login outcomes via `LoginStatus.USER_BLOCKED`, allowing the controller to return a clear UX error for blocked accounts.

## New or Updated Classes, Methods, and Templates

- Controller classes and methods:
  - `LoginController.loginSubmit(...)` admin redirect and blocked-user handling.
  - `LoginController.registroForm(...)` and `LoginController.registroSubmit(...)` with conditional admin registration.
  - `UsuarioController.registeredUsers(...)`, `registeredUserDescription(...)`, `bloquearUsuario(...)`, `desbloquearUsuario(...)`, plus helper methods for role checks/navbar data.
- Service/repository methods:
  - `UsuarioService.registrar(UsuarioData, boolean)`, `existsAdmin()`, `getAdmin()`, `esAdministrador(Long)`, `bloquearUsuario(Long)`, `desbloquearUsuario(Long)`, `findAllUsuarios()`.
  - `UsuarioRepository.findByIsAdminTrue()` and blocked-state query support.
- Thymeleaf templates added or significantly extended:
  - `listaUsuarios.html`
  - `descripcionUsuario.html`
  - `fragments.html` (shared navbar fragments)
  - `formRegistro.html` (conditional admin checkbox)

## Tests Implemented

Testing is split across web/controller, service, and repository suites under `src/test/java/todolist`. `UsuarioWebTest` validates end-to-end HTTP behavior for the new routes and role restrictions: admin redirect on login, admin checkbox visibility logic, 401 responses for non-admin/anonymous access to protected pages, navbar rendering on registered pages, and block/unblock endpoint behavior. `UsuarioServiceTest` validates business rules that must remain stable independent of web flow: single-admin enforcement, admin detection, blocked-user login status, block/unblock state transitions, and prevention of blocking the admin account. Repository tests (`UsuarioTest`) cover persistence and query-level correctness for user flags.

This strategy gives confidence at multiple levels: controller tests catch route/view regressions, service tests guard business invariants, and repository tests verify data access contracts.

## Source Code Example and Explanation

The following method is central to v1.1.0 because it enforces a hard business rule (maximum one admin) before persistence:

```java
@Transactional
public UsuarioData registrar(UsuarioData usuario, boolean isAdmin) {
    Optional<Usuario> usuarioBD = usuarioRepository.findByEmail(usuario.getEmail());
    if (usuarioBD.isPresent())
        throw new UsuarioServiceException("El usuario " + usuario.getEmail() + " ya está registrado");
    else if (usuario.getEmail() == null)
        throw new UsuarioServiceException("El usuario no tiene email");
    else if (usuario.getPassword() == null)
        throw new UsuarioServiceException("El usuario no tiene password");
    else {
        if (isAdmin && existsAdmin()) {
            throw new UsuarioServiceException("Ya existe un administrador en el sistema");
        }

        Usuario usuarioNuevo = modelMapper.map(usuario, Usuario.class);
        usuarioNuevo.setIsAdmin(isAdmin);
        usuarioNuevo = usuarioRepository.save(usuarioNuevo);
        return modelMapper.map(usuarioNuevo, UsuarioData.class);
    }
}
```

Why this is relevant: the check is executed at service level (not only in the view), so the invariant is protected even if future clients bypass the registration form. This design keeps the rule close to domain logic and makes it testable via unit/service tests.

[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/rOF8_HM_)
