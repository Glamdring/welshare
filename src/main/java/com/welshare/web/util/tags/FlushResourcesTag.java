package com.welshare.web.util.tags;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import com.google.common.base.Joiner;
import com.google.common.collect.Multimap;

public class FlushResourcesTag extends SimpleTagSupport {

    private static final String CSS_CONTENT_TYPE = "text/css";
    private static final String JS_CONTENT_TYPE = "text/javascript";

    private String contentType;
    private String assetsVersion;

    @Override
    public void doTag() throws JspException, IOException {
        Multimap<String, String> map = AddResourceTag.resources.get();
        Collection<String> resources = map.get(contentType);

        JspWriter out = getJspContext().getOut();
        PageContext pageContext = (PageContext) getJspContext();

        if (CSS_CONTENT_TYPE.equals(contentType)) {
            out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
        } else if (JS_CONTENT_TYPE.equals(contentType)) {
            out.write("<script type=\"text/javascript\" src=\"");
        } else {
            throw new JspException("Wrong content type passed");
        }
        out.write(pageContext.getServletContext().getContextPath()
                + "/static/" + assetsVersion + "/merge/" + contentType.replace("text/", "")
                + "?resources=");

        out.write(Joiner.on(',').join(resources));

        if (CSS_CONTENT_TYPE.equals(contentType)) {
            out.write("\" />");
        }
        if (JS_CONTENT_TYPE.equals(contentType)) {
            out.write("\"></script>");
        }

        map.removeAll(contentType);
        AddResourceTag.resources.remove();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAssetsVersion() {
        return assetsVersion;
    }

    public void setAssetsVersion(String applicationVersion) {
        this.assetsVersion = applicationVersion;
    }
}
