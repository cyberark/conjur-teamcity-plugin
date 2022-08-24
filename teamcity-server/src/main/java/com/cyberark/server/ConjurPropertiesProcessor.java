package com.cyberark.server;

import com.cyberark.common.ConjurJspKey;
import jetbrains.buildServer.serverSide.InvalidProperty;
import jetbrains.buildServer.serverSide.PropertiesProcessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class ConjurPropertiesProcessor implements PropertiesProcessor {
    private boolean isEmptyOrNull(String str) {
        if (str == null) {
            return true;
        }
        return str.trim().isEmpty();
    }

    public Collection<InvalidProperty> process(Map<String, String> properties) {
        String shouldNotBeEmpty = "Should not be empty";
        ArrayList<InvalidProperty> errors = new ArrayList<InvalidProperty>();
        ConjurJspKey conjurKeys = new ConjurJspKey();

        // Validate Appliance url
        String url = properties.get(conjurKeys.getApplianceUrl());
        if (isEmptyOrNull(url)) {
            errors.add(new InvalidProperty(conjurKeys.getApplianceUrl(), shouldNotBeEmpty));
        } else if (!url.startsWith("https://") && !url.startsWith("http://")) {
            errors.add(new InvalidProperty(conjurKeys.getApplianceUrl(),
                    "URL should start with 'https://' or 'http://'"));
        }

        // Validate Account
        String account = properties.get(conjurKeys.getAccount());
        if (isEmptyOrNull(account)) {
            errors.add(new InvalidProperty(conjurKeys.getAccount(), shouldNotBeEmpty));
        }

        // Validate Authn Login
        String login = properties.get(conjurKeys.getAuthnLogin());
        if (isEmptyOrNull(login)) {
            errors.add(new InvalidProperty(conjurKeys.getAuthnLogin(), shouldNotBeEmpty));
        }

        // Validate api key
        String apiKey = properties.get(conjurKeys.getApiKey());
        if (isEmptyOrNull(apiKey)) {
            errors.add(new InvalidProperty(conjurKeys.getApiKey(), shouldNotBeEmpty));
        }

        return errors;
    }
}
