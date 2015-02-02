package com.welshare.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;

public final class ReflectionUtils {

    private ReflectionUtils() { }

    public static List<BeanProperty> getAnnotatedProperties(Object o, Class<? extends Annotation> a) {
        try {
            List<BeanProperty> result = new ArrayList<BeanProperty>();
            for (Field fld : o.getClass().getDeclaredFields()) {
                if (fld.isAnnotationPresent(a)) {
                    Object value = BeanUtils.getProperty(o, fld.getName());
                    BeanProperty p = new BeanProperty(fld.getName(), value);
                    result.add(p);
                }
            }
            return result;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static BeanProperty getAnnotatedProperty(Object o, Class<? extends Annotation> a) {
        List<BeanProperty> multiple = getAnnotatedProperties(o, a);
        if (multiple.isEmpty()) {
            return null;
        }
        return multiple.get(0);
    }

    public static class BeanProperty {
        private String name;
        private Object value;

        public BeanProperty(String name, Object value) {
            super();
            this.name = name;
            this.value = value;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Object getValue() {
            return value;
        }
        public void setValue(Object value) {
            this.value = value;
        }
    }
}
