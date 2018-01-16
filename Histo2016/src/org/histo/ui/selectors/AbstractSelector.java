package org.histo.ui.selectors;

import org.apache.log4j.Logger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AbstractSelector {

	protected static Logger logger = Logger.getLogger("org.histo");

	protected boolean selected;

}
