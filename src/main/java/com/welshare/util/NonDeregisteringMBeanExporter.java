package com.welshare.util;

import org.springframework.jmx.export.MBeanExporter;

public class NonDeregisteringMBeanExporter extends MBeanExporter {

    private boolean deregisterBeans = true;

    @Override
    protected void unregisterBeans() {
        if (deregisterBeans) {
            super.unregisterBeans();
        }
    }
    public boolean isDeregisterBeans() {
        return deregisterBeans;
    }

    public void setDeregisterBeans(boolean deregisterBeans) {
        this.deregisterBeans = deregisterBeans;
    }
}
