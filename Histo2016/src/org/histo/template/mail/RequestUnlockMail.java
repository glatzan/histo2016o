package org.histo.template.mail;

import java.io.StringWriter;

import javax.persistence.Transient;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.histo.model.user.HistoUser;
import org.histo.template.MailTemplate;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter

// HashMap<String, String> subject = new HashMap<String, String>();
// subject.put("%name%", currentUser.getPhysician().getPerson().getFullName());
//
// HashMap<String, String> content = new HashMap<String, String>();
// content.put("%name%", currentUser.getPhysician().getPerson().getFullName());
// content.put("%username%", currentUser.getUsername());
// content.put("%i%", currentUser.getPhysician().getClinicRole());

public class RequestUnlockMail extends MailTemplate {

	@Transient
	private HistoUser histoUser;
	
	public void prepareTemplate(HistoUser histoUser) {
		prepareTemplate();
		this.histoUser = histoUser;
	}
	
	public void fillTemplate() {
		initVelocity();
		
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