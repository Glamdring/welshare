package com.welshare.util;

import java.util.Map;
import java.util.Properties;

import org.apache.commons.beanutils.BeanUtils;

import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationContext;

public class TwitterConfigurationHolder implements Configuration {

    private boolean debugEnabled;
    private String source;
    private String userAgent;
    private String user;
    private String password;
    private boolean useSSL;
    private String httpProxyHost;
    private String httpProxyUser;
    private String httpProxyPassword;
    private int httpProxyPort;
    private int httpConnectionTimeout;
    private int httpReadTimeout;
    private int httpStreamingReadTimeout;
    private int httpRetryCount;
    private int httpRetryIntervalSeconds;
    private int httpMaxTotalConnections;
    private int httpDefaultMaxPerRoute;
    private String oAuthConsumerKey;
    private String oAuthConsumerSecret;
    private String oAuthAccessToken;
    private String oAuthAccessTokenSecret;
    private String oAuthRequestTokenURL;
    private String oAuthAuthorizationURL;
    private String oAuthAccessTokenURL;
    private String oAuthAuthenticationURL;
    private String restBaseURL;
    private String searchBaseURL;
    private String streamBaseURL;
    private String userStreamBaseURL;
    private String siteStreamBaseURL;
    private String dispatcherImpl;
    private int asyncNumThreads;
    private String clientVersion;
    private String clientURL;
    private Map<String, String> requestHeaders;
    private boolean includeRTsEnabled;
    private boolean userStreamRepliesAllEnabled;
    private String mediaProvider;
    private String mediaProviderAPIKey;
    private boolean includeEntitiesEnabled;
    private boolean prettyDebugEnabled;
    private boolean GZIPEnabled;
    private boolean JSONStoreEnabled;
    private boolean MBeanEnabled;
    private Properties mediaProviderParameters;
    private boolean GAE;
    private String uploadBaseURL;
    private String loggerFactory;
    private boolean includeMyRetweetEnabled;
    private boolean stallWarningsEnabled;
    private long contributingTo;

    public TwitterConfigurationHolder() {
        // not-so-beautiful, but the easiest way to springify
        // twitterj, which has too many package-private and final classes
        // + some defaults
        Configuration defaults = ConfigurationContext.getInstance();
        try {
            BeanUtils.copyProperties(this, defaults);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }
    public void setDebugEnabled(boolean debug) {
        this.debugEnabled = debug;
    }
    public String getSource() {
        return source;
    }
    public void setSource(String source) {
        this.source = source;
    }
    public String getUserAgent() {
        return userAgent;
    }
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
    public String getUser() {
        return user;
    }
    public void setUser(String user) {
        this.user = user;
    }
    public String getPassword() {
        return password;
    }
    public void setPassword(String password) {
        this.password = password;
    }
    public boolean isUseSSL() {
        return useSSL;
    }
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }
    public String getHttpProxyHost() {
        return httpProxyHost;
    }
    public void setHttpProxyHost(String httpProxyHost) {
        this.httpProxyHost = httpProxyHost;
    }
    public String getHttpProxyUser() {
        return httpProxyUser;
    }
    public void setHttpProxyUser(String httpProxyUser) {
        this.httpProxyUser = httpProxyUser;
    }
    public String getHttpProxyPassword() {
        return httpProxyPassword;
    }
    public void setHttpProxyPassword(String httpProxyPassword) {
        this.httpProxyPassword = httpProxyPassword;
    }
    public int getHttpProxyPort() {
        return httpProxyPort;
    }
    public void setHttpProxyPort(int httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }
    public int getHttpConnectionTimeout() {
        return httpConnectionTimeout;
    }
    public void setHttpConnectionTimeout(int httpConnectionTimeout) {
        this.httpConnectionTimeout = httpConnectionTimeout;
    }
    public int getHttpReadTimeout() {
        return httpReadTimeout;
    }
    public void setHttpReadTimeout(int httpReadTimeout) {
        this.httpReadTimeout = httpReadTimeout;
    }
    public int getHttpStreamingReadTimeout() {
        return httpStreamingReadTimeout;
    }
    public void setHttpStreamingReadTimeout(int httpStreamingReadTimeout) {
        this.httpStreamingReadTimeout = httpStreamingReadTimeout;
    }
    public int getHttpRetryCount() {
        return httpRetryCount;
    }
    public void setHttpRetryCount(int httpRetryCount) {
        this.httpRetryCount = httpRetryCount;
    }
    public int getHttpRetryIntervalSeconds() {
        return httpRetryIntervalSeconds;
    }
    public void setHttpRetryIntervalSeconds(int httpRetryIntervalSeconds) {
        this.httpRetryIntervalSeconds = httpRetryIntervalSeconds;
    }
    public int getHttpMaxTotalConnections() {
        return httpMaxTotalConnections;
    }
    public void setHttpMaxTotalConnections(int maxTotalConnections) {
        this.httpMaxTotalConnections = maxTotalConnections;
    }
    public int getHttpDefaultMaxPerRoute() {
        return httpDefaultMaxPerRoute;
    }
    public void setHttpDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.httpDefaultMaxPerRoute = defaultMaxPerRoute;
    }
    public String getOAuthConsumerKey() {
        return oAuthConsumerKey;
    }
    public void setOAuthConsumerKey(String oAuthConsumerKey) {
        this.oAuthConsumerKey = oAuthConsumerKey;
    }
    public String getOAuthConsumerSecret() {
        return oAuthConsumerSecret;
    }
    public void setOAuthConsumerSecret(String oAuthConsumerSecret) {
        this.oAuthConsumerSecret = oAuthConsumerSecret;
    }
    public String getOAuthAccessToken() {
        return oAuthAccessToken;
    }
    public void setOAuthAccessToken(String oAuthAccessToken) {
        this.oAuthAccessToken = oAuthAccessToken;
    }
    public String getOAuthAccessTokenSecret() {
        return oAuthAccessTokenSecret;
    }
    public void setOAuthAccessTokenSecret(String oAuthAccessTokenSecret) {
        this.oAuthAccessTokenSecret = oAuthAccessTokenSecret;
    }
    public String getOAuthRequestTokenURL() {
        return oAuthRequestTokenURL;
    }
    public void setOAuthRequestTokenURL(String oAuthRequestTokenURL) {
        this.oAuthRequestTokenURL = oAuthRequestTokenURL;
    }
    public String getOAuthAuthorizationURL() {
        return oAuthAuthorizationURL;
    }
    public void setOAuthAuthorizationURL(String oAuthAuthorizationURL) {
        this.oAuthAuthorizationURL = oAuthAuthorizationURL;
    }
    public String getOAuthAccessTokenURL() {
        return oAuthAccessTokenURL;
    }
    public void setOAuthAccessTokenURL(String oAuthAccessTokenURL) {
        this.oAuthAccessTokenURL = oAuthAccessTokenURL;
    }
    public String getOAuthAuthenticationURL() {
        return oAuthAuthenticationURL;
    }
    public void setOAuthAuthenticationURL(String oAuthAuthenticationURL) {
        this.oAuthAuthenticationURL = oAuthAuthenticationURL;
    }
    public String getRestBaseURL() {
        return restBaseURL;
    }
    public void setRestBaseURL(String restBaseURL) {
        this.restBaseURL = restBaseURL;
    }
    public String getSearchBaseURL() {
        return searchBaseURL;
    }
    public void setSearchBaseURL(String searchBaseURL) {
        this.searchBaseURL = searchBaseURL;
    }
    public String getStreamBaseURL() {
        return streamBaseURL;
    }
    public void setStreamBaseURL(String streamBaseURL) {
        this.streamBaseURL = streamBaseURL;
    }
    public String getUserStreamBaseURL() {
        return userStreamBaseURL;
    }
    public void setUserStreamBaseURL(String userStreamBaseURL) {
        this.userStreamBaseURL = userStreamBaseURL;
    }
    public String getDispatcherImpl() {
        return dispatcherImpl;
    }
    public void setDispatcherImpl(String dispatcherImpl) {
        this.dispatcherImpl = dispatcherImpl;
    }
    public int getAsyncNumThreads() {
        return asyncNumThreads;
    }
    public void setAsyncNumThreads(int asyncNumThreads) {
        this.asyncNumThreads = asyncNumThreads;
    }
    public String getClientVersion() {
        return clientVersion;
    }
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
    }
    public String getClientURL() {
        return clientURL;
    }
    public void setClientURL(String clientURL) {
        this.clientURL = clientURL;
    }
    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }
    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }
    public boolean isDalvik() {
        return false;
    }
    public boolean isIncludeRTsEnabled() {
        return includeRTsEnabled;
    }
    public void setIncludeRTsEnabled(boolean includeRTsEnabled) {
        this.includeRTsEnabled = includeRTsEnabled;
    }
    public boolean isUserStreamRepliesAllEnabled() {
        return userStreamRepliesAllEnabled;
    }
    public void setUserStreamRepliesAllEnabled(boolean userStreamRepliesAllEnabled) {
        this.userStreamRepliesAllEnabled = userStreamRepliesAllEnabled;
    }
    public String getSiteStreamBaseURL() {
        return siteStreamBaseURL;
    }
    public void setSiteStreamBaseURL(String siteStreamBaseURL) {
        this.siteStreamBaseURL = siteStreamBaseURL;
    }
    public String getMediaProvider() {
        return mediaProvider;
    }
    public void setMediaProvider(String mediaProvider) {
        this.mediaProvider = mediaProvider;
    }
    public String getMediaProviderAPIKey() {
        return mediaProviderAPIKey;
    }
    public void setMediaProviderAPIKey(String mediaProviderAPIKey) {
        this.mediaProviderAPIKey = mediaProviderAPIKey;
    }
    public boolean isIncludeEntitiesEnabled() {
        return includeEntitiesEnabled;
    }
    public void setIncludeEntitiesEnabled(boolean includeEntitiesEnabled) {
        this.includeEntitiesEnabled = includeEntitiesEnabled;
    }
    public boolean isPrettyDebugEnabled() {
        return prettyDebugEnabled;
    }
    public void setPrettyDebugEnabled(boolean prettyDebugEnabled) {
        this.prettyDebugEnabled = prettyDebugEnabled;
    }
    public boolean isGZIPEnabled() {
        return GZIPEnabled;
    }
    public void setGZIPEnabled(boolean gZIPEnabled) {
        GZIPEnabled = gZIPEnabled;
    }
    public boolean isJSONStoreEnabled() {
        return JSONStoreEnabled;
    }
    public void setJSONStoreEnabled(boolean jSONStoreEnabled) {
        JSONStoreEnabled = jSONStoreEnabled;
    }
    public boolean isMBeanEnabled() {
        return MBeanEnabled;
    }
    public void setMBeanEnabled(boolean mBeanEnabled) {
        MBeanEnabled = mBeanEnabled;
    }
    public Properties getMediaProviderParameters() {
        return mediaProviderParameters;
    }
    public void setMediaProviderParameters(Properties mediaProviderParameters) {
        this.mediaProviderParameters = mediaProviderParameters;
    }
    public boolean isGAE() {
        return GAE;
    }
    public void setGAE(boolean gAE) {
        GAE = gAE;
    }

    public String getUploadBaseURL() {
        return uploadBaseURL;
    }

    public void setUploadBaseURL(String uploadBaseURL) {
        this.uploadBaseURL = uploadBaseURL;
    }

    public String getLoggerFactory() {
        return loggerFactory;
    }

    public void setLoggerFactory(String loggerFactory) {
        this.loggerFactory = loggerFactory;
    }

    public boolean isIncludeMyRetweetEnabled() {
        return includeMyRetweetEnabled;
    }

    public void setIncludeMyRetweetEnabled(boolean includeMyRetweetEnabled) {
        this.includeMyRetweetEnabled = includeMyRetweetEnabled;
    }

    public boolean isStallWarningsEnabled() {
        return stallWarningsEnabled;
    }

    public void setStallWarningsEnabled(boolean stallWarningsEnabled) {
        this.stallWarningsEnabled = stallWarningsEnabled;
    }

    public long getContributingTo() {
        return contributingTo;
    }

    public void setContributingTo(long contributingTo) {
        this.contributingTo = contributingTo;
    }
}
