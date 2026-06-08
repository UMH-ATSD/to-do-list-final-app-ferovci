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

        EquipoData equipo = equipoService.crearEquipo("Proyecto 1", null);
        assertThat(equipo.getId()).isNotNull();

        EquipoData equipoBd = equipoService.recuperarEquipo(equipo.getId());
        assertThat(equipoBd).isNotNull();
        assertThat(equipoBd.getNombre()).isEqualTo("Proyecto 1");
    }

    @Test
    public void renombrarEquipo() {
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1", null);

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
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1", null);
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
        equipoService.crearEquipo("Proyecto BBB", null);
        equipoService.crearEquipo("Proyecto AAA", null);

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
        EquipoData equipo = equipoService.crearEquipo("Proyecto 1", null);

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
        EquipoData equipo1 = equipoService.crearEquipo("Project 1", null);
        EquipoData equipo2 = equipoService.crearEquipo("Project 2", null);
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
        EquipoData equipo = equipoService.crearEquipo("Project 1", null);
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
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1", null);

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
        EquipoData equipoCreado1 = equipoService.crearEquipo("Proyecto 1", null);
        EquipoData equipoCreado2 = equipoService.crearEquipo("Proyecto 2", null);

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
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1", null);

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
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1", null);

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
        assertThatThrownBy(() -> equipoService.crearEquipo(nombreVacio, null))
                .isInstanceOf(EquipoServiceException.class)
                .hasMessage("El equipo no tiene nombre");
    }

    @Test
    public void crearEquipoConNombreNull() {
        // THEN
        // Intentamos crear un equipo con nombre null
        assertThatThrownBy(() -> equipoService.crearEquipo(null, null))
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
        EquipoData equipoCreado = equipoService.crearEquipo("Proyecto 1", null);
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
    public void crearEquipoConDescripcion() {
        // GIVEN
        // We want to create a team with description
        String nombre = "Backend Team";
        String descripcion = "Backend development team";

        // WHEN
        // We create the team
        EquipoData equipoCreado = equipoService.crearEquipo(nombre, descripcion);

        // THEN
        // The team should be created with description
        assertThat(equipoCreado).isNotNull();
        assertThat(equipoCreado.getNombre()).isEqualTo(nombre);
        assertThat(equipoCreado.getDescripcion()).isEqualTo(descripcion);
    }

    @Test
    public void crearEquipoSinDescripcion() {
        // GIVEN
        // We want to create a team without description
        String nombre = "Frontend Team";

        // WHEN
        // We create the team without description
        EquipoData equipoCreado = equipoService.crearEquipo(nombre, null);

        // THEN
        // The team should be created without description
        assertThat(equipoCreado).isNotNull();
        assertThat(equipoCreado.getNombre()).isEqualTo(nombre);
        assertThat(equipoCreado.getDescripcion()).isNull();
    }

    @Test
    public void crearEquipoConDescripcionVacia() {
        // create a team with empty description
        String nombre = "Empty Desc Team";
        String descripcionVacia = "   ";

        // create the team with empty description
        EquipoData equipoCreado = equipoService.crearEquipo(nombre, descripcionVacia);

        // The team should be created without description 
        assertThat(equipoCreado).isNotNull();
        assertThat(equipoCreado.getNombre()).isEqualTo(nombre);
        assertThat(equipoCreado.getDescripcion()).isNull();
    }

    @Test
    public void recuperarEquipoConDescripcion() {
        
        EquipoData equipoCreado = equipoService.crearEquipo("Dev Team", "Development team for backend");

        EquipoData equipoRetrieved = equipoService.recuperarEquipo(equipoCreado.getId());

        assertThat(equipoRetrieved).isNotNull();
        assertThat(equipoRetrieved.getDescripcion()).isEqualTo("Development team for backend");

    }

}
