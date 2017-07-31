package org.histo.util.mail.template;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.histo.model.PDFContainer;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AbstractMail {

	protected int id;
	
	protected String subject;

	protected String content;

	protected PDFContainer attachment;

	public String fillMail(HashMap<String, String> fillIn) {
		VelocityEngine ve = new VelocityEngine();
		ve.init();
		/* create a context and add data */
		VelocityContext context = new VelocityContext();

		for (Map.Entry<String, String> entry : fillIn.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			context.put(key, value);
		}

		/* now render the template into a StringWriter */
		StringWriter writer = new StringWriter();

		Velocity.evaluate(context, writer, "mystring", content);
		/* show the World */
		return writer.toString();
	}
}
