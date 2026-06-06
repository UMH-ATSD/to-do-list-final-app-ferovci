package todolist.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import todolist.model.Equipo;
import todolist.model.Usuario;
import java.util.List;

import java.util.Optional;

public interface EquipoRepository extends CrudRepository<Equipo, Long>{
    Optional<Equipo> findByNombre(String s);

    public List<Equipo> findAll();

    // Projection interface for team member counts
    interface TeamMemberCount {
        Long getTeamId();
        Long getMemberCount();
    }

    // JPQL query returning team id and member count (includes teams with zero members)
    @Query("SELECT e.id AS teamId, COUNT(u) AS memberCount FROM Equipo e LEFT JOIN e.usuarios u GROUP BY e.id")
    List<TeamMemberCount> countMembersByTeam();
}
