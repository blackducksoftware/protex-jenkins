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
import hudson.model.AbstractBuild;

import com.blackducksoftware.integration.protex.jenkins.Messages;

public class ProtexReportAction implements Action {

    private final AbstractBuild<?, ?> build;

    // All persisted fields must be serializable
    // Storing the html report as a String , Good or Bad idea? Suggestions?
    private final String reportHtmlContent;

    public ProtexReportAction(AbstractBuild<?, ?> build, String reportHtmlContent) {
        this.build = build;
        this.reportHtmlContent = reportHtmlContent;
    }

    public AbstractBuild<?, ?> getBuild() {
        return build;
    }

    public String getReportHtmlContent() {
        return reportHtmlContent;
    }

    @Override
    public String getIconFileName() {
        return "/plugin/protex-jenkins/images/blackduck.png";
    }

    @Override
    public String getDisplayName() {
        return Messages.ProtexReportAction_getDisplayName();
    }

    @Override
    public String getUrlName() {
        return "protexreport";
    }

}
