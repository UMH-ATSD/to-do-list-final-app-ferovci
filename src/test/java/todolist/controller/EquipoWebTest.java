package todolist.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import todolist.authentication.ManagerUserSession;
import todolist.dto.EquipoData;
import todolist.dto.UsuarioData;
import todolist.service.EquipoService;
import todolist.service.UsuarioService;


import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
public class EquipoWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EquipoService equipoService;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private ManagerUserSession managerUserSession;

    @Test
    public void listadoEquiposMuestraLosEquiposYLaNavbar() throws Exception {
        // GIVEN
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        usuario.setEmail("ana@umh.es");
        org.mockito.Mockito.when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo1 = new EquipoData();
        equipo1.setId(1L);
        equipo1.setNombre("Backend");

        EquipoData equipo2 = new EquipoData();
        equipo2.setId(2L);
        equipo2.setNombre("Frontend");

        org.mockito.Mockito.when(equipoService.findAllOrdenadoPorNombre())
                .thenReturn(Arrays.asList(equipo1, equipo2));
        org.mockito.Mockito.when(equipoService.equiposUsuario(10L))
                .thenReturn(Collections.emptyList());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Teams")))
                .andExpect(content().string(containsString("Backend")))
                .andExpect(content().string(containsString("Frontend")))
                .andExpect(content().string(containsString("ToDoList")))
                .andExpect(content().string(containsString("Tasks")));
    }

    @Test
    public void listadoEquiposMuestraJoinYNoLeaveCuandoUsuarioNoEsMiembro() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(Collections.singletonList(equipo));
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/equipos/1/join")))
                .andExpect(content().string(not(containsString("/equipos/1/leaveFromList"))));
    }

    @Test
    public void listadoEquiposMuestraLeaveYNoJoinCuandoUsuarioEsMiembro() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(Collections.singletonList(equipo));
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.singletonList(equipo));

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/equipos/1/leaveFromList")))
                .andExpect(content().string(not(containsString("/equipos/1/join"))));
    }

    @Test
    public void listadoEquiposDevuelve401SiNoHayUsuarioLogeado() throws Exception {
        // GIVEN
        org.mockito.Mockito.when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Usuario no autorizado")));
    }

    @Test
    public void descripcionEquipoMuestraEquipoYMiembros() throws Exception {
        // GIVEN
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        usuario.setEmail("ana@umh.es");
        org.mockito.Mockito.when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        UsuarioData miembro = new UsuarioData();
        miembro.setId(2L);
        miembro.setEmail("member@umh.es");

        org.mockito.Mockito.when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        org.mockito.Mockito.when(equipoService.usuariosEquipo(1L)).thenReturn(java.util.Arrays.asList(miembro));
        org.mockito.Mockito.when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Backend")))
                .andExpect(content().string(containsString("member@umh.es")))
                .andExpect(content().string(containsString("ToDoList")));
    }

    @Test
    public void descripcionEquipoMuestraBotonJoinSiUsuarioNoPertenece() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/equipos/1/join")))
                .andExpect(content().string(not(containsString("/equipos/1/leaveFromDescription"))));
    }

    @Test
    public void descripcionEquipoMuestraBotonLeaveSiUsuarioPertenece() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.singletonList(equipo));

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/equipos/1/leaveFromDescription")))
                .andExpect(content().string(not(containsString("/equipos/1/join"))));
    }

    @Test
    public void descripcionEquipoDevuelve401SiNoHayUsuarioLogeado() throws Exception {
        // GIVEN
        org.mockito.Mockito.when(managerUserSession.usuarioLogeado()).thenReturn(null);

        // WHEN, THEN
        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Usuario no autorizado")));
    }

    @Test
    public void descripcionEquipoMuestraControlesAdminCuandoUsuarioEsAdministrador() throws Exception {
        whenLoggedUserIsPresent();
        when(usuarioService.esAdministrador(10L)).thenReturn(true);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/equipos/1/renombrar")))
                .andExpect(content().string(containsString("/equipos/1/eliminar")));
    }

    @Test
    public void descripcionEquipoNoMuestraControlesAdminParaUsuarioNoAdministrador() throws Exception {
        whenLoggedUserIsPresent();
        when(usuarioService.esAdministrador(10L)).thenReturn(false);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");

        when(equipoService.recuperarEquipo(1L)).thenReturn(equipo);
        when(equipoService.usuariosEquipo(1L)).thenReturn(Collections.emptyList());
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos/1"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("/equipos/1/renombrar"))))
                .andExpect(content().string(not(containsString("/equipos/1/eliminar"))));
    }

    @Test
    public void crearEquipoPostCreaEquipoYRedirige() throws Exception {
        whenLoggedUserIsPresent();
        EquipoData nuevo = new EquipoData();
        nuevo.setId(5L);
        nuevo.setNombre("Infra");
        when(equipoService.crearEquipo("Infra")).thenReturn(nuevo);

        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/equipos/nuevo")
                        .param("nombre", "Infra"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));
    }

    @Test
    public void addUserEndpointAddsUserAndRedirects() throws Exception {
        whenLoggedUserIsPresent();
        UsuarioData usuario = new UsuarioData();
        usuario.setId(2L);
        usuario.setEmail("member@umh.es");
        when(usuarioService.findByEmail("member@umh.es")).thenReturn(usuario);
        doNothing().when(equipoService).añadirUsuarioAEquipo(1L, 2L);

        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/equipos/1/addUser")
                        .param("email", "member@umh.es"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/1"));
    }

    @Test
    public void removeUserEndpointRemovesUserAndRedirects() throws Exception {
        whenLoggedUserIsPresent();
        doNothing().when(equipoService).quitarUsuarioDeEquipo(1L, 2L);

        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/equipos/1/removeUser")
                        .param("userId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos/1"));
    }

    @Test
    public void userAccountPageMuestaPerfil() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        usuario.setEmail("ana@umh.es");
        usuario.setIsAdmin(false);
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo = new EquipoData();
        equipo.setId(1L);
        equipo.setNombre("Backend");
        when(equipoService.equiposUsuario(10L)).thenReturn(java.util.Arrays.asList(equipo));

        this.mockMvc.perform(get("/usuarios/10/cuenta"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My Account")))
                .andExpect(content().string(containsString("ana@umh.es")))
                .andExpect(content().string(containsString("Ana García")))
                .andExpect(content().string(containsString("Backend")))
                .andExpect(content().string(containsString("Leave")));
    }

    @Test
    public void userAccountPageDevuelve401SiNoEsElUsuarioLogeado() throws Exception {
        whenLoggedUserIsPresent();

        this.mockMvc.perform(get("/usuarios/999/cuenta"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Usuario no autorizado")));
    }

    @Test
    public void leaveTeamEndpointRemovesUserAndRedireccionaCuenta() throws Exception {
        whenLoggedUserIsPresent();
        doNothing().when(equipoService).quitarUsuarioDeEquipo(1L, 10L);

        this.mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/equipos/1/leave"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/usuarios/10/cuenta"));
    }

    @Test
    public void joinTeamEndpointAnadeUsuarioYRedirigeAEquipos() throws Exception {
        whenLoggedUserIsPresent();
        doNothing().when(equipoService).añadirUsuarioAEquipo(1L, 10L);

        this.mockMvc.perform(post("/equipos/1/join"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));
    }

    @Test
    public void joinTeamEndpointDevuelve401SinUsuarioLogeado() throws Exception {
        org.mockito.Mockito.when(managerUserSession.usuarioLogeado()).thenReturn(null);

        this.mockMvc.perform(post("/equipos/1/join"))
                .andExpect(status().isUnauthorized())
                .andExpect(status().reason(containsString("Usuario no autorizado")));
    }

    @Test
    public void renombrarEquipoAdminSuccessfullyRenamesAndRedirects() throws Exception {
        whenLoggedUserIsPresent();
        when(usuarioService.esAdministrador(10L)).thenReturn(true);

        EquipoData equipoRenombrado = new EquipoData();
        equipoRenombrado.setId(1L);
        equipoRenombrado.setNombre("Backend Updated");
        when(equipoService.renombrarEquipo(1L, "Backend Updated")).thenReturn(equipoRenombrado);

        this.mockMvc.perform(post("/equipos/1/renombrar")
                        .param("nuevoNombre", "Backend Updated"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));
    }

    @Test
    public void eliminarEquipoAdminSuccessfullyDeletesAndRedirects() throws Exception {
        whenLoggedUserIsPresent();
        when(usuarioService.esAdministrador(10L)).thenReturn(true);

        doNothing().when(equipoService).eliminarEquipo(1L);

        this.mockMvc.perform(post("/equipos/1/eliminar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/equipos"));
    }

    @Test
    public void listadoEquiposMuestraNumeroDeMiembros() throws Exception {
        whenLoggedUserIsPresent();

        UsuarioData usuario = new UsuarioData();
        usuario.setId(10L);
        usuario.setNombre("Ana García");
        when(usuarioService.findById(10L)).thenReturn(usuario);

        EquipoData equipo1 = new EquipoData();
        equipo1.setId(1L);
        equipo1.setNombre("Backend");
        equipo1.setMemberCount(2L);

        EquipoData equipo2 = new EquipoData();
        equipo2.setId(2L);
        equipo2.setNombre("Frontend");
        equipo2.setMemberCount(0L);

        when(equipoService.findAllOrdenadoPorNombre()).thenReturn(java.util.Arrays.asList(equipo1, equipo2));
        when(equipoService.equiposUsuario(10L)).thenReturn(Collections.emptyList());

        this.mockMvc.perform(get("/equipos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Members")))
                .andExpect(content().string(containsString("<td>2</td>")))
                .andExpect(content().string(containsString("<td>0</td>")));
    }

    private void whenLoggedUserIsPresent() {
        org.mockito.Mockito.when(managerUserSession.usuarioLogeado()).thenReturn(10L);
    }
}
