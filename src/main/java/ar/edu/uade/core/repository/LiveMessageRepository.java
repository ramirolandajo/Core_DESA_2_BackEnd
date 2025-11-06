package ar.edu.uade.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.uade.core.model.LiveMessage;

@Repository
public interface LiveMessageRepository extends JpaRepository<LiveMessage, Integer>{
    Optional<LiveMessage> findByEventId(Integer eventId);
}
