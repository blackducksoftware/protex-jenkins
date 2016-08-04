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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.blackducksoftware.integration.protex.ProtexServerInfo;
import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.protex.helper.TestHelper;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;

import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepMonitor;

public class PostBuildProtexScanTest {
    private static PrintStream orgStream = null;

    private static PrintStream orgErrStream = null;

    private static PrintStream currStream = null;

    private ByteArrayOutputStream byteOutput = null;

    private static File testWorkspace = new File("testWorkspace");

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void init() throws Exception {
        orgStream = System.out;
        orgErrStream = System.err;

        if (!testWorkspace.exists()) {
            testWorkspace.mkdir();
        }

    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.setOut(orgStream);
        System.setErr(orgErrStream);
    }

    @Before
    public void testSetup() throws IOException {
        byteOutput = new ByteArrayOutputStream();
        currStream = new PrintStream(byteOutput);
        System.setOut(currStream);
        System.setErr(currStream);
    }

    @After
    public void testTearDown() throws Exception {
        System.setOut(orgStream);
        System.setErr(orgErrStream);
        byteOutput.close();
        byteOutput = null;
        currStream.close();
        currStream = null;
    }

    private AbstractBuild<?, ?> getMockedAbstractBuild(final Result buildResult, final TestBuildListener listener) throws Exception {

        final AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) {
                return null;
            }
        }).when(build).setResult(Mockito.any(Result.class));

        Mockito.when(build.getResult()).thenAnswer(new Answer<Result>() {
            @Override
            public Result answer(final InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return buildResult;
            }
        });

        final FreeStyleProject proj = j.createFreeStyleProject("testing");
        final AbstractBuild<?, ?> realBuild = proj.scheduleBuild2(0).get();
        final EnvVars variables = realBuild.getEnvironment(listener);
        final Node node = realBuild.getBuiltOn();

        Mockito.when(build.getEnvironment(Mockito.any(TaskListener.class))).thenAnswer(new Answer<EnvVars>() {
            @Override
            public EnvVars answer(final InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return variables;
            }
        });

        Mockito.when(build.getProject()).thenAnswer(new Answer<FreeStyleProject>() {
            @Override
            public FreeStyleProject answer(final InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return proj;
            }
        });

        Mockito.when(build.getBuiltOn()).thenAnswer(new Answer<Node>() {
            @Override
            public Node answer(final InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return node;
            }
        });

        return build;
    }

    @WithoutJenkins
    @Test
    public void testGetRequiredMonitorService() {
        final PostBuildProtexScan postScan = new PostBuildProtexScan(null, null, null, null, null, null, null);
        assertEquals(BuildStepMonitor.NONE, postScan.getRequiredMonitorService());
    }

    @Test
    public void testPostBuildProtexScanCreationEmpty() {
        final PostBuildProtexScan postScan = new PostBuildProtexScan(null, null, null, null, null, null, null);
        assertNull(postScan.getProtexServerId());
        assertNull(postScan.getProtexPostCredentials());
        assertNull(postScan.getProtexPostProjectName());
        assertNull(postScan.getProtexPostTemplateProjectName());
        assertNull(postScan.getProtexPostProjectSourcePath());
        assertNotNull(postScan.getDescriptor());
    }

    @Test
    public void testPostBuildProtexScanCreation() {
        final String uuid = UUID.randomUUID().toString();
        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, "CredentialID", "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertEquals(uuid, postScan.getProtexServerId());
        assertEquals("CredentialID", postScan.getProtexPostCredentials());
        assertEquals("TestProjectName", postScan.getProtexPostProjectName());
        assertEquals("TemplateProjectName", postScan.getProtexPostTemplateProjectName());
        assertEquals("/NONEXISTENT/PATH", postScan.getProtexPostProjectSourcePath());
        assertNotNull(postScan.getDescriptor());
    }

    @WithoutJenkins
    @Test
    public void testPluginConfiguredMissingServerId() {
        final PostBuildProtexScan postScan = new PostBuildProtexScan(null, "CredentialID", "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());
    }

    @WithoutJenkins
    @Test
    public void testPluginConfiguredMissingCredentialId() {
        final String uuid = UUID.randomUUID().toString();
        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, null, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());

    }

    @WithoutJenkins
    @Test
    public void testPluginConfiguredMissingProjectName() {
        final String uuid = UUID.randomUUID().toString();
        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, "CredentialID", null, "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());
    }

    @Test
    public void testPrintConfiguration() throws Exception {
        final String uuid = UUID.randomUUID().toString();

        final String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        final ProtexServerInfo currentServer = new ProtexServerInfo("Protex Test Name", "http://testURL", "100", uuid);

        final PostBuildProtexScanDescriptor desc = j.jenkins.getDescriptorByType(PostBuildProtexScanDescriptor.class);
        desc.getProtexServers().add(currentServer);

        final FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        final FreeStyleBuild build = proj.scheduleBuild2(0).get();

        final String output = build.getLog();
        assertTrue(output, output.contains("Initializing - Protex Jenkins Plugin"));
        assertTrue(output, output.contains("Using Url : http://testURL"));
        assertTrue(output, output.contains("Using Username : TestUser"));
        assertTrue(output, output.contains("Using Protex Global TimeOut : 100"));
        assertTrue(output, output.contains("Using Build Full Name :"));
        assertTrue(output, output.contains("Using Build Number :"));
        assertTrue(output, output.contains("Using Build Workspace Path :"));
        assertTrue(output, output.contains("Using Protex Project Name : TestProjectName"));
        assertTrue(output, output.contains("Using Protex Source Path  : /NONEXISTENT/PATH"));

    }

    @Test
    public void testVariableReplacementVariableNotSet() throws Exception {
        final String uuid = UUID.randomUUID().toString();

        final TestBuildListener listener = new TestBuildListener(currStream);
        final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        final String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        final AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);

        final EnvVars variables = build.getEnvironment(listener);

        final String value = "UserSetVariableAs-${NON_EXISTING_VARIABLE}";

        assertEquals(value, postScan.handleVariableReplacement(build, logger, variables, value));

        final String output = byteOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertEquals(2, split.length);
        assertTrue(output
                .contains("Variable was not properly replaced. Value : UserSetVariableAs-${NON_EXISTING_VARIABLE}, Result : UserSetVariableAs-${NON_EXISTING_VARIABLE}"));
        assertTrue(output, output.contains("Make sure the variable has been properly defined."));
    }

    @Test
    public void testVariableReplacement() throws Exception {
        final String uuid = UUID.randomUUID().toString();

        final TestBuildListener listener = new TestBuildListener(currStream);
        final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        final String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        final AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);
        final EnvVars variables = build.getEnvironment(listener);

        final String value = "TestProject-${JOB_NAME}";

        assertEquals("TestProject-testing", postScan.handleVariableReplacement(build, logger, variables, value));

        final String value2 = "ProjectVersion-${BUILD_NUMBER}";

        assertEquals("ProjectVersion-1", postScan.handleVariableReplacement(build, logger, variables, value2));

    }

    @Test
    public void testVariableReplacementEmpty() throws Exception {
        final String uuid = UUID.randomUUID().toString();

        final TestBuildListener listener = new TestBuildListener(currStream);
        final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        final String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        final PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        final AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);
        final EnvVars variables = build.getEnvironment(listener);

        final String value = "";

        assertEquals("", postScan.handleVariableReplacement(build, logger, variables, value));

        assertNull(postScan.handleVariableReplacement(build, logger, variables, null));

    }

}
