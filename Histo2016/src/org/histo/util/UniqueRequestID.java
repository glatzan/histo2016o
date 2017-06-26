package org.histo.util;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomNotUniqueReqest;

import lombok.Getter;
import lombok.Setter;

public class UniqueRequestID {

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
	}

	public void checkUniqueRequestID(boolean toClose){

		FacesContext fc = FacesContext.getCurrentInstance();
		Map<String, String> params = fc.getExternalContext().getRequestParameterMap();
		String submittedRequestID = params.get(SUBMITTEDID);

		if (submittedRequestID == null || submittedRequestID.isEmpty())
			throw new CustomNotUniqueReqest(toClose);

		if (uniqueRequestID.isEmpty() || !uniqueRequestID.equals(submittedRequestID))
			throw new CustomNotUniqueReqest(toClose);

		setUniqueRequestID("");
	}
}
