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

    // This method will return one SProjectFeatureDescriptor that represents the CyberArk Conjur Connection
    //   provided in the project Connections. This method will return null if no Connection can be found and will throw
    //   a MultipleConnectionsReturnedException if more than one connection was found.
    private SProjectFeatureDescriptor getConnectionType(SProject project, String providerType) throws MultipleConnectionsReturnedException {
        List<SProjectFeatureDescriptor> connections = new ArrayList<SProjectFeatureDescriptor>();
        for (SProjectFeatureDescriptor desc : project.getAvailableFeaturesOfType(OAuthConstants.FEATURE_TYPE)) {
            String connectionType = desc.getParameters().get(OAuthConstants.OAUTH_TYPE_PARAM);
            if (connectionType.equals(providerType)) {
                connections.add(desc);
            }
        }

        // If no connections were found return null
        if (connections.size() == 0) {
            return null;
        }

        // If more than one connection was found return error
        if (connections.size() > 1 ) {
            throw new MultipleConnectionsReturnedException("Only one CyberArk Conjur Connection should be configured for this project.");
        }

        return connections.get(0);
    }

    private BuildProblemData createBuildProblem(SBuild build, String message) {
        return BuildProblemData.createBuildProblem(build.getBuildNumber(), ConjurSettings.getFeatureType(), message);
    }

    @Override
    public void updateParameters(BuildStartContext context) {
        SRunningBuild build = context.getBuild();

        SBuildType buildType = build.getBuildType();
        if (buildType == null) {
            // It is possible of build type to be null, if this is the case let's return and not retrieve conjur secrets
            return;
        }

        SProject project = buildType.getProject();
        SProjectFeatureDescriptor connectionFeature = null;

        try {
            connectionFeature = getConnectionType(project, ConjurSettings.getFeatureType());
        } catch (MultipleConnectionsReturnedException e) {
            BuildProblemData buildProblem = createBuildProblem(build, String.format("ERROR: %s", e.getMessage()));
            build.addBuildProblem(buildProblem);
        }
        
        if (connectionFeature == null) {
            // If connection feature cannot be found (no connection has been configured on this project)
            // then return and do not perform conjur secret retrieval actions
            return;
        }

        ConjurConnectionParameters conjurConnParams = new ConjurConnectionParameters(connectionFeature.getParameters(), false);

        try {
            for(Map.Entry<String, String> kv : conjurConnParams.getAgentSharedParameters().entrySet()) {
                context.addSharedParameter(kv.getKey(), kv.getValue());
            }
        } catch (MissingMandatoryParameterException e) {
            BuildProblemData buildProblem = createBuildProblem(build,
                    String.format("ERROR: Setting agent's shared parameters. %s. %s",
                            e.getMessage(), conjurConnParams));
            build.addBuildProblem(buildProblem);
        }
    }
}
