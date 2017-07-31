package org.histo.settings;

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
import org.histo.dao.OrganizationDAO;
import org.histo.model.Contact;
import org.histo.model.Organization;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.interfaces.GsonAble;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.google.gson.annotations.Expose;

@Configurable
public class LdapHandler implements GsonAble {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private OrganizationDAO organizationDAO;

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

			// if (logger.isTraceEnabled())
			// printAllAttributes(attrs);

			if (attrs != null) {
				// check if uid is not a number, only people with a name as
				// uid are active
				Attribute attr = attrs.get("uid");
//				printAllAttributes(attrs);
				if (attr != null && attr.size() == 1 && !StringUtils.isNumeric(attr.get().toString())) {
					Physician newPhysician = new Physician(new Person(new Contact()));
					newPhysician.setUid(attr.get().toString());
					newPhysician.setClinicEmployee(true);
					newPhysician.setDnObjectName(result.getName());
					newPhysician.setId(i);
					initPhysicianFromLdapAttributes(newPhysician, attrs);
					physicians.add(newPhysician);

				}

			}
			i++;
		}

		return physicians;
	}

	/**
	 * Copies data from ldap into this physician object. cn: Dr. Michael Reich
	 * ou: Klinik f�r Augenheilkunde givenName: Andreas mail:
	 * andreas.glatz@uniklinik-freiburg.de sn: Glatz title: Arzt
	 * telephonenumber: +49 761 270 40010 pager: 12-4027
	 * 
	 * @param attrs
	 */
	public void initPhysicianFromLdapAttributes(Physician physician, Attributes attrs) {

//		logger.debug("Upadting physician data for " + physician.getUid() + " from ldap");

		try {
			// name surname title
			Attribute attr = attrs.get("personalTitle");

			if (attr != null && attr.size() == 1) {
				physician.getPerson().setTitle(attr.get().toString());
			}

			// uid
			attr = attrs.get("uid");
			if (attr != null && attr.size() == 1) {
				physician.setUid(attr.get().toString());
			}

			// name
			attr = attrs.get("sn");
			if (attr != null && attr.size() == 1) {
				physician.getPerson().setLastName(attr.get().toString());
			}

			attr = attrs.get("employeeNumber");
			if (attr != null && attr.size() == 1) {
				physician.setEmployeeNumber(attr.get().toString());
			}

			attr = attrs.get("givenName");
			if (attr != null && attr.size() == 1) {
				physician.getPerson().setFirstName(attr.get().toString());
			}

			attr = attrs.get("mail");
			if (attr != null && attr.size() == 1) {
				physician.getPerson().getContact().setEmail(attr.get().toString());
			}

			attr = attrs.get("telephonenumber");
			if (attr != null && attr.size() == 1) {
				physician.getPerson().getContact().setPhone(attr.get().toString());
			}

			attr = attrs.get("pager");
			if (attr != null && attr.size() == 1) {
				physician.getPerson().getContact().setPager(attr.get().toString());
			}

			// role in clinic
			attr = attrs.get("title");
			if (attr != null && attr.size() == 1) {
				physician.setClinicRole(attr.get().toString());
			}

			// department
			attr = attrs.get("ou");
			if (attr != null && attr.size() == 1) {
				Organization org = null;
				try {
					logger.trace("Loading organization " + attr.get().toString() );
					org = organizationDAO.getOrganizationByName(attr.get().toString());

				} catch (IllegalStateException e) {
					logger.trace("Organiation not found");
					org = new Organization(attr.get().toString(), new Contact());
					org.setIntern(true);
				}

				if (physician.getPerson().getOrganizsations() == null)
					physician.getPerson().setOrganizsations(new ArrayList<Organization>());

				physician.getPerson().getOrganizsations().add(org);
			}

			// sex
			attr = attrs.get("uklfrPersonType");
			if (attr != null && attr.size() == 1) {
				try {
					int intSEX = Integer.parseInt(attr.get().toString());

					if (intSEX == 1) // male
						physician.getPerson().setGender(Person.Gender.MALE);
					else if (intSEX > 1) // female
						physician.getPerson().setGender(Person.Gender.FEMALE);
					else // unknow
						physician.getPerson().setGender(Person.Gender.UNKNOWN);
				} catch (NumberFormatException e) {
					physician.getPerson().setGender(Person.Gender.UNKNOWN);
				}
			}

		} catch (NamingException e) {
			logger.error("Error while updating physician data for " + physician.getUid() + " from ldap", e);
		}
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
