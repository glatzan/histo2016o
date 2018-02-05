package org.histo.adaptors.printer;

import org.apache.log4j.Logger;
import org.histo.model.interfaces.HasID;

import com.google.gson.annotations.Expose;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractPrinter implements HasID {

	protected static Logger logger = Logger.getLogger("org.histo");

	@Expose
	protected long id;

	@Expose
	protected String address;

	@Expose
	protected String port;

	@Expose
	protected String name;

	@Expose
	protected String description;

	@Expose
	protected String location;

	@Expose
	protected String commentary;

	@Expose
	protected String userName;

	@Expose
	protected String password;

	@Expose
	protected String deviceUri;

	public AbstractPrinter() {
	}

	public abstract boolean printTestPage();
}
