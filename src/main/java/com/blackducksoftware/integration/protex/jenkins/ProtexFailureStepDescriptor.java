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

import hudson.Extension;
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.model.Descriptor;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.IOException;
import java.io.Serializable;

import javax.servlet.ServletException;

import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Descriptor for {@link PostBuildProtexScan}. Used as a singleton. The
 * class is marked as public so that it can be accessed from views.
 *
 * <p>
 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment for
 * the configuration screen.
 */
@Extension
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class ProtexFailureStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public ProtexFailureStepDescriptor() {
        super(ProtexFailureStep.class);
        load();
    }

    @Override
    public boolean isApplicable(Class aClass) {
        // Indicates that this builder can be used with all kinds of project
        // types
        return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    @Override
    public String getDisplayName() {
        return Messages.ProtexPostScan_getDisplayNameFailureStep();
    }

    public String getPluginVersion() {
        Plugin p = Jenkins.getInstance().getPlugin("protex-jenkins");
        PluginWrapper pw = p.getWrapper();
        return pw.getVersion();
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData)
            throws Descriptor.FormException {
        // To persist global configuration information,
        // set that to properties and call save().

        // ^Can also use req.bindJSON(this, formData);
        // (easier when there are many fields; need set* methods for this,
        // like setUseFrench)
        save();
        return super.configure(req, formData);
    }

    public FormValidation doCheckBuildFailOnPendingIDPost(
            @QueryParameter boolean value) throws IOException,
            ServletException {
        if (value) {
            return FormValidation
                    .warningWithMarkup(Messages.ProtexPostScan_getFailOnPendingId());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckBuildFailOnLicenseViolationPost(
            @QueryParameter boolean value) throws IOException,
            ServletException {
        if (value) {
            return FormValidation
                    .warningWithMarkup(Messages.ProtexPostScan_getFailOnLicenseViolations());
        }
        return FormValidation.ok();
    }

}
