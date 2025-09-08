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
public class ProductDTO {

    private Integer id;
    private String title;
    private String description;
    private Float price;
    private Integer stock;
    private List<String> mediaSrc;
}