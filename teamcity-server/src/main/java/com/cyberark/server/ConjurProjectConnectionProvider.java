package com.cyberark.server;

import com.cyberark.common.ConjurConnectionParameters;
import com.cyberark.common.ConjurSettings;
import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import jetbrains.buildServer.serverSide.PropertiesProcessor;
import jetbrains.buildServer.serverSide.oauth.OAuthProvider;
import jetbrains.buildServer.serverSide.oauth.OAuthConnectionDescriptor;

import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;


public class ConjurProjectConnectionProvider extends OAuthProvider {
	final private PluginDescriptor descriptor;

    public ConjurProjectConnectionProvider(PluginDescriptor descriptor) {
        this.descriptor = descriptor;
	}

	@Override
	@NotNull
	public String getDisplayName() {
		return ConjurSettings.getConnectionName();
	}

	@Override
	@NotNull
	public String getType() { return ConjurSettings.getFeatureType(); }

	@Override
	public String getEditParametersUrl() {
		return this.descriptor.getPluginResourcesPath("conjurconnection.jsp");
	}

	@Override
	public PropertiesProcessor getPropertiesProcessor() {
    	return new ConjurPropertiesProcessor();
	}

	@Override
	@NotNull
	public String describeConnection(OAuthConnectionDescriptor connection) {
		ConjurConnectionParameters parameters = new ConjurConnectionParameters(connection.getParameters(), false);

		String message = "NA";
		try {
			message = String.format("Connection to CyberArk Conjur server at '%s' with login '%s'",
					parameters.getApplianceUrl(), parameters.getAuthnLogin());
		} catch (MissingMandatoryParameterException e) {
			message = "Invalid Conjur Connection Configuration";
		}

		return message;
	}
}