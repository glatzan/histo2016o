package org.histo.tut;

import org.histo.action.MainHandlerAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommonController {
 
	@Autowired
	private MainHandlerAction mainHandlerAction;
	
    @RequestMapping(value = "/test")
    public String getLogin() {
                System.out.println("Hello Welcome to API " + mainHandlerAction);
        return "Hello Welcome to API";
    }
 
    @RequestMapping(value = "/dologout", method = RequestMethod.GET)
    public String getLogout() {
        return "Exit to API";
    }
}
