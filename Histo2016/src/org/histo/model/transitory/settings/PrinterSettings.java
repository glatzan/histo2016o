package org.histo.model.transitory.settings;

import org.histo.model.interfaces.GsonAble;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PrinterSettings implements GsonAble {

	private String cupsHost;

	private int cupsPost;

	private String testPage;

}
