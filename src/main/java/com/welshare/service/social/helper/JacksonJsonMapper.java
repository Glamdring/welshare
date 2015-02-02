package com.welshare.service.social.helper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import com.restfb.JsonMapper;
import com.restfb.exception.FacebookJsonMappingException;

/**
 * Attempt to provide a jackson implementation. Alas, the json mapper relies
 * on specific restfb annotations so not easily possible.
 *
 * @author Bozhidar Bozhanov
 * @deprecated not working. Use DefaultJsonMapper
 */
@Deprecated
public class JacksonJsonMapper implements JsonMapper {

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public <T> T toJavaObject(String jsonString, Class<T> targetClass) {
        try {
            return mapper.readValue(jsonString, targetClass);
        } catch (IOException e) {
            throw new FacebookJsonMappingException("Problem creating java object from json", e);
        }
    }

    @Override
    public <T> List<T> toJavaList(String json, Class<T> elementClass) {
        try {
            JavaType type = TypeFactory.collectionType(ArrayList.class, elementClass);
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new FacebookJsonMappingException("Problem creating java object from json", e);
        }
    }

    @Override
    public String toJson(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new FacebookJsonMappingException("Problem creating java object from json", e);
        }
    }

    @Override
    public String toJson(Object object, boolean ignoreNullValuedProperties) {
        // TODO Auto-generated method stub
        return null;
    }

}
