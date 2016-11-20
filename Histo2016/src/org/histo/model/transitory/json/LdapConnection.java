package org.histo.model.transitory.json;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

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
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.histo.config.HistoSettings;
import org.histo.model.Physician;
import org.histo.model.interfaces.GsonAble;
import org.histo.util.HistoUtil;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.reflect.TypeToken;

public class LdapConnection implements GsonAble {

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

		NamingEnumeration<?> results = connection.search(base, filter, constraints);

		// temp id
		int i = 0;
		while (results != null && results.hasMore()) {
			SearchResult result = (SearchResult) results.next();
			Attributes attrs = result.getAttributes();
			if (attrs != null) {
				// check if uid is not a number, only people with a name as
				// uid are active
				Attribute attr = attrs.get("uid");
				if (attr != null && attr.size() == 1 && !StringUtils.isNumeric(attr.get().toString())) {
					Physician newPhysician = new Physician(i++);
					newPhysician.setClinicEmployee(true);
					newPhysician.setDnObjectName(result.getName());
					newPhysician.copyIntoObject(attrs);

					physicians.add(newPhysician);
				}
			}
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

			connection = new InitialDirContext(env);
		}
	}

	public void closeConnection() throws IOException, NamingException {
		connection.close();
		connection = null;
	}

	public static final LdapConnection factroy(String jsonFile) {
		Gson gson = new Gson();
		LdapConnection result = gson.fromJson(HistoUtil.loadTextFile(jsonFile), LdapConnection.class);

		return result;
	}

	public static final void printAllAttributes(Attributes attrs) {
		for (NamingEnumeration<?> ae = attrs.getAll(); ae.hasMoreElements();) {
			Attribute attr;
			try {
				attr = (Attribute) ae.next();
				String attrId = attr.getID();
				for (Enumeration<?> vals = attr.getAll(); vals.hasMoreElements(); System.out
						.println(attrId + ": " + vals.nextElement()))
					;
			} catch (NamingException e) {
				e.printStackTrace();
			}

		}
	}

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

}
