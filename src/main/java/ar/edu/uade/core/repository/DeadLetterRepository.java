package ar.edu.uade.core.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ar.edu.uade.core.model.DeadLetterMessage;

@Repository
public interface DeadLetterRepository extends JpaRepository<DeadLetterMessage, Integer>{

}

