package com.dellife.basicproject.service;

import com.dellife.basicproject.dto.BasicRequestDto;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@SpringBootTest
public class BasicServiceTest {

    @Autowired
    private BasicService basicService;

    @Test
    public void name() {
        //given
        BasicRequestDto requestDto = new BasicRequestDto("name", 10);

        //when
        String result = basicService.hello(requestDto.getAge());

        //then
        assertThat(result).isEqualTo("1");
    }
}
