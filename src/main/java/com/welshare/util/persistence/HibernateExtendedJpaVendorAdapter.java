package com.welshare.util.persistence;

import java.util.Map;

import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class HibernateExtendedJpaVendorAdapter extends HibernateJpaVendorAdapter {

    private Map<String, Object> vendorProperties;

    @Override
    public Map<String, Object> getJpaPropertyMap() {
        Map<String, Object> properties = super.getJpaPropertyMap();
        properties.putAll(vendorProperties);
        return properties;
    }

    public Map<String, Object> getVendorProperties() {
        return vendorProperties;
    }

    public void setVendorProperties(Map<String, Object> vendorProperties) {
        this.vendorProperties = vendorProperties;
    }

}
