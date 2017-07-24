package org.histo.experimental;

import org.histo.dao.GenericDAO;

public class NotificationHandler implements Runnable {


	private GenericDAO genericDAO;
	
	public NotificationHandler(GenericDAO genericDAO) {
		this.genericDAO = genericDAO;
	}

	String name;

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		while (true) {
//			
//			Slide slide = new Slide();
//			slide.setSlideID(String.valueOf(contactHandlerAction.getTest()));
//			genericDAO.save(slide);
//			
//			contactHandlerAction.setTest(contactHandlerAction.getTest() + 1);
//			contactHandlerAction.updateTest();
//			System.out.println(name + " is running " + contactHandlerAction.getTest());
//
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}
//
//			System.out.println(name + " is running");
		}
	}

}
