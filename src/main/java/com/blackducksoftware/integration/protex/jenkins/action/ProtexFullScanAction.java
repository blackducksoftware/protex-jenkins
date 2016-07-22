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

import hudson.model.Action;
import hudson.model.AbstractProject;
import hudson.model.Cause;
import hudson.model.Cause.UserIdCause;

import java.io.IOException;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;

import org.kohsuke.stapler.StaplerProxy;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import com.blackducksoftware.integration.protex.jenkins.Messages;
import com.blackducksoftware.integration.protex.jenkins.PostBuildProtexScan;

/**
 * This action is used to hold a boolean value which is used by the {@link PostBuildProtexScan} in order to determine if
 * a full scan needs to be performed or not. This action
 * is displayed on the project/job if the {@link PostBuildProtexScan} is
 * configured properly.
 *
 * @author Paulo Santos
 *
 */
public class ProtexFullScanAction implements Action, StaplerProxy {

    private final AbstractProject<?, ?> project;

    private boolean fullScanRequired;

    /**
     * Constructor that specifies the project this action belongs to. The
     * boolean to maintain state is set to false meaning no full scan action will be performed.
     *
     * @param project
     *            The project that action belongs to.
     */
    public ProtexFullScanAction(AbstractProject<?, ?> project) {
        this.project = project;
        fullScanRequired = false;
    }

    /**
     * Constructor that specifies the project and the initial boolean value to
     * maintain the state whether a full scan should occur or not.
     *
     * @param project
     *            The project that the action belongs to.
     * @param fullScanRequired
     *            The boolean to determine if a full scan should be run or not.
     */
    public ProtexFullScanAction(AbstractProject<?, ?> project, boolean fullScanRequired) {
        this.project = project;
        this.fullScanRequired = fullScanRequired;
    }

    /**
     * Retrieve the project/job that this action belongs to.
     *
     * @return the project the action belongs to.
     */
    public AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * Determine if a full scan should be executed or not on the next build.
     *
     * @return True if the full scan should be run on the next build.
     */
    public boolean isFullScanRequired() {
        return fullScanRequired;
    }

    /**
     * Replace the boolean to determine if a full scan should be executed or not on the next build.
     *
     * @param fullScanRequired
     *            True if the full scan should be run on the next build; False otherwise.
     */
    public void setFullScanRequired(boolean fullScanRequired) {
        this.fullScanRequired = fullScanRequired;
    }

    @Override
    public String getDisplayName() {
        return Messages.ProtexFullScanAction_getDisplayName();
    }

    @Override
    public String getIconFileName() {
        if (Jenkins.getInstance().hasPermission(AbstractProject.BUILD) == false) {
            return null; // user does not have the permission so hide the action.
        }

        return "/plugin/protex-jenkins/images/blackduck.png";
    }

    @Override
    public String getUrlName() {
        return "protexrunfullscan";
    }

    @Override
    public Object getTarget() {
        // This will throw an access denied exception if the user does not have the build permission.
        Jenkins.getInstance().checkPermission(AbstractProject.BUILD);
        return this;
    }

    public void doStartProtexFullScanBuild(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
        // schedule a full scan action.
        setFullScanRequired(true);
        Cause cause = new UserIdCause();
        getProject().scheduleBuild(cause);

        // keep the user on the same page that where they invoked this action.
        rsp.forwardToPreviousPage(req);
    }
}
