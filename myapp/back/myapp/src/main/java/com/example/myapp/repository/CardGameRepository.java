package com.example.myapp.repository;

import com.example.myapp.contants.enumeration.CardGameStatus;
import com.example.myapp.domain.CardGame;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CardGameRepository extends JpaRepository<CardGame, Long> {

    /**
     * Games in the given status where the user holds a seat, newest first. Joins through the
     * player table rather than naming columns on the game, because seats are rows — there is
     * no fixed set of username columns the way Gomoku has black/white.
     */
    @Query("""
        select distinct g from CardGame g
        join g.players p
        where g.status = :status
          and p.username = :username
          and p.invitationStatus <> com.example.myapp.contants.enumeration.CardGameInvitationStatus.DECLINED
        order by g.id desc
        """)
    List<CardGame> findByParticipantAndStatus(
        @Param("username") String username,
        @Param("status") CardGameStatus status
    );

    /** Games in any of the given statuses where the user holds a seat, newest first. */
    @Query("""
        select distinct g from CardGame g
        join g.players p
        where g.status in :statuses
          and p.username = :username
          and p.invitationStatus <> com.example.myapp.contants.enumeration.CardGameInvitationStatus.DECLINED
        order by g.id desc
        """)
    List<CardGame> findByParticipantAndStatuses(
        @Param("username") String username,
        @Param("statuses") List<CardGameStatus> statuses
    );
}
