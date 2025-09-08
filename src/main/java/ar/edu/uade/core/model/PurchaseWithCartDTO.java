package ar.edu.uade.core.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class PurchaseWithCartDTO {
    private Integer id;
    private LocalDateTime date;
    private LocalDateTime reservationTime;
    private String direction;
    private String status;
    private CartDTO cart;
   
}

