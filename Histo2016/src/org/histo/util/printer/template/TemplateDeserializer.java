package org.histo.util.printer.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

// https://stackoverflow.com/questions/19588020/gson-serialize-a-list-of-polymorphic-objects
public class TemplateDeserializer implements JsonDeserializer<List<AbstractTemplate>> {

    private static Map<String, Class> map = new TreeMap<String, Class>();

    static {
        map.put("AbstractTemplate", AbstractTemplate.class);
    }

    public List<AbstractTemplate> deserialize(JsonElement json, Type typeOfT,
            JsonDeserializationContext context) throws JsonParseException {

        List<AbstractTemplate> list = new ArrayList<AbstractTemplate>();
		JsonArray ja = json.getAsJsonArray();

        for (JsonElement je : ja) {

            String type = je.getAsJsonObject().get("className").getAsString();
            Class c = map.get(type);
            if (c == null)
                throw new RuntimeException("Unknow class: " + type);
            list.add(context.deserialize(je, c));
        }

        return list;

    }

}
