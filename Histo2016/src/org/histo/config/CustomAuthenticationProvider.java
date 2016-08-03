package org.histo.config;

import java.util.Collection;
import java.util.Hashtable;

import javax.naming.Context;

import org.histo.dao.UserDAO;
import org.histo.model.UserAcc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserDAO userDAO;
    
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
	String userName = authentication.getName().trim();
	String password = authentication.getCredentials().toString().trim();

	String url = "ldap://ldap.forumsys.com:389";
	Hashtable env = new Hashtable();
	env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
	env.put(Context.PROVIDER_URL, url);
	env.put(Context.SECURITY_AUTHENTICATION, "simple");
	env.put(Context.SECURITY_PRINCIPAL, "uid=" + userName + ",dc=example,dc=com");
	env.put(Context.SECURITY_CREDENTIALS, password);

	try {
	   // DirContext ctx = new InitialDirContext(env);
	    System.out.println("connected");
	   // System.out.println(ctx.getEnvironment());

	    System.out.println("success");

	    UserAcc userAcc = userDAO.loadUserByName(userName);

	    System.out.println("User loaded " + userAcc);
	    if (userAcc == null) {
		throw new BadCredentialsException("More than one user found");
	    }

	    Collection<? extends GrantedAuthority> authorities = userAcc.getAuthorities();
	   // ctx.close();

	    System.out.println(userAcc + " " + password + " " + authorities);
	    return new UsernamePasswordAuthenticationToken(userAcc, password, authorities);
	}

//	} catch (AuthenticationNotSupportedException ex) {
//	    System.out.println("The authentication is not supported by the server");
//	} 
	catch (AuthenticationException ex) {
	    System.out.println("incorrect password or username");
	} 
//	catch (NamingException ex) {
//	    System.out.println("error when trying to create the context");
//	}
	throw new BadCredentialsException("Username not found.");

    }

    @Override
    public boolean supports(Class<? extends Object> authentication) {
	return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }
}