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
    private Integer productCode;
    private String name;
    private String description;
    private float unitPrice;
    private float discount;
    private int stock;
    private List<Integer> categories;
    private Integer brand;
    private float calification;
    private List<String> images;
    private boolean isNew;
    private boolean isBestSeller;
    private boolean isFeatured;
    private boolean hero; 
    private boolean active;
}