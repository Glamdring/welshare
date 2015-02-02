package com.welshare.web.util.tags;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;

public class AddResourceTag extends SimpleTagSupport {

    public static final ThreadLocal<Multimap<String, String>> resources = new ThreadLocal<Multimap<String, String>>();

    private String resourcePath;
    private String contentType;

    @Override
    public void doTag() throws JspException, IOException {
       if (resources.get() == null) {
           resources.set(LinkedListMultimap.<String, String>create());
       }

       resources.get().put(contentType, resourcePath);
       super.doTag();
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
