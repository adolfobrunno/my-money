package br.com.abba.soft.mymoney.infrastructure.web.mvc;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/api", "/docs"})
    public String index() {
        // Redirect to the new login page as the entry point for the mobile-like UI
        return "redirect:/login.html";
    }
}
