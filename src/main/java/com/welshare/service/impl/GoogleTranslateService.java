package com.welshare.service.impl;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GoogleTranslateService {
    private static final Logger logger = LoggerFactory.getLogger(GoogleTranslateService.class);

    private static final String GOOGLE_TRANSLATE_URL = "https://www.googleapis.com/language/translate/v2?key={key}&q={q}&target={target}";

    @Value("${google.translate.key}")
    private String key;

    private final RestTemplate restTemplate = new RestTemplate();

    public String translate(String text, String targetLanguage) {
        // not specifying source - google will automatically try to resolve it
        JsonNode node = restTemplate.getForObject(GOOGLE_TRANSLATE_URL, JsonNode.class, key, text, targetLanguage);
        try {
            return node.get("data").get("translations").get(0).get("translatedText").asText();
        } catch (Exception ex) {
            logger.warn("Translation problem: " + ex.getMessage());
            // if anything goes wrong here (missing data from the result), return the original text
            return text;
        }
    }
}
