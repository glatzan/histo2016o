package org.histo.util;

import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

public class VelocityNoOutputLogger implements LogChute {
	public void init(RuntimeServices arg0) throws Exception {
	}

	public boolean isLevelEnabled(int arg0) {
		return false;
	}

	public void log(int arg0, String arg1, Throwable arg2) {
	}

	public void log(int arg0, String arg1) {
	}
}