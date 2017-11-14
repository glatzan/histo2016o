package org.histo.model.transitory.settings;

import org.histo.model.interfaces.GsonAble;

public class PrinterSettings implements GsonAble {

	private String cupsHost;

	private int cupsPost;

	private String testPage;

	// ************************ Getter/Setter ************************
	public String getCupsHost() {
		return cupsHost;
	}

	public void setCupsHost(String cupsHost) {
		this.cupsHost = cupsHost;
	}

	public int getCupsPost() {
		return cupsPost;
	}

	public void setCupsPost(int cupsPost) {
		this.cupsPost = cupsPost;
	}

	public String getTestPage() {
		return testPage;
	}

	public void setTestPage(String testPage) {
		this.testPage = testPage;
	}

}
