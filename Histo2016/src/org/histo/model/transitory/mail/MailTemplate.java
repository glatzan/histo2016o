package org.histo.model.transitory.mail;

import java.lang.reflect.Type;

import org.histo.config.HistoSettings;
import org.histo.config.enums.MailType;
import org.histo.model.interfaces.HasID;
import org.histo.util.interfaces.FileHandlerUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MailTemplate implements HasID, FileHandlerUtil {

	private long id;

	private String subject;

	private String contentPath;

	private MailType mailType;

	public static final MailTemplate factroy(MailType mailType) {
		return factroy(HistoSettings.MAIL_TEMPLATE_JSON, mailType);
	}

	public static final MailTemplate factroy(String jsonFile, MailType mailType) {

		Type type = new TypeToken<MailTemplate[]>() {
		}.getType();

		Gson gson = new Gson();
		MailTemplate[] result = gson.fromJson(FileHandlerUtil.getContentOfFile(jsonFile), type);

		return MailTemplate.getTemplateByType(result, mailType);
	}

	public static final MailTemplate getTemplateByType(MailTemplate[] tempaltes, MailType type) {

		for (MailTemplate mailTemplate : tempaltes) {
			if (mailTemplate.getMailType().equals(type))
				return mailTemplate;
		}

		return null;
	}

	public String getContent() {
		return FileHandlerUtil.getContentOfFile(getContentPath());
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContentPath() {
		return contentPath;
	}

	public void setContentPath(String contentPath) {
		this.contentPath = contentPath;
	}

	public MailType getMailType() {
		return mailType;
	}

	public void setMailType(MailType mailType) {
		this.mailType = mailType;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
