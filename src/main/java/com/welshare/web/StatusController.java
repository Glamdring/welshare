package com.welshare.web;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.welshare.service.infrastructure.InternalService;

@Controller
public class StatusController {

    @Inject
    private InternalService internalService;

    @RequestMapping("/app/status")
    @ResponseBody
    public String getStatus() {
        return internalService.getStatus();
    }
}
