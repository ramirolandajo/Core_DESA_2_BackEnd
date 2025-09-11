package ar.edu.uade.core.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.uade.core.model.MessageConsumption;

@Repository
public interface MessageConsumptionRepository extends JpaRepository<MessageConsumption, Integer>{
    boolean existsByEventIdAndModuleName(Integer eventId, String moduleName);
    List<MessageConsumption> findByEventId(Integer eventId);
    List<MessageConsumption> findByLiveMessageId(Integer liveMessageId);
}
