package com.welshare.mock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.welshare.service.EmailService;

@Service
@Profile("test")
public class EmailServiceMock implements EmailService {

    @Override
    public void send(EmailDetails details) {
        // do nothing
    }

}
