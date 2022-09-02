package com.cyberark.server;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cyberark.common.ConjurApi;
import com.cyberark.common.ConjurConfig;
import com.cyberark.common.ConjurConnectionParameters;
import com.cyberark.common.exceptions.ConjurApiAuthenticateException;
import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import freemarker.template.EmptyMap;
import jetbrains.buildServer.controllers.ActionErrors;
import jetbrains.buildServer.controllers.BaseFormXmlController;
import jetbrains.buildServer.controllers.BasePropertiesBean;
import jetbrains.buildServer.controllers.PublicKeyUtil;
import jetbrains.buildServer.controllers.XmlResponseUtil;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import org.springframework.web.servlet.ModelAndView;

/**
 * @author bnasslahsen
 */
public class ConjurTestConnectionController extends BaseFormXmlController {

	private final WebControllerManager wcm;

	private final SSLTrustStoreProvider trustStoreProvider;

	protected final Log logger = LogFactory.getLog(this.getClass());

	public static final String FAILED_TEST_CONNECTION_ERR = "failedTestConnection";


	public ConjurTestConnectionController(@NotNull final SBuildServer server,
			@NotNull final WebControllerManager wcm,
			@NotNull final SSLTrustStoreProvider trustStoreProvider) {
		super(server);
		this.wcm = wcm;
		this.trustStoreProvider = trustStoreProvider;
		wcm.registerController("/conjur-test-connection.html", this);
	}

	@Override
	protected void doPost(@NotNull final HttpServletRequest request,
			@NotNull final HttpServletResponse response, @NotNull final Element xmlResponse) {

		if (PublicKeyUtil.isPublicKeyExpired(request)) {
			PublicKeyUtil.writePublicKeyExpiredError(xmlResponse);
			return;
		}
		BasePropertiesBean propertiesBean = new BasePropertiesBean(new EmptyMap());
		PluginPropertiesUtil.bindPropertiesFromRequest(request, propertiesBean);
		Map<String, String> properties = propertiesBean.getProperties();

		doTestConnection(properties, xmlResponse);
	}

	private void doTestConnection(Map<String, String> properties, Element xmlResponse) {
		ConjurPropertiesProcessor processor = new ConjurPropertiesProcessor();
		ActionErrors errors = new ActionErrors();
		processor.process(properties).forEach(invalidProperty -> errors.addError(invalidProperty));
		if (errors.hasErrors()) {
			errors.serialize(xmlResponse);
			return;
		}

		ConjurConnectionParameters settings = new ConjurConnectionParameters(properties);
		ConjurConfig config = null;
		try {
			config = new ConjurConfig(
					settings.getApplianceUrl(),
					settings.getAccount(),
					settings.getAuthnLogin(),
					settings.getApiKey(),
					null,
					settings.getCertFile()
			);
		}
		catch (MissingMandatoryParameterException e) {
			String message = String.format("ERROR: Retrieving conjur parameters. %s", e.getMessage());
			errors.addError(FAILED_TEST_CONNECTION_ERR, message);
		}

		ConjurApi client = new ConjurApi(config);
		try {
			logger.debug("Attempting to Authenticate to conjur");
			client.authenticate();
			XmlResponseUtil.writeTestResult(xmlResponse, "");
			return;
		}
		catch (ConjurApiAuthenticateException e) {
			String message = String.format(
					"ERROR: Authenticating to conjur at '%s' with account '%s' and with login '%s'. %s",
					config.url,
					config.account,
					config.username,
					e.getMessage()
			);
			errors.addError(FAILED_TEST_CONNECTION_ERR, message);
		}
		catch (Exception e) {
			String message = String.format(
					"ERROR: Generic error returned when establishing connection to conjur. %s",
					e
			);
			errors.addError(FAILED_TEST_CONNECTION_ERR, message);
		}
		if (errors.hasErrors()) {
			errors.serialize(xmlResponse);
		}
	}

	@Override
	protected ModelAndView doGet(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response) {
		return null;
	}
}