package com.cyberark.server;

import com.cyberark.common.exceptions.MissingMandatoryParameterException;
import com.cyberark.common.exceptions.MultipleConnectionsReturnedException;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.serverSide.*;
import jetbrains.buildServer.serverSide.oauth.OAuthConstants;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import com.cyberark.common.*;

public class ConjurBuildStartContextProcessor implements BuildStartContextProcessor {

    // This method will params that represents the CyberArk Conjur Connection
    //   provided in the build's parent project Connections. This method will return null if no Connection can be found
    //   and will throw a MultipleConnectionsReturnedException if more than one connection was found.
    //   The convention is to only accept one and consider more as a configuration error.
    private ConjurConnectionParameters getConjurConnectionParams(SBuild build, String providerType) throws MultipleConnectionsReturnedException {
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
        if (connections.size() > 1 ) {
            throw new MultipleConnectionsReturnedException("Only one CyberArk Conjur Connection should be configured for this project.");
        }

        SProjectFeatureDescriptor connectionFeature = connections.get(0);
        return new ConjurConnectionParameters(connectionFeature.getParameters(), false);
    }

    private BuildProblemData createBuildProblem(SBuild build, String message) {
        return BuildProblemData.createBuildProblem(build.getBuildNumber(), ConjurSettings.getFeatureType(), message);
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        SRunningBuild build = context.getBuild();

        ConjurConnectionParameters conjurConnParams = null;
        try {
            conjurConnParams = getConjurConnectionParams(build, ConjurSettings.getFeatureType());
        } catch (MultipleConnectionsReturnedException e) {
            BuildProblemData buildProblem = createBuildProblem(build, String.format("ERROR: %s", e.getMessage()));
            build.addBuildProblem(buildProblem);
        }

        try {
            if (conjurConnParams != null) {
                for (Map.Entry<String, String> kv : conjurConnParams.getAgentSharedParameters().entrySet()) {
                    context.addSharedParameter(kv.getKey(), kv.getValue());
                }
            }
        } catch (MissingMandatoryParameterException e) {
            BuildProblemData buildProblem = createBuildProblem(build,
                    String.format("ERROR: Setting agent's shared parameters. %s. %s", e.getMessage(), conjurConnParams));
            build.addBuildProblem(buildProblem);
        }
    }
}
