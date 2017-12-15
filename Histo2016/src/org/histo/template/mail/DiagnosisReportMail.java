package org.histo.template.mail;

import java.io.StringWriter;
import java.util.Properties;

import javax.persistence.Transient;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.tools.generic.DateTool;
import org.histo.model.AssociatedContact;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.template.MailTemplate;

import lombok.Getter;
import lombok.Setter;

//@Entity
@Getter
@Setter
public class DiagnosisReportMail extends MailTemplate {

	@Transient
	private Patient patient;

	@Transient
	private Task task;

	@Transient
	private AssociatedContact contact;

	public void prepareTemplate(Patient patient, Task task, AssociatedContact contact) {
		prepareTemplate();

		this.patient = patient;
		this.task = task;
		this.contact = contact;

	}

	public void fillTemplate() {
		VelocityEngine ve = new VelocityEngine();
		Properties props = new Properties();
		props.put("runtime.log.logsystem.class", "org.apache.velocity.runtime.log.SimpleLog4JLogSystem");
		props.put("runtime.log.logsystem.log4j.category", "velocity");
		props.put("runtime.log.logsystem.log4j.logger", "velocity");

		ve.init(props);
		
		/* create a context and add data */
		VelocityContext context = new VelocityContext();

		
		context.put("date", new DateTool());
		
		if (patient != null)
			context.put("patient", patient);
		
		if (task != null)
			context.put("task", task);
		
		if (contact != null)
			context.put("contact", contact);
	

		/* now render the template into a StringWriter */
		StringWriter writer = new StringWriter();

		Velocity.evaluate(context, writer, "mystring", getSubject());
		setSubject(writer.toString());

		writer.getBuffer().setLength(0);

		Velocity.evaluate(context, writer, "mystring", getBody());
		setBody(writer.toString());
	}
}
