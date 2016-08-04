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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.UUID;

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
import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.protex.helper.TestHelper;
import com.blackducksoftware.integration.protex.jenkins.action.ScanRunAction;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;

import hudson.model.AbstractBuild;
import hudson.model.Action;
import hudson.model.BuildListener;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.tasks.BuildStepMonitor;

public class ProtexFailureStepTest {
	private static PrintStream orgStream = null;

	private static PrintStream orgErrStream = null;

	private static PrintStream currStream = null;

	private ByteArrayOutputStream byteOutput = null;

	@Rule
	public JenkinsRule j = new JenkinsRule();

	@BeforeClass
	public static void init() throws Exception {
		orgStream = System.out;
		orgErrStream = System.err;
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

	private ProtexFacade getMockedFacade(final int pendingIds, final int licenseViolations) throws Exception {
		final ProtexFacade mockedFacade = Mockito.mock(ProtexFacade.class);

		Mockito.doAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(final InvocationOnMock invocation) {
				return pendingIds;
			}
		}).when(mockedFacade).getPendingIds(Mockito.anyString());

		Mockito.doAnswer(new Answer<Integer>() {
			@Override
			public Integer answer(final InvocationOnMock invocation) {
				return licenseViolations;
			}
		}).when(mockedFacade).getViolationCount(Mockito.anyString());

		return mockedFacade;
	}

	private AbstractBuild getMockedAbstractBuild(final Result buildResult, final PostBuildProtexScan protexScan, final boolean scanRun, final ProtexFailureStep failureStep)
			throws Exception {

		final AbstractBuild build = Mockito.mock(AbstractBuild.class);

		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(final InvocationOnMock invocation) {
				return null;
			}
		}).when(build).setResult(Mockito.any(Result.class));

		Mockito.when(build.getResult()).thenReturn(buildResult);

		final FreeStyleProject proj = j.createFreeStyleProject("testing");
		if (scanRun && protexScan != null) {
			// Run Scan during build
			proj.getPublishersList().add(protexScan);

			Mockito.doAnswer(new Answer<Action>() {
				@Override
				public Action answer(final InvocationOnMock invocation) {
					return new ScanRunAction();
				}
			}).when(build).getAction(ScanRunAction.class);
		}
		if (failureStep != null) {
			// Add failure step
			proj.getPublishersList().add(failureStep);
		}

		final AbstractBuild realBuild = proj.scheduleBuild2(0).get();
		if (!scanRun && protexScan != null) {
			// Add Scan after build
			// Simulating the protexScan was configured to run after the Failure Conditions
			proj.getPublishersList().add(protexScan);
		}

		final TestBuildListener listener = new TestBuildListener(currStream);
		Mockito.when(build.getEnvironment(Mockito.any(BuildListener.class))).thenReturn(realBuild.getEnvironment(listener));

		Mockito.doAnswer(new Answer<FreeStyleProject>() {
			@Override
			public FreeStyleProject answer(final InvocationOnMock invocation) {
				return proj;
			}
		}).when(build).getProject();

		return build;
	}

	@WithoutJenkins
	@Test
	public void testGetRequiredMonitorService() {
		final ProtexFailureStep failureStep = new ProtexFailureStep(false, false);
		assertEquals(BuildStepMonitor.NONE, failureStep.getRequiredMonitorService());
	}

	@Test
	public void testProtexFailureStepCreationFalse() {
		final ProtexFailureStep failureStep = new ProtexFailureStep(false, false);
		assertFalse(failureStep.getBuildFailOnPendingIDPost());
		assertFalse(failureStep.getBuildFailOnLicenseViolationPost());
		assertNotNull(failureStep.getDescriptor());
	}

	@Test
	public void testProtexFailureStepCreationTrue() {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		assertTrue(failureStep.getBuildFailOnPendingIDPost());
		assertTrue(failureStep.getBuildFailOnLicenseViolationPost());
		assertNotNull(failureStep.getDescriptor());
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditionsPendingIdsWithoutFailureCondition() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(false, true);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertTrue(failureStep.checkProtexFailConditions(getMockedFacade(10, 0), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(1, split.length);
		assertTrue(StringUtils.isEmpty(output));
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditionsPendingIds() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertFalse(failureStep.checkProtexFailConditions(getMockedFacade(10, 0), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(1, split.length);
		assertTrue(output.contains("Failing the Build because there are  : 10, File(s) Pending Id"));
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditionsLicenseViolationsWithoutFailureCondition() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, false);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertTrue(failureStep.checkProtexFailConditions(getMockedFacade(0, 10), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(1, split.length);
		assertTrue(StringUtils.isEmpty(output));
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditionsLicenseViolations() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertFalse(failureStep.checkProtexFailConditions(getMockedFacade(0, 10), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(1, split.length);
		assertTrue(output.contains("Failing the Build because there are  : 10, License Violation(s)"));
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditionsWithoutFailureCondition() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(false, false);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertTrue(failureStep.checkProtexFailConditions(getMockedFacade(10, 10), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(1, split.length);
		assertTrue(StringUtils.isEmpty(output));
	}

	@WithoutJenkins
	@Test
	public void testCheckProtexFailConditions() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		assertFalse(failureStep.checkProtexFailConditions(getMockedFacade(10, 10), logger, ""));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(2, split.length);
		assertTrue(output.contains("Failing the Build because there are  : 10, File(s) Pending Id"));
		assertTrue(output.contains("Failing the Build because there are  : 10, License Violation(s)"));
	}

	@Test
	public void testPerformBuildFailed() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);

		assertTrue(failureStep.perform(getMockedAbstractBuild(Result.FAILURE, null, false, failureStep), null, listener));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		assertEquals(2, split.length);
		assertTrue(output.contains("Build was not successful. Will not run Protex Failure Conditions."));
		assertTrue(output.contains("Finished running Protex Failure Conditions."));
	}

	@Test
	public void testPerformProtexScanNotPresent() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);

		assertTrue(failureStep.perform(getMockedAbstractBuild(Result.SUCCESS, null, false, failureStep), null, listener));

		final String output = byteOutput.toString("UTF-8");
		orgStream.println(output);
		String split[] = null;
		split = output.split("\\n");
		assertEquals(2, split.length);
		assertTrue(output.contains("The Protex scan needs to run before you can check the Failure Conditions."));
		assertTrue(output.contains("Please add the Protex Scan to the Job configuration."));
	}

	@Test
	public void testPerformProtexScanNotRun() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);

		final String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TEST_USERNAME",
				"TEST_PASSWORD");

		final String uuid = UUID.randomUUID().toString();

		assertTrue(failureStep
				.perform(
						getMockedAbstractBuild(Result.SUCCESS,
								new PostBuildProtexScan(uuid, validCredentials, "TEST_PROJECT_NAME", "", "", 2.0, null),
								false,
								failureStep),
						null,
						listener));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		orgStream.println(output);
		assertEquals(2, split.length);
		assertTrue(output.contains("The Protex scan needs to run before you can check the Failure Conditions."));
		assertTrue(output.contains("Please fix the order in the Job configuration."));
	}

	@Test
	public void testPerformProtexScanNotConfigured() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);

		assertTrue(failureStep.perform(
				getMockedAbstractBuild(Result.SUCCESS, new PostBuildProtexScan(null, null, null, null, null, 2.0, null), true, failureStep),
				null,
				listener));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		orgStream.println(output);
		assertEquals(2, split.length);
		assertTrue(output, output.contains("The Protex scan was not configured correctly."));
		assertTrue(output, output.contains("Please configure the Protex scan before running the Failure Conditions"));
	}

	@Test
	public void testPerformProtexScanMisconfigured() throws Exception {
		final ProtexFailureStep failureStep = new ProtexFailureStep(true, true);
		final TestBuildListener listener = new TestBuildListener(currStream);

		assertTrue(failureStep.perform(
				getMockedAbstractBuild(Result.SUCCESS, new PostBuildProtexScan("Fake id", "Fake id", "FakeProjectName", "", "", 2.0, null), true, failureStep),
				null, listener));

		final String output = byteOutput.toString("UTF-8");
		String split[] = null;
		split = output.split("\\n");
		orgStream.println(output);
		assertTrue(output, output.contains("There was a problem creating the ProtexFacade"));
	}

}
