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
package com.blackducksoftware.integration.protex.jenkins;

import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.protex.ProtexFacade;
import com.blackducksoftware.integration.protex.ProtexFacadeException;
import com.blackducksoftware.integration.protex.jenkins.action.ScanRunAction;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class ProtexFailureStep extends Recorder {

    // Init Variables for Protex Plugin

    private final Boolean buildFailOnPendingIDPost;

    private final Boolean buildFailOnLicenseViolationPost;

    private transient Result result;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public ProtexFailureStep(boolean buildFailOnPendingIDPost, boolean buildFailOnLicenseViolationPost) {
        // These are the fields for the plugin

        this.buildFailOnPendingIDPost = buildFailOnPendingIDPost;
        this.buildFailOnLicenseViolationPost = buildFailOnLicenseViolationPost;

    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public boolean getBuildFailOnPendingIDPost() {
        return buildFailOnPendingIDPost;
    }

    public boolean getBuildFailOnLicenseViolationPost() {
        return buildFailOnLicenseViolationPost;
    }

    @Override
    public ProtexFailureStepDescriptor getDescriptor() {
        return (ProtexFailureStepDescriptor) super.getDescriptor();
    }

    private void setResult(Result result) {
        this.result = result;
    }

    /**
     * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build
     * Action
     *
     * @param build
     *            AbstractBuild
     * @param launcher
     *            Launcher
     * @param listener
     *            BuildListener
     * @throws InterruptedException
     * @throws IOException
     *
     */
    @Override
    public boolean perform(AbstractBuild build, Launcher launcher,
            BuildListener listener) throws IOException, InterruptedException {
        setResult(build.getResult());
        ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.DEBUG);
        if (result.equals(Result.SUCCESS)) {

            PostBuildProtexScan protexScan = (PostBuildProtexScan) build.getProject().getPublishersList().get(PostBuildProtexScan.class);
            if (protexScan == null) {
                // The User did not configure this job to run the Protex Scan
                logger.error("The Protex scan needs to run before you can check the Failure Conditions.");
                logger.error("Please add the Protex Scan to the Job configuration.");
                build.setResult(Result.UNSTABLE);
                return true;
            }

            ScanRunAction scanAction = build.getAction(ScanRunAction.class);

            if (scanAction == null) {
                // We can not force the order of the Post Build steps
                // The best we can do is catch when the Users configures the steps in the wrong order, print a
                // message, and make the build Unstable

                // If the scanAction is null the scan has not run yet

                logger.error("The Protex scan needs to run before you can check the Failure Conditions.");
                logger.error("Please fix the order in the Job configuration.");
                build.setResult(Result.UNSTABLE);
                return true;
            }

            if (!protexScan.isPluginConfigured()) {
                // The User did not configure the Protex Scan correctly
                logger.error("The Protex scan was not configured correctly.");
                logger.error("Please configure the Protex scan before running the Failure Conditions");
                build.setResult(Result.UNSTABLE);
                return true;
            }

            ClassLoader originalClassLoader = Thread.currentThread()
                    .getContextClassLoader();

            boolean changed = false;
            try {

                if (PostBuildProtexScan.class.getClassLoader() != originalClassLoader) {
                    changed = true;
                    Thread.currentThread().setContextClassLoader(PostBuildProtexScan.class.getClassLoader());
                }

                ProtexFacade facade = null;
                try {
                    facade = protexScan.getProtexFacade(logger);
                    if (facade == null) {
                        logger.error("There was a problem creating the ProtexFacade");
                        build.setResult(Result.UNSTABLE);
                        return true;
                    }
                } catch (Exception e1) {
                    // Catch all errors and print stack trace of any errors that
                    // happened in creation of the ProtexFacade
                    logger.error(e1);
                    build.setResult(Result.UNSTABLE);
                    return true;
                }

                EnvVars variables = build.getEnvironment(listener);

                String projectName = protexScan.handleVariableReplacement(build, logger, variables, protexScan.getProtexPostProjectName());
                if (!build.getResult().equals(Result.SUCCESS)) {
                    return true;
                }

                String projectId = facade.getProtexProjectId(projectName);
                if (projectId == null) {
                    logger.error("The project Id was returned as null");
                    build.setResult(Result.UNSTABLE);
                    return true;
                }
                logger.info("Checking the scan results against the Failure Conditions");
                if (!checkProtexFailConditions(facade, logger, projectId)) {
                    build.setResult(Result.FAILURE);
                    return true;
                }
            } catch (ServerConnectionException e) {
                logger.error(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
                return true;
            } catch (ProtexFacadeException e) {
                if (e.getSdkFaultErrorCode() != null) {
                    logger.error("SdkFault ErrorCode : " + e.getSdkFaultErrorCode().toString());
                }
                if (e.getSdkFaultMessage() != null) {
                    logger.error("SdkFault ErrorMessage : " + e.getSdkFaultMessage());
                }
                logger.error(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
                return true;
            } catch (ServerConfigException e) {
                logger.error(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
                return true;
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
                return true;
            } finally {
                if (changed) {
                    Thread.currentThread().setContextClassLoader(
                            originalClassLoader);
                }
            }

        } else {
            logger.warn("Build was not successful. Will not run Protex Failure Conditions.");
        }
        logger.info("Finished running Protex Failure Conditions.");
        build.setResult(result);
        return true;
    }

    /**
     * Check the results of the Protex Scan against the Failure Conditions that the User has set.
     *
     * @return true if passed the checks, false otherwise
     * @throws ServerConfigException
     * @throws ProtexFacadeException
     * @throws ServerConnectionException
     */
    public Boolean checkProtexFailConditions(ProtexFacade facade, ProtexJenkinsLogger logger, String projectId) throws ServerConnectionException,
            ProtexFacadeException,
            ServerConfigException {
        boolean passedChecks = true;
        if (getBuildFailOnPendingIDPost() || getBuildFailOnLicenseViolationPost()) {
            if (getBuildFailOnPendingIDPost()) {
                long pendingIds = facade.getPendingIds(projectId);
                if (pendingIds > 0) {
                    logger.error("Failing the Build because there are  : " + pendingIds + ", File(s) Pending Id");
                    passedChecks = false;
                }
            }

            if (getBuildFailOnLicenseViolationPost()) {
                long licenseViolations = facade.getViolationCount(projectId);
                if (licenseViolations > 0) {
                    logger.error("Failing the Build because there are  : " + licenseViolations + ", License Violation(s)");
                    passedChecks = false;
                }
            }
        }
        return passedChecks;
    }
}
