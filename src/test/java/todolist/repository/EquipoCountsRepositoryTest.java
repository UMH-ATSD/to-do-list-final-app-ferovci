package todolist.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;
import todolist.model.Equipo;
import todolist.model.Usuario;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Sql(scripts = "/clean-db.sql")
public class EquipoCountsRepositoryTest {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @Transactional
    public void countMembersByTeam_returnsCorrectCounts() {
        // GIVEN: three teams, two users; one team with two members, one with one, one with zero
        Equipo teamA = new Equipo("Team A");
        Equipo teamB = new Equipo("Team B");
        Equipo teamC = new Equipo("Team C");
        equipoRepository.save(teamA);
        equipoRepository.save(teamB);
        equipoRepository.save(teamC);

        Usuario user1 = new Usuario("u1@example.com");
        Usuario user2 = new Usuario("u2@example.com");
        usuarioRepository.save(user1);
        usuarioRepository.save(user2);

        // Add user1 to teamA and teamB
        teamA.addUsuario(user1);
        teamB.addUsuario(user1);
        // Add user2 to teamA
        teamA.addUsuario(user2);

        equipoRepository.save(teamA);
        equipoRepository.save(teamB);
        usuarioRepository.save(user1);
        usuarioRepository.save(user2);

        // WHEN
        List<EquipoRepository.TeamMemberCount> counts = equipoRepository.countMembersByTeam();

        // THEN
        Map<Long, Long> countsMap = counts.stream().collect(Collectors.toMap(EquipoRepository.TeamMemberCount::getTeamId, EquipoRepository.TeamMemberCount::getMemberCount));

        assertThat(countsMap.get(teamA.getId())).isEqualTo(2L);
        assertThat(countsMap.get(teamB.getId())).isEqualTo(1L);
        assertThat(countsMap.get(teamC.getId())).isEqualTo(0L);
    }
}

