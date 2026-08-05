package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.GomokuGameStatus;
import com.example.myapp.domain.GomokuGame;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface GomokuGameRepository extends JpaRepository<GomokuGame, Long> {

    /** An existing game in one of the given statuses between two users, regardless of colour. */
    @Query("""
        select g from GomokuGame g
        where g.status in :statuses
          and (
            (g.blackUsername = :a and g.whiteUsername = :b)
            or (g.blackUsername = :b and g.whiteUsername = :a)
          )
        order by g.id desc
        """)
    List<GomokuGame> findBetween(
        @Param("a") String a,
        @Param("b") String b,
        @Param("statuses") List<GomokuGameStatus> statuses
    );

    /** The user's current game in the given status (as either colour), newest first. */
    @Query("""
        select g from GomokuGame g
        where g.status = :status
          and (g.blackUsername = :username or g.whiteUsername = :username)
        order by g.id desc
        """)
    List<GomokuGame> findByParticipantAndStatus(
        @Param("username") String username,
        @Param("status") GomokuGameStatus status
    );

    default Optional<GomokuGame> findActiveForUser(String username) {
        return findByParticipantAndStatus(username, GomokuGameStatus.ACTIVE).stream().findFirst();
    }
}
