package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller serving the login pages.
 *
 * @req SWR-016
 * @req SWR-028
 * @req SWR-044
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "admin-login";
    }
}
