package com.cyberark.server;

/**
 * @author bnasslahsen
 */

import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.serverSide.crypt.RSACipher;

import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

public class PluginPropertiesUtil {

	private PluginPropertiesUtil() {
	}

	public static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean) {
		bindPropertiesFromRequest(request, bean, false);
	}

	public static void bindPropertiesFromRequest(HttpServletRequest request, BasePropertiesBean bean, boolean includeEmptyValues) {
		bean.clearProperties();
		Iterator var3 = request.getParameterMap().keySet().iterator();

		while(var3.hasNext()) {
			Object o = var3.next();
			String paramName = (String)o;
			if (paramName.startsWith("prop:")) {
				if (paramName.startsWith("prop:encrypted:")) {
					setEncryptedProperty(paramName, request, bean, includeEmptyValues);
				} else {
					setStringProperty(paramName, request, bean, includeEmptyValues);
				}
			}
		}

	}

	private static void setStringProperty(String paramName, HttpServletRequest request, BasePropertiesBean bean, boolean includeEmptyValues) {
		String propName = paramName.substring("prop:".length());
		String propertyValue = request.getParameter(paramName).trim();
		if (includeEmptyValues || propertyValue.length() > 0) {
			bean.setProperty(propName, toUnixLineFeeds(propertyValue));
		}

	}

	private static void setEncryptedProperty(String paramName, HttpServletRequest request, BasePropertiesBean bean, boolean includeEmptyValues) {
		String propName = paramName.substring("prop:encrypted:".length());
		String propertyValue = RSACipher.decryptWebRequestData(request.getParameter(paramName));
		if (propertyValue != null && (includeEmptyValues || propertyValue.length() > 0)) {
			bean.setProperty(propName, toUnixLineFeeds(propertyValue));
		}

	}

	private static String toUnixLineFeeds(String str) {
		return str.replace("\r", "");
	}
}
