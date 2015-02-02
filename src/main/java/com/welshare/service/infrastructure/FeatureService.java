package com.welshare.service.infrastructure;

import java.util.Map;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

@Service
public class FeatureService {

    @Resource(name="featureMap")
    private Map<String, Boolean> features;

    public void enableFeature(String feature) {
        features.put(feature, Boolean.TRUE);
    }
    public void disableFeature(String feature) {
        features.put(feature, Boolean.FALSE);
    }
    public boolean isFeatureActive(String feature) {
        Boolean result = features.get(feature);
        if (result == null) {
            return false;
        }
        return result;
    }
}
