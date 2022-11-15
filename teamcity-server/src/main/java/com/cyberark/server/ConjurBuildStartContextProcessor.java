package com.cyberark.server;

import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import com.cyberark.common.exceptions.MultipleConnectionsReturnedException;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;
import jetbrains.buildServer.serverSide.parameters.types.PasswordsProvider;

import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.cyberark.common.*;

public class ConjurBuildStartContextProcessor implements BuildStartContextProcessor, PasswordsProvider {

    // This method will params that represents the CyberArk Conjur Connection
    //   provided in the build's parent project Connections. This method will return null if no Connection can be found
    //   and will throw a MultipleConnectionsReturnedException if more than one connection was found.
    //   The convention is to only accept one and consider more as a configuration error.
    private Map<String, String> getConjurConnectionParams(SBuild build, String providerType, boolean returnSecretParamsOnly)
            throws MultipleConnectionsReturnedException, MissingMandatoryParameterException {
        SBuildType buildType = build.getBuildType();
        if (buildType == null) {
            return null;
        }

        SProject project = buildType.getProject();
        List<SProjectFeatureDescriptor> connections = new ArrayList<>();
        for (SProjectFeatureDescriptor desc : project.getAvailableFeaturesOfType(OAuthConstants.FEATURE_TYPE)) {
            String connectionType = desc.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
            if (connectionType.equals(providerType)) {
                connections.add(desc);
            }
        }

        if (connections.size() == 0) {
            return null;
        }
        if (connections.size() > 1) {
            throw new MultipleConnectionsReturnedException("Only one CyberArk Conjur Connection should be configured for this project.");
        }

        SProjectFeatureDescriptor connectionFeature = connections.get(0);
        ConjurConnectionParameters conjurConnParams = new ConjurConnectionParameters(connectionFeature.getParameters(), false);
        return conjurConnParams.getAgentSharedParameters(returnSecretParamsOnly);
    }

    private BuildProblemData createBuildProblem(SBuild build, String message) {
        return BuildProblemData.createBuildProblem(build.getBuildNumber(), ConjurSettings.getFeatureType(), message);
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        SRunningBuild build = context.getBuild();

        Map<String, String> conjurConnParams = null;
        try {
            conjurConnParams = getConjurConnectionParams(build, ConjurSettings.getFeatureType(), false);
        } catch (MultipleConnectionsReturnedException | MissingMandatoryParameterException e) {
            BuildProblemData buildProblem = createBuildProblem(build, String.format("ERROR: %s", e.getMessage()));
            build.addBuildProblem(buildProblem);
        }

        if (conjurConnParams != null) {
            for (Map.Entry<String, String> kv : conjurConnParams.entrySet()) {
                context.addSharedParameter(kv.getKey(), kv.getValue());
            }
        }
    }

    // the purpose of this method is to mark a given param as password-type
    @Override
    public Collection<Parameter> getPasswordParameters(SBuild build) {
        Map<String, String> conjurConnParams = null;
        try {
            conjurConnParams = getConjurConnectionParams(build, ConjurSettings.getFeatureType(), true);
        } catch (MultipleConnectionsReturnedException | MissingMandatoryParameterException e) {
            // mute: all the build problems will/must have already been added in updateParameters() above
        }

        ArrayList<Parameter> pwdParams = new ArrayList<>();
        if (conjurConnParams != null) {
            for (Map.Entry<String, String> kv : conjurConnParams.entrySet()) {
                pwdParams.add(new SimpleParameter(kv.getKey(), kv.getValue()));
            }
        }
        return pwdParams;
    }
}