package ar.edu.uade.core.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.uade.core.service.KafkaMockService;

@RestController
@RequestMapping(value = "core")
public class CoreController {

    @Autowired
    KafkaMockService kafkaMockService;

}
