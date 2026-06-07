package todolist.service;

import todolist.dto.EquipoData;
import todolist.dto.UsuarioData;
import todolist.model.Equipo;
import todolist.model.Usuario;
import todolist.repository.EquipoRepository;
import todolist.repository.UsuarioRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

@Service
public class EquipoService {

    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UsuarioRepository usuarioRepository;

    // Se añade un equipo en la aplicación.
    // El nombre debe ser distinto de null
    // El nombre no debe estar registrado en la base de datos
    @Transactional
    public EquipoData registrar(EquipoData equipo) {
        if (equipo.getNombre() == null || equipo.getNombre().trim().isEmpty())
            throw new EquipoServiceException("El equipo no tiene nombre");

        Optional<Equipo> equipoBD = equipoRepository.findByNombre(equipo.getNombre());
        if (equipoBD.isPresent())
            throw new EquipoServiceException("El equipo " + equipo.getNombre() + " ya está registrado");
        else {
            Equipo equipoNuevo = modelMapper.map(equipo, Equipo.class);
            equipoNuevo = equipoRepository.save(equipoNuevo);
            return modelMapper.map(equipoNuevo, EquipoData.class);
        }
    }

    @Transactional(readOnly = true)
    public EquipoData findByNombre(String nombre) {
        Equipo equipo = equipoRepository.findByNombre(nombre).orElse(null);
        if (equipo == null) return null;
        else {
            EquipoData equipoData = modelMapper.map(equipo, EquipoData.class);
            // Populate member count
            List<EquipoRepository.TeamMemberCount> counts = equipoRepository.countMembersByTeam();
            if (counts != null) {
                for (EquipoRepository.TeamMemberCount tmc : counts) {
                    if (tmc != null && tmc.getTeamId() != null && tmc.getTeamId().equals(equipo.getId())) {
                        Long memberCount = tmc.getMemberCount() == null ? 0L : tmc.getMemberCount();
                        equipoData.setMemberCount(memberCount);
                        break;
                    }
                }
            }
            if (equipoData.getMemberCount() == null) {
                equipoData.setMemberCount(0L);
            }
            return equipoData;
        }
    }

    @Transactional(readOnly = true)
    public EquipoData findById(Long equipoId) {
        Equipo equipo = equipoRepository.findById(equipoId).orElse(null);
        if (equipo == null) return null;
        else {
            EquipoData equipoData = modelMapper.map(equipo, EquipoData.class);
            // Populate member count
            List<EquipoRepository.TeamMemberCount> counts = equipoRepository.countMembersByTeam();
            if (counts != null) {
                for (EquipoRepository.TeamMemberCount tmc : counts) {
                    if (tmc != null && tmc.getTeamId() != null && tmc.getTeamId().equals(equipoId)) {
                        Long memberCount = tmc.getMemberCount() == null ? 0L : tmc.getMemberCount();
                        equipoData.setMemberCount(memberCount);
                        break;
                    }
                }
            }
            if (equipoData.getMemberCount() == null) {
                equipoData.setMemberCount(0L);
            }
            return equipoData;
        }
    }

    @Transactional
    public EquipoData crearEquipo(String nombre, String descripcion) {
        if (nombre == null || nombre.trim().isEmpty())
            throw new EquipoServiceException("El equipo no tiene nombre");

        Optional<Equipo> equipoBD = equipoRepository.findByNombre(nombre);
        if (equipoBD.isPresent())
            throw new EquipoServiceException("El equipo " + nombre + " ya está registrado");
        else {
            Equipo equipoNuevo = new Equipo(nombre);
            if (descripcion != null && !descripcion.trim().isEmpty()) {
                equipoNuevo.setDescripcion(descripcion.trim());
            }
            equipoNuevo = equipoRepository.save(equipoNuevo);

            return modelMapper.map(equipoNuevo, EquipoData.class);
        }
    }

    @Transactional
    public EquipoData renombrarEquipo(Long idEquipo, String nuevoNombre) {
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty())
            throw new EquipoServiceException("El equipo no tiene nombre");

        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");

        Optional<Equipo> equipoBD = equipoRepository.findByNombre(nuevoNombre);
        if (equipoBD.isPresent() && !equipoBD.get().getId().equals(idEquipo))
            throw new EquipoServiceException("El equipo " + nuevoNombre + " ya está registrado");

        equipo.setNombre(nuevoNombre);
        equipo = equipoRepository.save(equipo);
        return modelMapper.map(equipo, EquipoData.class);
    }

    @Transactional
    public void eliminarEquipo(Long idEquipo) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");

        for (Usuario usuario : new ArrayList<>(equipo.getUsuarios())) {
            equipo.removeUsuario(usuario);
        }

        equipoRepository.save(equipo);
        equipoRepository.delete(equipo);
    }

    @Transactional
    public EquipoData recuperarEquipo(Long id) {
        Equipo equipo = equipoRepository.findById(id).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");
        EquipoData equipoData = modelMapper.map(equipo, EquipoData.class);
        // Populate member count
        List<EquipoRepository.TeamMemberCount> counts = equipoRepository.countMembersByTeam();
        if (counts != null) {
            for (EquipoRepository.TeamMemberCount tmc : counts) {
                if (tmc != null && tmc.getTeamId() != null && tmc.getTeamId().equals(id)) {
                    Long memberCount = tmc.getMemberCount() == null ? 0L : tmc.getMemberCount();
                    equipoData.setMemberCount(memberCount);
                    break;
                }
            }
        }
        if (equipoData.getMemberCount() == null) {
            equipoData.setMemberCount(0L);
        }
        return equipoData;
    }

    @Transactional
    public List<EquipoData> findAllOrdenadoPorNombre() {
        // recuperamos todos los equipos
        List<Equipo> equipos;
        equipos = equipoRepository.findAll();

        // cambiamos el tipo de la lista de equipos
        List<EquipoData> equiposData = equipos.stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());

        // Fetch member counts in a single query and map by team id
        List<EquipoRepository.TeamMemberCount> counts = equipoRepository.countMembersByTeam();
        Map<Long, Long> countByTeam = new HashMap<>();
        if (counts != null) {
            for (EquipoRepository.TeamMemberCount tmc : counts) {
                if (tmc != null && tmc.getTeamId() != null) {
                    countByTeam.put(tmc.getTeamId(), tmc.getMemberCount() == null ? 0L : tmc.getMemberCount());
                }
            }
        }

        // set memberCount on DTOs (default to 0 if no entry)
        for (EquipoData ed : equiposData) {
            ed.setMemberCount(countByTeam.getOrDefault(ed.getId(), 0L));
        }

        // ordenamos la lista por nombre del equipo
        Collections.sort(equiposData, (a, b) -> a.getNombre().compareTo(b.getNombre()));
        return equiposData;
    }

    @Transactional
    public void añadirUsuarioAEquipo(Long idEquipo, Long idUsuario) {
        // recuperamos el equipo
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null) throw new EquipoServiceException("El equipo no existe");

        // recuperamos el usuario
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) throw new EquipoServiceException("El usuario no existe");

        // comprobamos que el usuario no pertenece al equipo
        if (equipo.getUsuarios().contains(usuario))
            throw new EquipoServiceException("El usuario ya pertenece al equipo");

        // añadimos el usuario al equipo usando el método helper
        equipo.addUsuario(usuario);
        // guardamos el equipo
        equipoRepository.save(equipo);
        // guardamos el usuario
        usuarioRepository.save(usuario);
        // con ello se guarda la relación
    }

    @Transactional
    public List<UsuarioData> usuariosEquipo(Long idEquipo) {
        // recuperamos el equipo
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null)
            throw new EquipoServiceException("El equipo no existe");

        // cambiamos el tipo de la lista de usuarios
        List<UsuarioData> usuarios = equipo.getUsuarios().stream()
                .map(usuario -> modelMapper.map(usuario, UsuarioData.class))
                .collect(Collectors.toList());
        return usuarios;
    }

    @Transactional
    public void quitarUsuarioDeEquipo(Long idEquipo, Long idUsuario) {
        Equipo equipo = equipoRepository.findById(idEquipo).orElse(null);
        if (equipo == null) throw new EquipoServiceException("El equipo no existe");

        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null) throw new EquipoServiceException("El usuario no existe");

        if (!equipo.getUsuarios().contains(usuario))
            throw new EquipoServiceException("El usuario no pertenece al equipo");

        // Removemos el usuario del equipo usando el método helper
        equipo.removeUsuario(usuario);

        equipoRepository.save(equipo);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public List<EquipoData> equiposUsuario(long idUsuario) {
        Usuario usuario = usuarioRepository.findById(idUsuario).orElse(null);
        if (usuario == null)
            throw new EquipoServiceException("El usuario no existe");

        // cambiamos el tipo de la lista de equipos
        List<EquipoData> equipos = usuario.getEquipos().stream()
                .map(equipo -> modelMapper.map(equipo, EquipoData.class))
                .collect(Collectors.toList());
        return equipos;

    }
}

