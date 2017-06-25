package org.histo.action;


import org.histo.config.exception.CustomNotUniqueReqest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Configuration;

@Configurable
public class Test {

	@Autowired
	private MainHandlerAction mainHandlerAction;

	public void test() {
		System.out.println(mainHandlerAction);
		throw new CustomNotUniqueReqest();
	}
}
