package ar.edu.uade.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class ConsumeResult {
    public enum Status { RETRY, DEAD, CONFLICT, NOT_FOUND }

    private Status status;
    private RetryMessage retryMessage;
    private DeadLetterMessage deadLetterMessage;
    private String message;

    public static ConsumeResult ofRetry(RetryMessage rm){
        ConsumeResult r = new ConsumeResult(); r.status = Status.RETRY; r.retryMessage = rm; return r;
    }
    public static ConsumeResult ofDead(DeadLetterMessage dm){
        ConsumeResult r = new ConsumeResult(); r.status = Status.DEAD; r.deadLetterMessage = dm; return r;
    }
    public static ConsumeResult ofConflict(String msg){
        ConsumeResult r = new ConsumeResult(); r.status = Status.CONFLICT; r.message = msg; return r;
    }
    public static ConsumeResult ofNotFound(){
        ConsumeResult r = new ConsumeResult(); r.status = Status.NOT_FOUND; return r;
    }
}

