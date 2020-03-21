package com.dellife.basicproject.service;

import org.springframework.stereotype.Service;

@Service
public class BasicService {

    public String hello(long number) {

        if (number < 5) {
            return "0";
        }

        return "1";
    }
}
