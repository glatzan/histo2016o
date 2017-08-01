package org.histo.template;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.histo.model.PDFContainer;
import org.histo.util.interfaces.FileHandlerUtil;

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
	
	
	@Override
	//TODO should be saved in database
	public void prepareTemplate() {
		String file =  FileHandlerUtil.getContentOfFile(getContent());
		
		String[] arr = file.split("\r\n", 2);
		
		if(arr.length != 2)
			return;
		
		subject = arr[0].replaceAll("Subject: ", "");
		body = arr[1].replaceAll("Body: ", "");
		
		
	}
	
	public void fillTemplate() {
		
	}
	
	@Transient
	public void setMailType(Class<? extends MailTemplate> type) {
		this.type = type.toString();
	}
	
	@Transient
	public Class<? extends MailTemplate> getMailType() {
		try {
			return (Class<? extends MailTemplate>) Class.forName(this.type);
		} catch (ClassNotFoundException e) {
			return MailTemplate.class;
		}
	}

}
