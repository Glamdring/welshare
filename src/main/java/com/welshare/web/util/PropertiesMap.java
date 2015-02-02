package com.welshare.web.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * Adapts the PropertyPlaceholderConfigurer to the Map interface in order to expose it to jsp and the likes. 
 * @author bozho
 */
public class PropertiesMap extends PropertyPlaceholderConfigurer implements Map<String, String> {

    private Map<String, String> properties;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void processProperties(
            ConfigurableListableBeanFactory beanFactoryToProcess,
            Properties props) throws BeansException {
        properties = new HashMap(props);
        super.processProperties(beanFactoryToProcess, props);
    }

    @Override
    public int size() {
        return properties.size();
    }

    @Override
    public boolean isEmpty() {
        return properties.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return properties.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
       return properties.containsKey(value);
    }

    @Override
    public String get(Object key) {
        return properties.get(key);
    }

    @Override
    public String put(String key, String value) {
        return properties.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return properties.remove(key);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void putAll(Map m) {
        properties.putAll(m);
    }

    @Override
    public void clear() {
        properties.clear();

    }

    @Override
    public Set<String> keySet() {
        return properties.keySet();
    }

    @Override
    public Collection<String> values() {
        return properties.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return properties.entrySet();
    }

}
