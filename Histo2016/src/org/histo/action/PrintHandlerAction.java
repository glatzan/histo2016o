package org.histo.action;

import java.io.IOException;
import java.io.Serializable;

import org.histo.util.ResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;

import java.net.URL;
import java.net.URLClassLoader;

@Controller
@Scope("session")
public class PrintHandlerAction implements Serializable {
	
	private static final long serialVersionUID = -4471922130006206831L;
	
	@Autowired
	private ResourceBundle resourceBundle;

	public void print() {
//		PdfReader pdfTemplate;
//		try {
//			pdfTemplate = new PdfReader("Q:\\AUG-T-HISTO\\Formulare\\ergebniss20.pdf");
//
//			ByteArrayOutputStream out = new ByteArrayOutputStream();
//			PdfStamper stamper = new PdfStamper(pdfTemplate, out);
//
//			stamper.setFormFlattening(true);
//
//			stamper.getAcroFields().setField("date", "Daniel Reuter");
//			stamper.close();
//			pdfTemplate.close();
//
//			FileOutputStream fos = new FileOutputStream("Q:\\AUG-T-HISTO\\Formulare\\ergebnis-test.pdf");
//
//			fos.write(out.toByteArray());
//
//			fos.close();
//
//		} catch (IOException | DocumentException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		System.out.println(resourceBundle.get("log.diagnosis.changed"));
		
		ClassLoader cl = ClassLoader.getSystemClassLoader();

        URL[] urls = ((URLClassLoader)cl).getURLs();

        for(URL url: urls){
        	System.out.println(url.getFile());
        }

        
		ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver(new DefaultResourceLoader());
		Resource[] resources;
		try {
			resources = patternResolver.getResources("classpath*:messages/messages*");
			for(Resource resource : resources) {
				   System.out.println(resource.getDescription());
				}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
