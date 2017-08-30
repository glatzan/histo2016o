package org.histo.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class TemplateDeserializer<T> implements JsonDeserializer<List<T>> {

	public List<T> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		List<T> list = new ArrayList<T>();
		JsonArray ja = json.getAsJsonArray();

		for (JsonElement je : ja) {

			String type = je.getAsJsonObject().get("templateName").getAsString();
			try {
				Class c = Class.forName(type);
				list.add(context.deserialize(je, c));
			} catch (ClassNotFoundException e) {
				throw new RuntimeException("Unknow class: " + type);
			}

		}

		return list;
	}
}
