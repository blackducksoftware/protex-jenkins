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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import hudson.model.Descriptor.FormException;
import hudson.util.FormValidation;
import net.sf.json.JSONObject;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;
import org.kohsuke.stapler.StaplerRequest;

public class ProtexFailureStepDescriptorTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void init() throws Exception {

    }

    @AfterClass
    public static void tearDown() throws Exception {
    }

    @Test
    public void testIsApplicable() {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertTrue(descriptor.isApplicable(null));
    }

    @Test
    public void testGetDisplayName() {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertEquals(Messages.ProtexPostScan_getDisplayNameFailureStep(), descriptor.getDisplayName());
    }

    @Test
    public void testGetPluginVersion() {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertNotNull(descriptor.getPluginVersion());
//        assertTrue(descriptor.getPluginVersion().contains("1.1.0"));
    }

    @Test
    public void testConfigure() throws FormException {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        JSONObject json = new JSONObject();
        StaplerRequest req = null;
        assertTrue(descriptor.configure(req, json));
    }

    @Test
    public void testDoCheckBuildFailOnPendingIDPostFalse() throws Exception {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertEquals(FormValidation.ok(), descriptor.doCheckBuildFailOnPendingIDPost(false));
    }

    @Test
    public void testDoCheckBuildFailOnPendingIDPostTrue() throws Exception {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertEquals(Messages.ProtexPostScan_getFailOnPendingId(), descriptor.doCheckBuildFailOnPendingIDPost(true).getMessage());
    }

    @Test
    public void testDoCheckBuildFailOnLicenseViolationPostFalse() throws Exception {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertEquals(FormValidation.ok(), descriptor.doCheckBuildFailOnLicenseViolationPost(false));
    }

    @Test
    public void testDoCheckBuildFailOnLicenseViolationPostTrue() throws Exception {
        ProtexFailureStepDescriptor descriptor = new ProtexFailureStepDescriptor();
        assertEquals(Messages.ProtexPostScan_getFailOnLicenseViolations(),
                descriptor.doCheckBuildFailOnLicenseViolationPost(true).getMessage());
    }
}
