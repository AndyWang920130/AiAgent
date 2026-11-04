package com.example.myapp.repository;

import com.example.myapp.domain.CardGame;
import com.example.myapp.domain.CardGamePlayer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardGamePlayerRepository extends JpaRepository<CardGamePlayer, Long> {

    /** Every seat at a table, in seat order — the order turns and refill draws follow. */
    List<CardGamePlayer> findByGameOrderBySeatAsc(CardGame game);

    Optional<CardGamePlayer> findByGameAndUsername(CardGame game, String username);
}
