package com.aisale.backend.service;

import com.aisale.backend.dto.ChatMessageResponse;
import org.springframework.context.ApplicationEvent;

import lombok.Getter;

@Getter
public class ChatMessageEvent extends ApplicationEvent {

    private final Long targetUserId;
    private final ChatMessageResponse message;

    public ChatMessageEvent(Object source, Long targetUserId, ChatMessageResponse message) {
        super(source);
        this.targetUserId = targetUserId;
        this.message = message;
    }
}
