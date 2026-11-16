package com.example.myapp.repository;

import com.example.myapp.domain.CardGameRecordPlayer;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CardGameRecordPlayerRepository extends JpaRepository<CardGameRecordPlayer, Long> {

    /**
     * Every seat belonging to the given records, seat order within each. One query for a whole
     * page of records, rather than the per-row lazy load that walking
     * {@code CardGameRecord.getPlayers()} down a page would cause.
     */
    List<CardGameRecordPlayer> findByRecordIdInOrderBySeatAsc(Collection<Long> recordIds);
}
