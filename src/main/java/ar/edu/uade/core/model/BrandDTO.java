package ar.edu.uade.core.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class BrandDTO {

    Integer id;
    String name;
    List<Integer> products;
    boolean active;
}
