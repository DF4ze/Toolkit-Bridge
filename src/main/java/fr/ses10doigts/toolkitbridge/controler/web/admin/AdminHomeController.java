package fr.ses10doigts.toolkitbridge.controler.web.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHomeController {

    @GetMapping({"/admin", "/admin/"})
    public String adminHome() {
        return "admin/index";
    }
}
