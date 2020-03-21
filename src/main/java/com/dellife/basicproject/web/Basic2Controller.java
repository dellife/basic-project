package com.dellife.basicproject.web;

import com.dellife.basicproject.dto.BasicRequestDto;
import com.dellife.basicproject.service.BasicService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class Basic2Controller {

    private final BasicService basicService;

    @GetMapping("/hello")
    public String hello3(BasicRequestDto requestDto) {
        return basicService.hello(requestDto.getAge());
    }

    @GetMapping("/hello2")
    public String hello4(BasicRequestDto requestDto) {
        return basicService.hello(requestDto.getAge());
    }
}
