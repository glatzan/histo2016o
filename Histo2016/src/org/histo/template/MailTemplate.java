package org.histo.template;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Transient;

import org.histo.action.handler.GlobalSettings;
import org.histo.model.PDFContainer;
import org.histo.util.StreamUtils;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter
public class MailTemplate extends Template {

	@Transient
	private PDFContainer attachment;

	@Transient
	private String subject;

	@Transient
	private String body;

	public static <T extends MailTemplate> T getDefaultTemplate(Class<T> mailType) {
		return getTemplates(mailType).stream().filter(p -> p.isDefaultOfType())
				.collect(StreamUtils.singletonCollector());
	}

	public static <T extends MailTemplate> List<T> getTemplates(Class<T> mailType) {

		// TODO move to Database
		Type type = new TypeToken<List<MailTemplate>>() {
		}.getType();

		GsonBuilder gb = new GsonBuilder();
		gb.registerTypeAdapter(type, new TemplateDeserializer<MailTemplate>());

		Gson gson = gb.create();

		ArrayList<MailTemplate> jsonArray = gson
				.fromJson(FileHandlerUtil.getContentOfFile(GlobalSettings.MAIL_TEMPLATES), type);

		List<T> result = new ArrayList<T>();

		for (MailTemplate mailTemplate : jsonArray) {
			if (mailTemplate.getTemplateName().equals(mailType.getName())) {
				result.add((T) mailTemplate);
			}
		}

		return result;
	}

	@Override
	// TODO should be saved in database
	public void prepareTemplate() {
		String file = FileHandlerUtil.getContentOfFile(getContent());

		String[] arr = file.split("\r\n", 2);

		if (arr.length != 2)
			return;

		subject = arr[0].replaceAll("Subject: ", "");
		body = arr[1].replaceAll("Body: ", "");

	}

	public void fillTemplate() {

	}
}