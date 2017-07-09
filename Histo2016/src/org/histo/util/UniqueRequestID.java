package org.histo.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.apache.log4j.Logger;
import org.histo.config.exception.CustomNotUniqueReqest;

import lombok.Getter;
import lombok.Setter;

public class UniqueRequestID {

	protected static Logger logger = Logger.getLogger("org.histo");

	private final static String SUBMITTEDID = "submittedRequestID";

	private SecureRandom random = new SecureRandom();

	@Getter
	@Setter
	private String uniqueRequestID;

	@Getter
	@Setter
	private String submittedRequestID;

	@Getter
	@Setter
	private boolean enabled;
	
	public UniqueRequestID() {
		setUniqueRequestID("");
	}

	public void nextUniqueRequestID() {
		setUniqueRequestID(new BigInteger(130, random).toString(32));
		logger.debug("New Unique ID generated");
	}

	public void checkUniqueRequestID(boolean toClose){
		// disables uniqueCheck
		checkUniqueRequestID(toClose, false);
	}
	
	public void checkUniqueRequestID(boolean toClose, boolean enabled){

		FacesContext fc = FacesContext.getCurrentInstance();
		Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
		String submittedRequestID = params.get(SUBMITTEDID);

		if (submittedRequestID == null || submittedRequestID.isEmpty()){
			logger.debug("No ID submitted");
			throw new CustomNotUniqueReqest(toClose);
		}

		if (uniqueRequestID.isEmpty() || !uniqueRequestID.equals(submittedRequestID)){
			logger.debug("ID does not equal");
			throw new CustomNotUniqueReqest(toClose);
		}

		logger.debug("Unique ID matched");
		
		setUniqueRequestID("");
		setEnabled(enabled);
	}
}
