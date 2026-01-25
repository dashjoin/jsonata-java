package com.dashjoin.jsonata;

import com.dashjoin.jsonata.json.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class JsonUtils {

    static Charset utf8Charset = Charset.forName("UTF-8");

    static Object toJson(String jsonStr) throws JsonMappingException, JsonProcessingException {
        return Json.parseJson(jsonStr);
    }

    static Object readJson(String name) throws StreamReadException, DatabindException, IOException {
        return Json.parseJson(new FileReader(name, utf8Charset));
    }

    static Object readJsonFromResources(String name) throws IOException {
        InputStream resourceAsStream = JsonUtils.class.getClassLoader().getResourceAsStream(name);
        if (resourceAsStream == null) {
            throw new RuntimeException("Resource not found: " + name);
        }
        return Json.parseJson(new InputStreamReader(resourceAsStream, utf8Charset));
    }
}
