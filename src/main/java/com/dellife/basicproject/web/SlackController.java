package com.dellife.basicproject.web;

import com.github.kingbbode.chatbot.core.common.annotations.Brain;
import com.github.kingbbode.chatbot.core.common.annotations.BrainCell;
import com.github.kingbbode.chatbot.core.common.request.BrainRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Brain
public class SlackController {

    @BrainCell(key = "test", function = "echo-start")
    public String state(BrainRequest brainRequest) {

        log.info(brainRequest.toString());
        return "테스트입니다.";
    }

}
