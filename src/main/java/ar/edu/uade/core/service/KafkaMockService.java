package ar.edu.uade.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class KafkaMockService {

    // NOS INTERESA
    /*
     * Productos: creacion, modificacion parcial(precio,stock), modificacion total, borrado(desactivacion)
     * Marca: creacion, producto agregado, borrado(desactivacion)
     * categoria: creacion, producto agregado, borrado(desactivacion)
     * 
     * Compra: confirmacion de compra, cancelacion de compra 
     */
    
    private static final Logger logger = LoggerFactory.getLogger(KafkaMockService.class);

}
