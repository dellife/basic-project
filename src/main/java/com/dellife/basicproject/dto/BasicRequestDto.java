package com.dellife.basicproject.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BasicRequestDto {

    private String name;
    private long age;

    public BasicRequestDto(String name, long age) {
        this.name = name;
        this.age = age;
    }
}
