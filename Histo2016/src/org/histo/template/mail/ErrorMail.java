package org.histo.template.mail;

import java.io.StringWriter;
import java.util.Date;
import java.util.Properties;

import javax.persistence.Transient;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.histo.model.user.HistoUser;
import org.histo.template.MailTemplate;

//errorMessage = errorMessage + "\r\n\r\nAbsender: "
//		+ userHandlerAction.getCurrentUser().getPhysician().getPerson().getFullName();

//mainHandlerAction.getSettings().getMail().sendMailFromSystem(
//		mainHandlerAction.getSettings().getErrorMails(),
//		"Fehlermeldung vom "
//				+ TimeUtil.formatDate(dateOfError, DateFormat.GERMAN_DATE_TIME.getDateFormat()),
//		errorMessage);


public class ErrorMail extends MailTemplate {

	@Transient
	private HistoUser histoUser;

	private String errorContent;
	
	private Date currentDate;

	public void prepareTemplate(HistoUser histoUser, String errorContent, Date currentDate) {
		prepareTemplate();

		this.histoUser = histoUser;
		this.errorContent = errorContent;
		this.currentDate = currentDate;
	}

	public void fillTemplate() {
		initVelocity();
		
		/* create a context and add data */
		VelocityContext context = new VelocityContext();

		context.put("histoUser", histoUser);
		context.put("errorContent", errorContent);
		context.put("currentDate", currentDate);

		/* now render the template into a StringWriter */
		StringWriter writer = new StringWriter();

		Velocity.evaluate(context, writer, "mystring", getSubject());
		setSubject(writer.toString());

		writer.getBuffer().setLength(0);

		Velocity.evaluate(context, writer, "mystring", getBody());
		setBody(writer.toString());
	}
}