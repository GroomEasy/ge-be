package com.ceos.menual.domain.email.service;

public interface EmailSender {

    /**
     * 이메일 전송
     * @param to 수신자 이메일
     * @param subject 제목
     * @param body 본문
     */
    void sendEmail(String to, String subject, String body);
}