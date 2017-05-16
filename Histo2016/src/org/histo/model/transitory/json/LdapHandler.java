package org.histo.model.transitory.json;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.interfaces.GsonAble;

import com.google.gson.annotations.Expose;

public class LdapHandler implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	@Expose
	private String host;
	@Expose
	private int port;
	@Expose
	private String suffix;
	@Expose
	private String base;

	private DirContext connection;

	public Physician getPhyscican(String userName) throws NamingException, SocketException, IOException {
		ArrayList<Physician> physicians = getListOfPhysicians("(uid=" + userName + ")");
		if (physicians.size() == 1)
			return physicians.get(0);
		return null;
	}

	public ArrayList<Physician> getListOfPhysicians(String filter) throws NamingException {

		ArrayList<Physician> physicians = new ArrayList<Physician>();

		SearchControls constraints = new SearchControls();
		constraints.setSearchScope(SearchControls.SUBTREE_SCOPE);

		logger.debug("Searching for Filter " + filter);

		NamingEnumeration<?> results = connection.search(base, filter, constraints);

		// temp id
		int i = 0;
		while (results != null && results.hasMore()) {
			SearchResult result = (SearchResult) results.next();
			Attributes attrs = result.getAttributes();

			if (logger.isTraceEnabled())
				printAllAttributes(attrs);

			if (attrs != null) {
				// check if uid is not a number, only people with a name as
				// uid are active
				Attribute attr = attrs.get("uid");
				printAllAttributes(attrs);
				if (attr != null && attr.size() == 1 && !StringUtils.isNumeric(attr.get().toString())) {
					Physician newPhysician = new Physician(new Person());
					newPhysician.setUid(attr.get().toString());
					newPhysician.setClinicEmployee(true);
					newPhysician.setDnObjectName(result.getName());
					newPhysician.copyIntoObject(attrs);
					newPhysician.setId(i);
					physicians.add(newPhysician);

				}

			}
			i++;
		}

		return physicians;
	}

	public boolean checkPassword(String userName, String password)
			throws SocketException, IOException, NamingException {
		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.SECURITY_PRINCIPAL, userName);
		env.put(Context.SECURITY_CREDENTIALS, password);
		openConnection(env);
		return true;
	}

	public void openConnection() throws SocketException, IOException, NamingException {
		openConnection(new Hashtable<String, String>());
	}

	public void openConnection(Hashtable<String, String> env) throws SocketException, IOException, NamingException {
		if (connection == null) {
			env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
			env.put(Context.PROVIDER_URL, "ldap://" + host + ":" + port + "/" + suffix);

			logger.debug("Open connection to ldap: " + env.get(Context.PROVIDER_URL));

			connection = new InitialDirContext(env);
		}
	}

	public void closeConnection() throws IOException, NamingException {
		connection.close();
		connection = null;
	}

	/********************************************************
	 * static
	 ********************************************************/

	public static final void printAllAttributes(Attributes attrs) {
		for (NamingEnumeration<?> ae = attrs.getAll(); ae.hasMoreElements();) {
			Attribute attr;
			try {
				attr = (Attribute) ae.next();
				String attrId = attr.getID();
				for (Enumeration<?> vals = attr.getAll(); vals.hasMoreElements(); logger
						.debug(attrId + ": " + vals.nextElement()))
					;
			} catch (NamingException e) {
				logger.error("Error while listing physician data ", e);
			}

		}
	}

	/********************************************************
	 * static
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getSuffix() {
		return suffix;
	}

	public String getBase() {
		return base;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public void setBase(String base) {
		this.base = base;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
