package org.histo.adaptors.printer;

import java.util.List;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.AbstractJsonHandler;
import org.histo.util.HistoUtil;
import org.histo.util.print.RoomContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class PrinterForRoomHandler extends AbstractJsonHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	/**
	 * Loads a printer from the room in association with the current ip
	 * 
	 * @return
	 */
	public ClinicPrinter getPrinterForRoom() {
		HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
				.getRequest();
		String ip = getRemoteAddress(request);

		if (ip != null) {
			String printerToRoomJson = requestJsonData(baseUrl.replace("$ip", ip));

			List<RoomContainer> container = RoomContainer.factory(printerToRoomJson);

			if (container.size() > 0) {
				RoomContainer firstConatiner = container.get(0);

				for (ClinicPrinter printer : globalSettings.getPrinterList()) {
					if (HistoUtil.isNotNullOrEmpty(firstConatiner.getPrinter())
							&& firstConatiner.getPrinter().equals(printer.getDeviceUri())) {
						logger.debug("Printer found for room " + ip + "; printer = " + printer.getName());
						return printer;
					}
				}
			}

		}

		return null;

	}

	/**
	 * Gets the remote address from a HttpServletRequest object. It prefers the
	 * `X-Forwarded-For` header, as this is the recommended way to do it (user may
	 * be behind one or more proxies).
	 *
	 * Taken from https://stackoverflow.com/a/38468051/778272
	 *
	 * @param request
	 *            - the request object where to get the remote address from
	 * @return a string corresponding to the IP address of the remote machine
	 */
	public static String getRemoteAddress(HttpServletRequest request) {
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		if (ipAddress != null) {
			// cares only about the first IP if there is a list
			ipAddress = ipAddress.replaceFirst(",.*", "");
		} else {
			ipAddress = request.getRemoteAddr();
		}
		return ipAddress;
	}
}
