package ar.edu.uade.core.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class MessageLookupResult {
    private String location; // LIVE, RETRY, DEAD, NOT_FOUND
    private Integer liveId;
    private Integer retryId;
    private Integer eventId;
    private String originModule;
    private String type;
    private String payload;
}

