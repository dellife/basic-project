package com.dellife.basicproject.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class BasicController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
}
