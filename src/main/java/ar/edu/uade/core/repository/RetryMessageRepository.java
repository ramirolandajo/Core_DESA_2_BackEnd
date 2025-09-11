package ar.edu.uade.core.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.uade.core.model.RetryMessage;

@Repository
public interface RetryMessageRepository extends JpaRepository<RetryMessage, Integer>{
    Optional<RetryMessage> findByEventId(Integer eventId);
    Optional<RetryMessage> findByOriginalLiveId(Integer originalLiveId);
}
