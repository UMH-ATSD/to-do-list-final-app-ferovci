package todolist.service;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import todolist.dto.EquipoData;
import todolist.dto.UsuarioData;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoServiceTest {

    @Autowired
    EquipoService equipoService;

    @Autowired
    UsuarioService usuarioService;

    @Test
    public void crearRecuperarEquipo() {

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }

    @Test
    public void renombrarEquipo() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        EquipoData equipoRenombrado = equipoService.renombrarEquipo(equipo.getId(), "Proyecto 2");

        assertThat(equipoRenombrado.getId()).isEqualTo(equipo.getId());
        assertThat(equipoRenombrado.getNombre()).isEqualTo("Proyecto 2");
        assertThat(equipoService.recuperarEquipo(equipo.getId()).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void eliminarEquipo() {
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuarioRegistrado.getId());

        equipoService.eliminarEquipo(equipoCreado.getId());

        assertThatThrownBy(() -> equipoService.recuperarEquipo(equipoCreado.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El equipo no existe");
        assertThat(equipoService.equiposUsuario(usuarioRegistrado.getId())).isEmpty();
    }

    @Test
    public void listadoEquiposOrdenAlfabetico() {
        // GIVEN
        // Dos equipos en la base de datos
        equipoService.crearEquipo("Proyecto BBB");
        equipoService.crearEquipo("Proyecto AAA");

        // WHEN
        // Recuperamos los equipos
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        // THEN
        // Los equipos están ordenados por nombre
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Proyecto AAA");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Proyecto BBB");
    }

    @Test
    public void añadirUsuarioAEquipoTest() {
        // GIVEN
        // Un usuario y un equipo en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        // Añadimos el usuario al equipo
        equipoService.añadirUsuarioAEquipo(equipo.getId(), usuario.getId());

        // THEN
        // El usuario pertenece al equipo
        List<UsuarioData> usuarios = equipoService.usuariosEquipo(equipo.getId());
        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("user@umh");
    }

    @Test
    public void recuperarEquiposDeUsuario() {
        // GIVEN
        // Un usuario y dos equipos en la base de datos
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);
        EquipoData equipo1 = equipoService.crearEquipo("Project 1");
        EquipoData equipo2 = equipoService.crearEquipo("Project 2");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());
        equipoService.añadirUsuarioAEquipo(equipo2.getId(), usuario.getId());

        // WHEN
        // Recuperamos los equipos del usuario
        List<EquipoData> equipos = equipoService.equiposUsuario(usuario.getId());

        // THEN
        // El usuario pertenece a los dos equipos
        assertThat(equipos).hasSize(2);
        assertThat(equipos.get(0).getNombre()).isEqualTo("Project 1");
        assertThat(equipos.get(1).getNombre()).isEqualTo("Project 2");
    }
    @Test
    public void comprobarExcepciones() {
        // Comprobamos las excepciones lanzadas por los métodos
        // recuperarEquipo, añadirUsuarioAEquipo, usuariosEquipo y equiposUsuario
        assertThatThrownBy(() -> equipoService.recuperarEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(1L, 1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.usuariosEquipo(1L))
                .isInstanceOf(EquipoServiceException.class);
        assertThatThrownBy(() -> equipoService.equiposUsuario(1L))
                .isInstanceOf(EquipoServiceException.class);

        // Creamos un equipo pero no un usuario
        // y comprobamos que también se lanza una excepción
        EquipoData equipo = equipoService.crearEquipo("Project 1");
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipo.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class);
    }

    @Test
    public void quitarUsuarioDeEquipo() {
        // GIVEN
        // Un usuario y un equipo en la base de datos
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1");

        // WHEN
        // Añadimos el usuario al equipo
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuarioRegistrado.getId());

        // THEN
        // El usuario pertenece al equipo
        List<UsuarioData> usuariosDelEquipo = equipoService.usuariosEquipo(equipoCreado.getId());
        assertThat(usuariosDelEquipo).hasSize(1);
        assertThat(usuariosDelEquipo.get(0).getEmail()).isEqualTo("user@umh");

        // WHEN
        // Quitamos el usuario del equipo
        equipoService.quitarUsuarioDeEquipo(equipoCreado.getId(), usuarioRegistrado.getId());

        // THEN
        // El usuario ya no pertenece al equipo
        List<UsuarioData> usuariosAfterRemove = equipoService.usuariosEquipo(equipoCreado.getId());
        assertThat(usuariosAfterRemove).hasSize(0);
    }

    @Test
    public void quitarUsuarioDeEquipoActualizaBidireccional() {
        // GIVEN
        // Un usuario en dos equipos
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);
        EquipoData equipoCreado1 = equipoService.crearEquipo("Proyecto 1");
        EquipoData equipoCreado2 = equipoService.crearEquipo("Proyecto 2");

        // WHEN
        // Añadimos el usuario a ambos equipos
        equipoService.añadirUsuarioAEquipo(equipoCreado1.getId(), usuarioRegistrado.getId());
        equipoService.añadirUsuarioAEquipo(equipoCreado2.getId(), usuarioRegistrado.getId());

        // THEN
        // El usuario pertenece a ambos equipos
        List<EquipoData> equiposDelUsuario = equipoService.equiposUsuario(usuarioRegistrado.getId());
        assertThat(equiposDelUsuario).hasSize(2);

        // WHEN
        // Quitamos el usuario de un equipo
        equipoService.quitarUsuarioDeEquipo(equipoCreado1.getId(), usuarioRegistrado.getId());

        // THEN
        // El usuario solo pertenece a un equipo (se mantiene la relación bidireccional)
        List<EquipoData> equiposAfterRemove = equipoService.equiposUsuario(usuarioRegistrado.getId());
        assertThat(equiposAfterRemove).hasSize(1);
        assertThat(equiposAfterRemove.get(0).getNombre()).isEqualTo("Proyecto 2");
    }

    @Test
    public void quitarUsuarioDeEquipoExcepcionEquipoNoExiste() {
        // GIVEN
        // Un usuario en la base de datos
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);

        // THEN
        // Intentamos quitar el usuario de un equipo que no existe
        assertThatThrownBy(() -> equipoService.quitarUsuarioDeEquipo(1L, usuarioRegistrado.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El equipo no existe");
    }

    @Test
    public void quitarUsuarioDeEquipoExcepcionUsuarioNoExiste() {
        // GIVEN
        // Un equipo en la base de datos
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1");

        // THEN
        // Intentamos quitar un usuario que no existe del equipo
        assertThatThrownBy(() -> equipoService.quitarUsuarioDeEquipo(equipoCreado.getId(), 1L))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El usuario no existe");
    }

    @Test
    public void quitarUsuarioDeEquipoExcepcionUsuarioNoPertenece() {
        // GIVEN
        // Un usuario y un equipo en la base de datos, pero el usuario no está en el equipo
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1");

        // THEN
        // Intentamos quitar el usuario del equipo cuando no pertenece
        assertThatThrownBy(() -> equipoService.quitarUsuarioDeEquipo(equipoCreado.getId(), usuarioRegistrado.getId()))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El usuario no pertenece al equipo");
    }

    @Test
    public void crearEquipoConNombreVacio() {
        // THEN
        // Intentamos crear un equipo con nombre vacío
        String nombreVacio = "";
        assertThatThrownBy(() -> equipoService.crearEquipo(nombreVacio))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El equipo no tiene nombre");
    }

    @Test
    public void crearEquipoConNombreNull() {
        // THEN
        // Intentamos crear un equipo con nombre null
        assertThatThrownBy(() -> equipoService.crearEquipo(null))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El equipo no tiene nombre");
    }

    @Test
    public void añadirUsuarioAEquipoYaExistente() {
        // GIVEN
        // Un usuario en un equipo
        UsuarioData usuarioData = new UsuarioData();
        usuarioData.setEmail("user@umh");
        usuarioData.setPassword("1234");
        UsuarioData usuarioRegistrado = usuarioService.registrar(usuarioData);
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1");
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuarioRegistrado.getId());

        // THEN
        // Intentamos añadir el usuario al equipo nuevamente
        Long equipoId = equipoCreado.getId();
        Long usuarioId = usuarioRegistrado.getId();
        assertThatThrownBy(() -> equipoService.añadirUsuarioAEquipo(equipoId, usuarioId))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El usuario ya pertenece al equipo");
    }

    @Test
    public void descriptionPreviewGeneratedWithCorrectFormat() {
        // GIVEN
        // A team with members
        UsuarioData usuario1 = new UsuarioData();
        usuario1.setEmail("user1@umh");
        usuario1.setPassword("1234");
        usuario1 = usuarioService.registrar(usuario1);

        EquipoData equipoCreado = equipoService.crearEquipo("Backend Team");
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuario1.getId());

        // WHEN
        // We retrieve the team
        EquipoData equipoRetrieved = equipoService.recuperarEquipo(equipoCreado.getId());

        // THEN
        // The preview should be generated with correct format
        assertThat(equipoRetrieved.getDescriptionPreview()).isNotNull();
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("Backend Team");
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("1 member");
    }

    @Test
    public void descriptionPreviewShowsMultipleMembersCorrectly() {
        // GIVEN
        // A team with multiple members
        UsuarioData usuario1 = new UsuarioData();
        usuario1.setEmail("user1@umh");
        usuario1.setPassword("1234");
        usuario1 = usuarioService.registrar(usuario1);

        UsuarioData usuario2 = new UsuarioData();
        usuario2.setEmail("user2@umh");
        usuario2.setPassword("1234");
        usuario2 = usuarioService.registrar(usuario2);

        EquipoData equipoCreado = equipoService.crearEquipo("Frontend Team");
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuario1.getId());
        equipoService.añadirUsuarioAEquipo(equipoCreado.getId(), usuario2.getId());

        // WHEN
        // We retrieve the team
        EquipoData equipoRetrieved = equipoService.recuperarEquipo(equipoCreado.getId());

        // THEN
        // The preview should show 2 members
        assertThat(equipoRetrieved.getDescriptionPreview()).isNotNull();
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("Frontend Team");
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("2 members");
    }

    @Test
    public void descriptionPreviewShowsZeroMembersWhenNoMembers() {
        // GIVEN
        // A team without members
        EquipoData equipoCreado = equipoService.crearEquipo("Empty Team");

        // WHEN
        // We retrieve the team
        EquipoData equipoRetrieved = equipoService.recuperarEquipo(equipoCreado.getId());

        // THEN
        // The preview should show 0 members
        assertThat(equipoRetrieved.getDescriptionPreview()).isNotNull();
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("Empty Team");
        assertThat(equipoRetrieved.getDescriptionPreview()).contains("0 members");
    }

    @Test
    public void descriptionPreviewIncludedInFindAllOrdenadoPorNombre() {
        // GIVEN
        // Multiple teams with different member counts
        UsuarioData usuario = new UsuarioData();
        usuario.setEmail("user@umh");
        usuario.setPassword("1234");
        usuario = usuarioService.registrar(usuario);

        EquipoData equipo1 = equipoService.crearEquipo("Project A");
        EquipoData equipo2 = equipoService.crearEquipo("Project B");
        equipoService.añadirUsuarioAEquipo(equipo1.getId(), usuario.getId());

        // WHEN
        // We retrieve all teams
        List<EquipoData> equipos = equipoService.findAllOrdenadoPorNombre();

        // THEN
        // All teams should have preview populated
        assertThat(equipos).hasSize(2);
        assertThat(equipos.stream().filter(e -> e.getId().equals(equipo1.getId())).findFirst().get().getDescriptionPreview())
                .contains("Project A")
                .contains("1 member");
        assertThat(equipos.stream().filter(e -> e.getId().equals(equipo2.getId())).findFirst().get().getDescriptionPreview())
                .contains("Project B")
                .contains("0 members");
    }

}
