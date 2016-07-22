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
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.model.AbstractBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Node;
import hudson.tasks.BuildStepMonitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
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

import com.blackducksoftware.integration.protex.ProtexFacade;
import com.blackducksoftware.integration.protex.ProtexFacadeException;
import com.blackducksoftware.integration.protex.ProtexServerInfo;
import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.protex.helper.TestHelper;
import com.blackducksoftware.integration.protex.helper.TestLogger;
import com.blackducksoftware.integration.protex.jenkins.action.ProtexVariableContributorAction;
import com.blackducksoftware.integration.protex.jenkins.action.ProtexReportAction;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;

public class PostBuildProtexScanTest {
    private static PrintStream orgStream = null;

    private static PrintStream orgErrStream = null;

    private static PrintStream currStream = null;

    private ByteArrayOutputStream byteOutput = null;

    private static Properties testProperties;

    private static File testWorkspace = new File("testWorkspace");

    private static TestHelper helper = null;

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @BeforeClass
    public static void init() throws Exception {
        testProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("test.properties");
        // URL passwordFile = classLoader.getResource("encryptedPasswordFile.txt");
        // System.setProperty("ENCRYPTED_PASSWORD_FILE", passwordFile.getPath());
        try {
            testProperties.load(is);
        } catch (IOException e) {
            System.err.println("reading test.properties failed!");
        }

        orgStream = System.out;
        orgErrStream = System.err;

        helper = new TestHelper(testProperties.getProperty("TEST_PROTEX_SERVER_URL"), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        helper.setLogger(new TestLogger());
        try {
            helper.createProtexProject(testProperties.getProperty("TEST_PROJECT_NAME"), null);
        } catch (ProtexFacadeException e) {
            if (e.getMessage().contains("Error while creating project") && e.getMessage().contains("already exists")) {
                // do nothing
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

        AbstractBuild<?, ?> build = Mockito.mock(AbstractBuild.class);

        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) {
                return null;
            }
        }).when(build).setResult(Mockito.any(Result.class));

        Mockito.when(build.getResult()).thenAnswer(new Answer<Result>() {
            @Override
            public Result answer(InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return buildResult;
            }
        });

        final FreeStyleProject proj = j.createFreeStyleProject("testing");
        AbstractBuild<?, ?> realBuild = proj.scheduleBuild2(0).get();
        final EnvVars variables = realBuild.getEnvironment(listener);
        final Node node = realBuild.getBuiltOn();

        Mockito.when(build.getEnvironment(Mockito.any(TaskListener.class))).thenAnswer(new Answer<EnvVars>() {
            @Override
            public EnvVars answer(InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return variables;
            }
        });

        Mockito.when(build.getProject()).thenAnswer(new Answer<FreeStyleProject>() {
            @Override
            public FreeStyleProject answer(InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return proj;
            }
        });

        Mockito.when(build.getBuiltOn()).thenAnswer(new Answer<Node>() {
            @Override
            public Node answer(InvocationOnMock invocation) throws IOException, InterruptedException, ExecutionException {
                return node;
            }
        });

        return build;
    }

    @WithoutJenkins
    @Test
    public void testGetRequiredMonitorService() {
        PostBuildProtexScan postScan = new PostBuildProtexScan(null, null, null, null, null, null, null);
        assertEquals(BuildStepMonitor.NONE, postScan.getRequiredMonitorService());
    }

    @Test
    public void testPostBuildProtexScanCreationEmpty() {
        PostBuildProtexScan postScan = new PostBuildProtexScan(null, null, null, null, null, null, null);
        assertNull(postScan.getProtexServerId());
        assertNull(postScan.getProtexPostCredentials());
        assertNull(postScan.getProtexPostProjectName());
        assertNull(postScan.getProtexPostTemplateProjectName());
        assertNull(postScan.getProtexPostProjectSourcePath());
        assertNotNull(postScan.getDescriptor());
    }

    @Test
    public void testPostBuildProtexScanCreation() {
        String uuid = UUID.randomUUID().toString();
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, "CredentialID", "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
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
        PostBuildProtexScan postScan = new PostBuildProtexScan(null, "CredentialID", "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());
    }

    @WithoutJenkins
    @Test
    public void testPluginConfiguredMissingCredentialId() {
        String uuid = UUID.randomUUID().toString();
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, null, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());

    }

    @WithoutJenkins
    @Test
    public void testPluginConfiguredMissingProjectName() {
        String uuid = UUID.randomUUID().toString();
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, "CredentialID", null, "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);
        assertFalse(postScan.isPluginConfigured());
    }

    @Test
    public void testPrintConfiguration() throws Exception {
        String uuid = UUID.randomUUID().toString();

        String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        ProtexServerInfo currentServer = new ProtexServerInfo("Protex Test Name", "http://testURL", "100", uuid);

        PostBuildProtexScanDescriptor desc = j.jenkins.getDescriptorByType(PostBuildProtexScanDescriptor.class);
        desc.getProtexServers().add(currentServer);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        FreeStyleBuild build = proj.scheduleBuild2(0).get();

        String output = build.getLog();
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
        String uuid = UUID.randomUUID().toString();

        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);

        EnvVars variables = build.getEnvironment(listener);

        String value = "UserSetVariableAs-${NON_EXISTING_VARIABLE}";

        assertEquals(value, postScan.handleVariableReplacement(build, logger, variables, value));

        String output = byteOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertEquals(2, split.length);
        assertTrue(output
                .contains("Variable was not properly replaced. Value : UserSetVariableAs-${NON_EXISTING_VARIABLE}, Result : UserSetVariableAs-${NON_EXISTING_VARIABLE}"));
        assertTrue(output, output.contains("Make sure the variable has been properly defined."));
    }

    @Test
    public void testVariableReplacement() throws Exception {
        String uuid = UUID.randomUUID().toString();

        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);
        EnvVars variables = build.getEnvironment(listener);

        String value = "TestProject-${JOB_NAME}";

        assertEquals("TestProject-testing", postScan.handleVariableReplacement(build, logger, variables, value));

        String value2 = "ProjectVersion-${BUILD_NUMBER}";

        assertEquals("ProjectVersion-1", postScan.handleVariableReplacement(build, logger, variables, value2));

    }

    @Test
    public void testVariableReplacementEmpty() throws Exception {
        String uuid = UUID.randomUUID().toString();

        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
        String credentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TestUser", "TestPassword");

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, credentials, "TestProjectName", "TemplateProjectName", "/NONEXISTENT/PATH", 2.0, null);

        AbstractBuild<?, ?> build = getMockedAbstractBuild(Result.SUCCESS, listener);
        EnvVars variables = build.getEnvironment(listener);

        String value = "";

        assertEquals("", postScan.handleVariableReplacement(build, logger, variables, value));

        assertNull(postScan.handleVariableReplacement(build, logger, variables, null));

    }

    @Test
    public void testValidateSourcePathOutsideWorkspace() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, "TestProjectName", "", "../..", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);
        AbstractBuild<?, ?> realBuild = proj.scheduleBuild2(0).get();
        String output = realBuild.getLog();
        assertTrue(
                output,
                output.contains("Can not specify a source path outside of the workspace"));
    }

    @Test
    public void testValidateSourcePathNotFound() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, "TestProjectName", "",
                "this/target/should/not/exist", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);
        AbstractBuild<?, ?> realBuild = proj.scheduleBuild2(0).get();
        String output = realBuild.getLog();
        assertTrue(output, output.contains("Source path could not be found :"));
    }

    @Test
    public void testValidateSourcePathExists() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, "TestProjectName", "", "./", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);
        AbstractBuild<?, ?> realBuild = proj.scheduleBuild2(0).get();
        String output = realBuild.getLog();
        assertTrue(output, output.contains("Source path exists at :"));

    }

    @Test
    public void testPerform() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        String output = build.getLog();

        assertTrue(output, output.contains("Initializing - Protex Jenkins Plugin"));
        assertTrue(output, output.contains("Starting Protex Scan..."));
        assertTrue(output, output.contains("Completed Protex Scan"));
        assertTrue(output, output.contains("Finished running Protex Post Build Step."));

        EnvVars vars = build.getEnvironment();
        assertNotNull(vars.get(ProtexVariableContributorAction.PROTEX_PROJECT_NAME));
        assertNotNull(vars.get(ProtexVariableContributorAction.PROTEX_SERVER_URL));
        assertNull(vars.get(ProtexVariableContributorAction.PROTEX_TEMPLATE_PROJECT_NAME));
        assertNull(vars.get(ProtexVariableContributorAction.PROTEX_PROJECT_SOURCE_PATH));
    }

    @Test
    public void testCheckEnvVarsSetCorrectly() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, "TestProject${JOB_NAME}", "TempProject${BUILD_NUMBER}",
                "${WORKSPACE}", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("CheckVars");
        proj.getPublishersList().add(postScan);

        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        EnvVars vars = build.getEnvironment();
        assertEquals("TestProjectCheckVars", vars.get(ProtexVariableContributorAction.PROTEX_PROJECT_NAME));
        assertEquals(testProperties.getProperty("TEST_PROTEX_SERVER_URL"), vars.get(ProtexVariableContributorAction.PROTEX_SERVER_URL));
        assertEquals("TempProject1", vars.get(ProtexVariableContributorAction.PROTEX_TEMPLATE_PROJECT_NAME));
        assertNotNull(vars.get(ProtexVariableContributorAction.PROTEX_PROJECT_SOURCE_PATH));
    }

    @Test
    public void testPerformInvalidUrl() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", "http://NONEXISTENT", "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        String output = build.getLog();

        assertTrue(output, output.contains("java.net.UnknownHostException: NONEXISTENT"));

    }

    @Test
    public void testPerformInvalidCredentials() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String inValidCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "ASSERTFAKEUSER",
                "FAKEPASSWORD");
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, inValidCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        String output = build.getLog();

        assertTrue(
                output,
                output
                        .contains("com.blackducksoftware.integration.protex.ProtexFacadeException: Error checking the project 'JenkinsProtexPluginTests' :The user name or password provided was not valid."));

    }

    @Test
    public void testPerformNonExistentSourceTarget() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "NON-Existent", 2.0, null);
        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.getPublishersList().add(postScan);

        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        String output = build.getLog();

        assertTrue(output, output.contains("com.blackducksoftware.integration.protex.exceptions.ProtexValidationException: Source path could not be found :"));

    }

    /*
     * Gets all the fields in the Class provided and its superclasses
     */
    public List<Field> getAllFields(List<Field> fields, Class<?> type) {
        fields.addAll(Arrays.asList(type.getDeclaredFields()));

        if (type.getSuperclass() != null) {
            fields = getAllFields(fields, type.getSuperclass());
        }

        return fields;
    }

    @Test
    public void testGenerateProtexReportReportTemplateDNE() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        TestLogger logger = new TestLogger();

        ProtexFacade facade = postScan.getProtexFacade(logger);

        String projectId = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));

        postScan.generateProtexReport(build, logger, facade, projectId, "Fake Template ASSERTION");

        String output = logger.getOutputString();
        String errorOutput = logger.getErrorOutputString();

        assertTrue(output, output.contains("Creating Protex Report from template : Fake Template ASSERTION"));
        assertTrue(output, output.contains("Could not find the Report Template : Fake Template ASSERTION"));
        assertTrue(errorOutput, errorOutput.isEmpty());

        assertNull(build.getAction(ProtexReportAction.class));
    }

    @Test
    public void testGenerateProtexReport() throws Exception {
        String uuid = UUID.randomUUID().toString();

        PostBuildProtexScanDescriptor descriptor = j.getInstance().getDescriptorByType(PostBuildProtexScanDescriptor.class);

        ProtexServerInfo goodUrl = new ProtexServerInfo("testServer", testProperties.getProperty("TEST_PROTEX_SERVER_URL"), "100", uuid);

        descriptor.getProtexServers().add(goodUrl);

        String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        PostBuildProtexScan postScan = new PostBuildProtexScan(uuid, validCredentials, testProperties.getProperty("TEST_PROJECT_NAME"), "",
                "", 2.0, null);

        FreeStyleProject proj = j.createFreeStyleProject("testing");
        proj.setCustomWorkspace("testWorkspace");
        AbstractBuild<?, ?> build = proj.scheduleBuild2(0).get();

        TestLogger logger = new TestLogger();

        ProtexFacade facade = postScan.getProtexFacade(logger);

        String projectId = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));

        postScan.generateProtexReport(build, logger, facade, projectId, testProperties.getProperty("TEST_REPORT_TEMPLATE"));

        String output = logger.getOutputString();
        String errorOutput = logger.getErrorOutputString();

        assertTrue(output, output.contains("Creating Protex Report from template : " + testProperties.getProperty("TEST_REPORT_TEMPLATE")));
        assertTrue(output, !output.contains("Could not find the Report Template : " + testProperties.getProperty("TEST_REPORT_TEMPLATE")));

        assertTrue(errorOutput, errorOutput.isEmpty());

        ProtexReportAction reportAction = build.getAction(ProtexReportAction.class);
        assertNotNull(reportAction);
        assertTrue(reportAction.getReportHtmlContent(), StringUtils.isNotBlank(reportAction.getReportHtmlContent()));
    }

}
