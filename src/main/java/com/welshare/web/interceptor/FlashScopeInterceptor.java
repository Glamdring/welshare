package com.welshare.web.interceptor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.collections.map.CompositeMap;
import org.apache.commons.collections.map.CompositeMap.MapMutator;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * Captures flashScope attribute from Controllers and puts it in the session for
 * reuse at the next request. Typical usecase: Controller1 returns new
 * ModelAndView("redirect:controller2", "flashScope.error", "message.error") in
 * Controller2's view, you can use ${flashScope.error}
 */
public class FlashScopeInterceptor implements HandlerInterceptor {
    public static final String DEFAULT_ATTRIBUTE_NAME = "flashScope";
    public static final String DEFAULT_SESSION_ATTRIBUTE_NAME = FlashScopeInterceptor.class
            .getName();
    public static final int DEFAULT_RETENTION_COUNT = 2;

    private String sessionAttributeName = DEFAULT_SESSION_ATTRIBUTE_NAME;
    private String attributeName = DEFAULT_ATTRIBUTE_NAME;
    private int retentionCount = DEFAULT_RETENTION_COUNT;

    /**
     * Unbinds current flashScope from session. Rolls request's flashScope to
     * the next scope. Binds request's flashScope, if not empty, to the session.
     *
     */
    public void afterCompletion(HttpServletRequest request,
            HttpServletResponse response, Object handler, Exception ex) {
        if (request.getSession(false) != null) {
            request.getSession().removeAttribute(this.sessionAttributeName);
        }
        Object requestAttribute = request.getAttribute(this.attributeName);
        if (requestAttribute instanceof MultiScopeModelMap) {
            MultiScopeModelMap attributes = (MultiScopeModelMap) requestAttribute;
            if (!attributes.isEmpty()) {
                attributes.next();
                if (!attributes.isEmpty()) {
                    request.getSession(true).setAttribute(
                            this.sessionAttributeName, attributes);
                }
            }
        }
    }

    /**
     * merge modelAndView.model['flashScope'] to current flashScope
     */
    public void postHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler,
            ModelAndView modelAndView) {
        if (modelAndView != null) {
            Map<String, Object> modelFlashScopeMap = null;
            for (Iterator<Entry<String, Object>> iterator = ((Map<String, Object>) modelAndView
                    .getModel()).entrySet().iterator(); iterator.hasNext();) {
                Entry<String, Object> entry = iterator.next();
                if (this.attributeName.equals(entry.getKey())
                        && entry.getValue() instanceof Map) {
                    if (modelFlashScopeMap == null) {
                        modelFlashScopeMap = (Map) entry.getValue();
                    } else {
                        modelFlashScopeMap.putAll((Map) entry.getValue());
                    }
                    iterator.remove();
                } else if (entry.getKey().startsWith(this.attributeName + ".")) {
                    String key = entry.getKey().substring(
                            this.attributeName.length() + 1);
                    if (modelFlashScopeMap == null) {
                        modelFlashScopeMap = new HashMap<String, Object>();
                    }
                    modelFlashScopeMap.put(key, entry.getValue());
                    iterator.remove();
                }
            }
            if (modelFlashScopeMap != null) {
                MultiScopeModelMap flashScopeMap;
                if (request.getAttribute(this.attributeName) instanceof MultiScopeModelMap) {
                    flashScopeMap = (MultiScopeModelMap) request
                            .getAttribute(this.attributeName);
                } else {
                    flashScopeMap = new MultiScopeModelMap(this.retentionCount);
                }
                flashScopeMap.putAll(modelFlashScopeMap);
                request.setAttribute(this.attributeName, flashScopeMap);
            }
        }
    }

    /**
     * binds session flashScope to current session, if not empty. Otherwise
     * cleans up empty flashScope
     */
    public boolean preHandle(HttpServletRequest request,
            HttpServletResponse response, Object handler) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object sessionAttribute = session
                    .getAttribute(this.sessionAttributeName);
            if (sessionAttribute instanceof MultiScopeModelMap) {
                MultiScopeModelMap flashScope = (MultiScopeModelMap) sessionAttribute;
                if (flashScope.isEmpty()) {
                    session.removeAttribute(this.sessionAttributeName);
                } else {
                    request.setAttribute(this.attributeName, flashScope);
                }
            }
        }
        return true;
    }

}

class MultiScopeModelMap extends CompositeMap implements Serializable,
        MapMutator {
    public MultiScopeModelMap(int num) {
        super();
        setMutator(this);
        for (int i = 0; i < num; ++i) {
            addComposited(new HashMap());
        }
    }

    /** Shadows composite map. */
    private final Deque<Map> maps = new LinkedList<Map>();

    @Override
    public synchronized void addComposited(Map map)
            throws IllegalArgumentException {
        super.addComposited(map);
        this.maps.addLast(map);
    }

    @Override
    public synchronized Map removeComposited(Map map) {
        Map removed = super.removeComposited(map);
        this.maps.remove(map);
        return removed;
    }

    /**
     * Starts a new scope. All items added in the session before the previous
     * session are removed. All items added in the previous scope are still
     * retrievable and removable.
     */
    public void next() {
        removeComposited(this.maps.getFirst());
        addComposited(new HashMap());
    }

    public Object put(CompositeMap map, Map[] composited, Object key,
            Object value) {
        if (composited.length < 1) {
            throw new UnsupportedOperationException(
                    "No composites to add elements to");
        }
        Object result = map.get(key);
        if (result != null) {
            map.remove(key);
        }
        composited[composited.length - 1].put(key, value);
        return result;
    }

    public void putAll(CompositeMap map, Map[] composited, Map mapToAdd) {
        for (Entry entry : (Set<Entry>) mapToAdd.entrySet()) {
            put(map, composited, entry.getKey(), entry.getValue());
        }
    }

    public void resolveCollision(CompositeMap composite, Map existing,
            Map added, Collection intersect) {
        existing.keySet().removeAll(intersect);
    }

    @Override
    public String toString() {
        return new HashMap(this).toString();
    }

}