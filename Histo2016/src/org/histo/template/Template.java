package org.histo.template;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.log4j.Logger;
import org.apache.velocity.app.Velocity;
import org.histo.model.interfaces.HasID;
import org.histo.util.VelocityNoOutputLogger;

import lombok.Getter;
import lombok.Setter;

//@Entity
//@Audited
//@SelectBeforeUpdate(true)
//@DynamicUpdate(true)
//@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
//@SequenceGenerator(name = "template_sequencegenerator", sequenceName = "template_sequence")
@Getter
@Setter
public abstract class Template implements HasID, Cloneable {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Id
	@GeneratedValue(generator = "template_sequencegenerator")
	@Column(unique = true, nullable = false)
	protected long id;

	@Column(columnDefinition = "VARCHAR")
	protected String name;

	@Column(columnDefinition = "VARCHAR")
	protected String content;

	@Column(columnDefinition = "VARCHAR")
	protected String content2;

	/**
	 * Type of the template
	 */
	@Column(columnDefinition = "VARCHAR")
	protected String type;

	/**
	 * If True the default of it's type
	 */
	@Column
	protected boolean defaultOfType;

	/**
	 * If true the generated content should not be saved in the database
	 */
	@Column
	protected boolean transientContent;

	/**
	 * Name of the template class
	 */
	@Column
	protected String templateName;

	protected String attributes;

	public void prepareTemplate() {

	}

	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			return null;
		}
	}
	
	public static final void initVelocity() {
		Velocity.setProperty(Velocity.RUNTIME_LOG_LOGSYSTEM, new VelocityNoOutputLogger());
		Velocity.init();
	}
}
