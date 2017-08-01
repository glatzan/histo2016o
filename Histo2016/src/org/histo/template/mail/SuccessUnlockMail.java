package org.histo.template.mail;

import java.io.StringWriter;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.histo.model.HistoUser;
import org.histo.template.MailTemplate;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter
public class SuccessUnlockMail extends MailTemplate {

	@Transient
	private HistoUser histoUser;
	
	public void prepareTemplate(HistoUser histoUser) {
		prepareTemplate();
		
		this.histoUser = histoUser;
	}
	
	public void fillTemplate() {
		VelocityEngine ve = new VelocityEngine();
		ve.init();
		/* create a context and add data */
		VelocityContext context = new VelocityContext();

		context.put("histoUser", histoUser);

		/* now render the template into a StringWriter */
		StringWriter writer = new StringWriter();

		Velocity.evaluate(context, writer, "mystring", getSubject());
		setSubject(writer.toString());
		
		writer.getBuffer().setLength(0);
		
		Velocity.evaluate(context, writer, "mystring", getBody());
		setBody(writer.toString());
	}
}
