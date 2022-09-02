package com.cyberark.agent;

import com.cyberark.common.*;
import com.cyberark.common.exceptions.ConjurApiAuthenticateException;
import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.util.EventDispatcher;
import jetbrains.buildServer.util.ssl.SSLTrustStoreProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ConjurBuildFeature extends AgentLifeCycleAdapter {
    public EventDispatcher<AgentLifeCycleListener> dispatcher;
    public SSLTrustStoreProvider trustStoreProvider;

    public ConjurBuildFeature(EventDispatcher<AgentLifeCycleListener> dispatcher, SSLTrustStoreProvider trustStoreProvider) {
        this.dispatcher = dispatcher;
        this.trustStoreProvider = trustStoreProvider;
        this.dispatcher.addListener(this);
    }

    // This method will turn a map of SOMETHING = %conjur:some/secret% into
    // SOMETHING = some/secret
    // input == {
    //   "env.SECRET": "%conjur:super/secret%",
    //   "env.DB_PASS": "%conjur:db/mysql/username%",
    //   "TEAMCITY_BUILD": "22"
    // }
    //
    // All non-conjur variables should not be returned
    // Also the %conjur: and % should be removed from the value
    // The key should remain the same
    // output == {
    //   "env.SECRET": "super/secret",
    //   "env.DB_PASS": "db/mysql/username"
    // }
    private Map<String, String> getVariableIdsFromBuildParameters(Map<String, String> parameters) {
        Map<String, String> variableIds = new HashMap<>();

        for (Map.Entry<String, String> kv : parameters.entrySet() ) {
            String variableIdPrefix = "%conjur:";
            String variableIdSuffix = "%";

            if (kv.getValue().startsWith(variableIdPrefix) && kv.getValue().endsWith(variableIdSuffix)) {
                // This value represents that this parameter needs to be replaced
                String id = kv.getValue().trim();
                id = id.substring(variableIdPrefix.length());
                id = id.substring(0, id.length()-variableIdSuffix.length());

                variableIds.put(kv.getKey(), id);
            }
        }

        return variableIds;
    }

    @Override
    public void buildStarted(AgentRunningBuild runningBuild) {
        BuildProgressLogger buildLogger = runningBuild.getBuildLogger();
        ConjurConnectionParameters conjurConfig = new ConjurConnectionParameters(runningBuild.getSharedConfigParameters(), true);
        LogUtil logger = new LogUtil(buildLogger, conjurConfig.getVerboseLogging());
		if(conjurConfig.isNotDefined()){
			logger.Verbose("No Conjur Connection Parameters defined. Build is proceeding ...");
			return;
		}
        ConjurConfig config = null;
        try {

            config = new ConjurConfig(
                    conjurConfig.getApplianceUrl(),
                    conjurConfig.getAccount(),
                    conjurConfig.getAuthnLogin(),
                    conjurConfig.getApiKey(),
                    null,
                    conjurConfig.getCertFile());
        } catch (MissingMandatoryParameterException e) {
            String message = String.format("ERROR: Retrieving conjur agent's shared parameters. %s", e.getMessage());
            buildLogger.logBuildProblem(
                    BuildProblemData.createBuildProblem(
                            "ConjurConnection", "ConjurConnection", message));
            runningBuild.stopBuild(message);
        }

        Map<String, String> buildParams = runningBuild.getSharedBuildParameters().getAllParameters();
        Map<String, String> conjurVariables = getVariableIdsFromBuildParameters(buildParams);

        if (conjurVariables.size() == 0) {
            logger.Verbose("No conjur variables were found within the shared build parameters.");
            // No conjur variables are present in the build parameters, if this is the case lets not attempt to
            // authenticate and just return
            return;
        }

        ConjurApi client = new ConjurApi(config);
        try {
            logger.Verbose("Attempting to Authenticate to conjur");
            client.authenticate();

            // iterate over each variable that has been found
            for(Map.Entry<String, String> kv : conjurVariables.entrySet()) {
                logger.Verbose(String.format("Attempting to retrieve secret '%s' with id '%s'", kv.getKey(), kv.getValue()));
                HttpResponse response = client.getSecret(kv.getValue());
                if (response.statusCode != 200 && conjurConfig.getFailOnError()) {
                    String message =  String.format("ERROR: Retrieving secret '%s' from conjur. Received status code '%d'",
                            kv.getValue(), response.statusCode);
                    buildLogger.logBuildProblem(
                            BuildProblemData.createBuildProblem(
                                    "ConjurConnection", "ConjurConnection", message));
                    runningBuild.stopBuild(message);
                }
                logger.Verbose(String.format("Successfully retrieved secret '%s' with id '%s'", kv.getKey(), kv.getValue()));
                kv.setValue(response.body);
            }

        } catch (ConjurApiAuthenticateException e) {
            if (!conjurConfig.getFailOnError()) {
                // Failed to authenticate but no fail on error, do not set running build parameters
                return;
            }
            String message = String.format(
                    "ERROR: Authenticating to conjur at '%s' with account '%s' and with login '%s'. %s",
                    config.url,
                    config.account,
                    config.username,
                    e.getMessage());

            buildLogger.logBuildProblem(
                    BuildProblemData.createBuildProblem(
                            "ConjurConnection", "ConjurConnection", message));
            runningBuild.stopBuild(message);
        }
        catch (Exception e) {
            if (!conjurConfig.getFailOnError()) {
                return;
            }
            String message =  String.format("ERROR: Generic error returned when establishing connection to conjur. %s", e.getMessage());

            buildLogger.logBuildProblem(
                    BuildProblemData.createBuildProblem(
                            "ConjurConnection", "ConjurConnection", message));
            runningBuild.stopBuild(message);
        }

        // TODO: Currently this is only going to set the conjur parameter as an environment variables
        //   this may be undesirable for users of this plugin when they are trying to set `system` or `config` parameters
        for(Map.Entry<String, String> kv : conjurVariables.entrySet()) {
            String  envVar = kv.getKey().substring(4, kv.getKey().length());
            logger.Verbose(String.format("Setting secret '%s' with id '%s'", envVar, kv.getKey()));
            runningBuild.addSharedEnvironmentVariable(envVar, kv.getValue());
            runningBuild.getPasswordReplacer().addPassword(kv.getValue());
        }
    }
}
