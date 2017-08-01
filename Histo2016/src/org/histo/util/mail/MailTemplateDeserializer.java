package org.histo.util.mail;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.histo.model.template.MailTemplate;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

public class MailTemplateDeserializer implements JsonDeserializer<List<MailTemplate>> {

	public List<MailTemplate> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {

		List<MailTemplate> list = new ArrayList<MailTemplate>();
		JsonArray ja = json.getAsJsonArray();

		for (JsonElement je : ja) {

			String type = je.getAsJsonObject().get("type").getAsString();

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
