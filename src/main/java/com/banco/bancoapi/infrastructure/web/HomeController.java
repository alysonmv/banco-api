package com.banco.bancoapi.infrastructure.web;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Redireciona a raiz para a documentacao (Swagger UI), para conveniencia ao abrir a app. */
@Hidden
@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "redirect:/swagger-ui.html";
    }
}
