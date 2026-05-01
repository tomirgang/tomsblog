package de.tomsblog.blogcontent.adapter.inbound.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web adapter serving Thymeleaf views for the public blog UI.
 *
 * @req SWR-025
 */
@Controller
public class BlogViewController {

    @GetMapping("/")
    public String index() {
        return "index";
    }
}
