/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *******************************************************************************/
package com.blackducksoftware.integration.protex.jenkins.action;

import hudson.EnvVars;
import hudson.model.EnvironmentContributingAction;
import hudson.model.AbstractBuild;

import org.apache.commons.lang3.StringUtils;

public class ProtexVariableContributorAction implements EnvironmentContributingAction {
    public static final String PROTEX_SERVER_URL = "PROTEX_SERVER_URL";

    public static final String PROTEX_PROJECT_NAME = "PROTEX_PROJECT_NAME";

    public static final String PROTEX_TEMPLATE_PROJECT_NAME = "PROTEX_TEMPLATE_PROJECT_NAME";

    public static final String PROTEX_PROJECT_SOURCE_PATH = "PROTEX_PROJECT_SOURCE_PATH";

    private final String protexServerUrl;

    private final String protexProjectName;

    private final String protexTemplateProjectName;

    private final String protexProjectSourcePath;

    public ProtexVariableContributorAction(String protexServerUrl, String protexProjectName, String protexTemplateProjectName, String protexProjectSourcePath) {
        this.protexServerUrl = protexServerUrl;
        this.protexProjectName = protexProjectName;
        this.protexTemplateProjectName = protexTemplateProjectName;
        this.protexProjectSourcePath = protexProjectSourcePath;
    }

    @Override
    public String getIconFileName() {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public String getDisplayName() {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public String getUrlName() {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public void buildEnvVars(AbstractBuild<?, ?> build, EnvVars env) {
        if (!StringUtils.isEmpty(protexServerUrl)) {
            env.put(PROTEX_SERVER_URL, protexServerUrl);
        }
        if (!StringUtils.isEmpty(protexProjectName)) {
            env.put(PROTEX_PROJECT_NAME, protexProjectName);
        }
        if (!StringUtils.isEmpty(protexTemplateProjectName)) {
            env.put(PROTEX_TEMPLATE_PROJECT_NAME, protexTemplateProjectName);
        }
        if (!StringUtils.isEmpty(protexProjectSourcePath)) {
            env.put(PROTEX_PROJECT_SOURCE_PATH, protexProjectSourcePath);
        }
    }

}
