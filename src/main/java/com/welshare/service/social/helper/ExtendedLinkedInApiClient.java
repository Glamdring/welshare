package com.welshare.service.social.helper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;

import com.google.code.linkedinapi.client.LinkedInApiClientException;
import com.google.code.linkedinapi.client.constant.ApplicationConstants;
import com.google.code.linkedinapi.client.constant.LinkedInApiUrls;
import com.google.code.linkedinapi.client.constant.LinkedInApiUrls.LinkedInApiUrlBuilder;
import com.google.code.linkedinapi.client.enumeration.HttpMethod;
import com.google.code.linkedinapi.client.impl.LinkedInApiJaxbClient;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthService;
import com.google.code.linkedinapi.client.oauth.LinkedInOAuthServiceFactory;
import com.google.code.linkedinapi.schema.Error;
import com.google.code.linkedinapi.schema.HttpHeader;
import com.google.code.linkedinapi.schema.Network;
import com.google.code.linkedinapi.schema.Update;

public class ExtendedLinkedInApiClient extends LinkedInApiJaxbClient {

    private static final Charset UTF_8_CHAR_SET = Charset.forName(ApplicationConstants.CONTENT_ENCODING);

    private int connectTimeout;
    private int readTimeout;

    public ExtendedLinkedInApiClient(String consumerKey, String consumerSecret) {
        super(consumerKey, consumerSecret);
    }

    public Network getNetworkUpdates(LinkedInApiUrls.LinkedInApiUrlBuilder builder) {
        return ((Network) readResponse(Network.class, callApiMethod(builder.buildUrl())));
    }

    @Override
    public LinkedInApiUrlBuilder createLinkedInApiUrlBuilder(String urlFormat) {
        return super.createLinkedInApiUrlBuilder(urlFormat);
    }

    protected InputStream callApiMethod(String apiUrl, int expected, List<HttpHeader> httpHeaders) {
        try {
            LinkedInOAuthService oAuthService = LinkedInOAuthServiceFactory.getInstance()
                    .createLinkedInOAuthService(this.getApiConsumer().getConsumerKey(),
                            this.getApiConsumer().getConsumerSecret());
            URL url = new URL(apiUrl);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();

            if (connectTimeout > -1) {
                request.setConnectTimeout(connectTimeout);
            }

            if (readTimeout > -1) {
                request.setReadTimeout(readTimeout);
            }

            for (String headerName : this.getRequestHeaders().keySet()) {
                request.setRequestProperty(headerName, (String) this.getRequestHeaders().get(headerName));
            }

            for (HttpHeader header : httpHeaders) {
                request.setRequestProperty(header.getName(), header.getValue());
            }
            oAuthService.signRequestWithToken(request, this.getAccessToken());
            request.connect();

            if (request.getResponseCode() != expected) {
                Error error = (Error) readResponse(
                        Error.class,
                        getWrappedInputStream(request.getErrorStream(),
                                "gzip".equalsIgnoreCase(request.getContentEncoding())));

                throw createLinkedInApiClientException(error);
            }
            return getWrappedInputStream(request.getInputStream(),
                    "gzip".equalsIgnoreCase(request.getContentEncoding()));
        } catch (IOException e) {
            throw new LinkedInApiClientException(e);
        }
    }

    @Override
    protected InputStream callApiMethod(String apiUrl, String xmlContent, String contentType,
            HttpMethod method, int expected) {

        try {
            LinkedInOAuthService oAuthService = LinkedInOAuthServiceFactory.getInstance()
                    .createLinkedInOAuthService(this.getApiConsumer().getConsumerKey(),
                            this.getApiConsumer().getConsumerSecret());
            URL url = new URL(apiUrl);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();

            if (connectTimeout > 0) {
                request.setConnectTimeout(connectTimeout);
            }

            if (readTimeout > 0) {
                request.setReadTimeout(readTimeout);
            }

            for (String headerName : this.getRequestHeaders().keySet()) {
                request.setRequestProperty(headerName, (String) this.getRequestHeaders().get(headerName));
            }

            request.setRequestMethod(method.fieldName());
            request.setDoOutput(true);
            oAuthService.signRequestWithToken(request, this.getAccessToken());

            if (contentType != null) {
                request.setRequestProperty("Content-Type", contentType);
            }

            if (xmlContent != null) {
                PrintWriter out = new PrintWriter(new OutputStreamWriter(request.getOutputStream(),
                        UTF_8_CHAR_SET));

                out.print(xmlContent);
                out.flush();
                out.close();
            }
            request.connect();
            if (request.getResponseCode() != expected) {
                Error error = (Error) readResponse(
                        Error.class,
                        getWrappedInputStream(request.getErrorStream(),
                                "gzip".equalsIgnoreCase(request.getContentEncoding())));

                throw createLinkedInApiClientException(error);
            }
            return getWrappedInputStream(request.getInputStream(),
                    "gzip".equalsIgnoreCase(request.getContentEncoding()));
        } catch (IOException e) {
            throw new LinkedInApiClientException(e);
        }
    }

    public Update getUpdate(String id) {
        assertNotNullOrEmpty("network update id", id);
        LinkedInApiUrls.LinkedInApiUrlBuilder builder = createLinkedInApiUrlBuilder("http://api.linkedin.com/v1/people/~/network/updates/key={updateKey}");
        String apiUrl = builder.withField("updateKey", id).buildUrl();
        return ((Update) readResponse(Update.class, callApiMethod(apiUrl)));
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
