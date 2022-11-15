package com.cyberark.common;

import com.cyberark.common.exceptions.MissingMandatoryParameterException;

import java.util.HashMap;
import java.util.Map;

public class ConjurConnectionParameters {
    private final String apiKey;
    private final String authnLogin;
    private final String applianceUrl;
    private final String account;
    private final String certFile;
    private final String failOnError;
    private final String verboseLogging;
    private static final ConjurJspKey conjurKeys = new ConjurJspKey();
    private static final String agentParameterPrefix = "teamcity.conjur.";

    public ConjurConnectionParameters(Map<String, String> parameters, boolean agentSide) {
        String prefix = agentSide ? agentParameterPrefix : "";
        this.apiKey = parameters.get(prefix + conjurKeys.getApiKey());
        this.applianceUrl = parameters.get(prefix + conjurKeys.getApplianceUrl());
        this.authnLogin = parameters.get(prefix + conjurKeys.getAuthnLogin());
        this.account = parameters.get(prefix + conjurKeys.getAccount());
        this.certFile = parameters.get(prefix + conjurKeys.getCertFile());
        this.failOnError = parameters.get(prefix + conjurKeys.getFailOnError());
        this.verboseLogging = parameters.get(prefix + conjurKeys.getVerboseLogging());
    }

    public Map<String, String> getAgentSharedParameters() throws MissingMandatoryParameterException {
        HashMap<String, String> sharedParameters = new HashMap<>();

        sharedParameters.put(agentParameterPrefix + conjurKeys.getAccount(), this.getAccount());
        sharedParameters.put(agentParameterPrefix + conjurKeys.getApplianceUrl(), this.getApplianceUrl());
        sharedParameters.put(agentParameterPrefix + conjurKeys.getAuthnLogin(), this.getAuthnLogin());
        sharedParameters.put(agentParameterPrefix + conjurKeys.getApiKey(), this.getApiKey()); // TODO !!! this must be added as a password-type param instead, or must not be added at all
        sharedParameters.put(agentParameterPrefix + conjurKeys.getCertFile(), this.getCertFile());
        sharedParameters.put(agentParameterPrefix + conjurKeys.getFailOnError(), String.valueOf(this.getFailOnError()));
        sharedParameters.put(agentParameterPrefix + conjurKeys.getVerboseLogging(), String.valueOf(this.getVerboseLogging()));

        return sharedParameters;
    }

    @Override
    public String toString() {
        return String.format("%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s\n%s: %s\n",
                conjurKeys.getApplianceUrl(), this.applianceUrl,
                conjurKeys.getAccount(), this.account,
                conjurKeys.getAuthnLogin(), this.authnLogin,
                conjurKeys.getFailOnError(), this.failOnError,
                conjurKeys.getCertFile(), this.certFile,
                conjurKeys.getVerboseLogging(), this.verboseLogging);
    }

    private String trimMandatoryParameter(String input, String key) throws MissingMandatoryParameterException {
        if (input == null) {
            throw new MissingMandatoryParameterException(String.format("Failed to retrieve mandatory parameter '%s'. This should not happen", key));
        }
        return input.trim();
    }

    private String trimOptionalParameter(String input) {
        if (input != null)
            return input.trim();
        else
            return null;
    }

    private boolean isTrue(String str) {
        if (str == null) {
            return false;
        }
        return str.trim().equalsIgnoreCase("true");
    }

    public String getApplianceUrl() throws MissingMandatoryParameterException {
        String url = trimMandatoryParameter(this.applianceUrl, conjurKeys.getApplianceUrl());
        // trim any trailing '/'
        if (url.endsWith("/")) {
            url = url.substring(0, url.length()-1);
        }
        return url;
    }

    public String getAccount() throws MissingMandatoryParameterException {
        return trimMandatoryParameter(this.account, conjurKeys.getAccount());
    }

    public String getAuthnLogin() throws MissingMandatoryParameterException {
        return trimMandatoryParameter(this.authnLogin, conjurKeys.getAuthnLogin());
    }

    public String getApiKey() throws MissingMandatoryParameterException {
        return trimMandatoryParameter(this.apiKey, conjurKeys.getApiKey());
    }

    public String getCertFile() {
        String certContent = trimOptionalParameter(this.certFile);
        if (certContent == null) {
            certContent = "";
        }
        return certContent;
    }

    public boolean getFailOnError() {
        return isTrue(this.failOnError);
    }

    public boolean getVerboseLogging() {
        return isTrue(this.verboseLogging);
    }
}