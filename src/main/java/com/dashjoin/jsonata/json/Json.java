package com.dashjoin.jsonata.json;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.dashjoin.jsonata.JException;
import com.dashjoin.jsonata.Utils;

/**
 * Vanilla JSON parser
 * 
 * Uses classes JsonParser + JsonHandler from:
 * https://github.com/ralfstx/minimal-json
 */
public class Json {

    public static class _JsonHandler extends JsonHandler<List<?>, Map<?, ?>> {
        protected Object value;

        @Override
        public List<?> startArray() {
            return new ArrayList<>();
        }

        @Override
        public Map<?, ?> startObject() {
            return new LinkedHashMap<>();
        }

        @Override
        public void endNull() {
            value = null;
        }

        @Override
        public void endBoolean(boolean bool) {
            value = bool;
        }

        @Override
        public void endString(String string) {
            value = string;
        }

        @Override
        public void endNumber(String string) {
            double d = Double.valueOf(string);
            try {
                value = Utils.convertNumber(d);
            } catch (JException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        @Override
        public void endArray(List array) {
            value = array;
        }

        @Override
        public void endObject(Map object) {
            value = object;
        }

        @Override
        public void endArrayValue(List array) {
            array.add(value);
        }

        @Override
        public void endObjectValue(Map object, String name) {
            object.put(name, value);
        }

        public Object getValue() {
            return value;
        }

    }

    /**
     * Parses the given JSON string
     * 
     * @param json
     * @return Parsed object
     */
    public static Object parseJson(String json) {
        _JsonHandler handler = new _JsonHandler();
        JsonParser jp = new JsonParser(handler);
        jp.parse(json);
        return handler.getValue();
    }

    /**
     * Parses the given JSON
     * 
     * @param json
     * @return Parsed object
     * @throws IOException
     */
    public static Object parseJson(Reader json) throws IOException {
        _JsonHandler handler = new _JsonHandler();
        JsonParser jp = new JsonParser(handler);
        jp.parse(json, 65536);
        return handler.getValue();
    }

    public static void main(String[] args) throws Throwable {

        _JsonHandler handler = new _JsonHandler();

        JsonParser jp = new JsonParser(handler);

        jp.parse("{\"a\":false}");

        System.out.println(handler.getValue());
    }
}
