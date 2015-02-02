package com.welshare.service.social.helper;

import java.net.HttpURLConnection;

import org.springframework.beans.factory.annotation.Value;

import com.restfb.DefaultWebRequestor;

public class ConfigurableFacebookWebRequestor extends DefaultWebRequestor {

    @Value("${facebook.read.timeout}")
    private int readTimeout;

    @Value("${facebook.connect.timeout}")
    private int connectTimeout;

    @Value("${facebook.background.read.timeout}")
    private int backgroundReadTimeout;

    @Value("${facebook.background.connect.timeout}")
    private int backgroundConnectTimeout;

    // field showing whether this requestor is making requests for synchronous
    // processing (e.g.fetching messages and showing to user) or asynchronous
    // (background tasks)
    private boolean background;

    public ConfigurableFacebookWebRequestor() {
        // default constructor for non-background tasks
    }

    public ConfigurableFacebookWebRequestor(boolean background) {
        this.background = background;
    }
    @Override
    protected void customizeConnection(HttpURLConnection connection) {
        connection.setUseCaches(true);
        if (background) {
            connection.setReadTimeout(backgroundReadTimeout);
            connection.setConnectTimeout(backgroundConnectTimeout);
        } else {
            connection.setReadTimeout(readTimeout);
            connection.setConnectTimeout(connectTimeout);
        }
    }
}
