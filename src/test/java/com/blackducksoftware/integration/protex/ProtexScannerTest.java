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
package com.blackducksoftware.integration.protex;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.protex.jenkins.ProtexJenkinsLogger;
import com.blackducksoftware.integration.protex.jenkins.ProtexScanner;
import com.blackducksoftware.protex.plugin.ProtexServer;

import junit.framework.Assert;

public class ProtexScannerTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	private static PrintStream orgStream = null;

	private static PrintStream orgErrStream = null;

	private ByteArrayOutputStream byteOutput = null;

	private static PrintStream currStream = null;

	private static File testWorkspace = new File("testWorkspace");

	@BeforeClass
	public static void init() throws Exception {
		final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

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

	@Test
	public void testFacadeRunProtexScanProjectNullProtexServer() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to provide the ProtexServer that the scan will use to connect to the server.");

		final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
		final TestBuildListener listener = new TestBuildListener(currStream);
		scanner.setLogger(new ProtexJenkinsLogger(listener));

		scanner.runProtexScan(null, "fakeId", "fakeSourcePath", false);
	}

	@Test
	public void testFacadeRunProtexScanProjectNullId() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to provide the Id of the Protex Project to scan.");

		final ProtexServer server = new ProtexServer("FakePassword");
		server.setServerUrl("https://www.google.com");
		server.setUsername("FakeUser");

		final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
		final TestBuildListener listener = new TestBuildListener(currStream);
		scanner.setLogger(new ProtexJenkinsLogger(listener));

		scanner.runProtexScan(server, null, "fakeSourcePath", false);
	}

	@Test
	public void testFacadeRunProtexScanProjectNullSourcePath() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to provide the source path to be scanned.");

		final ProtexServer server = new ProtexServer("FakePassword");
		server.setServerUrl("https://www.google.com");
		server.setUsername("FakeUser");

		final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
		final TestBuildListener listener = new TestBuildListener(currStream);
		scanner.setLogger(new ProtexJenkinsLogger(listener));

		scanner.runProtexScan(server, "fakeId", null, false);
	}

	@Test
	public void testFacadeRunProtexScanProjectEmptyId() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to provide the Id of the Protex Project to scan.");

		final ProtexServer server = new ProtexServer("FakePassword");
		server.setServerUrl("https://www.google.com");
		server.setUsername("FakeUser");

		final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
		final TestBuildListener listener = new TestBuildListener(currStream);
		scanner.setLogger(new ProtexJenkinsLogger(listener));

		scanner.runProtexScan(server, "", "fakeSourcePath", false);
	}

	@Test
	public void testFacadeRunProtexScanProjectEmptySourcePath() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to provide the source path to be scanned.");

		final ProtexServer server = new ProtexServer("FakePassword");
		server.setServerUrl("https://www.google.com");
		server.setUsername("FakeUser");

		final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
		final TestBuildListener listener = new TestBuildListener(currStream);
		scanner.setLogger(new ProtexJenkinsLogger(listener));

		scanner.runProtexScan(server, "fakeId", "", false);
	}

	@Test
	public void testFacadeRunProtexScanProjectNullUrl() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to set the Url to connect to in the ProtexServer object");

		if (testWorkspace.exists()) {

			final ProtexServer server = new ProtexServer("FakePassword");
			server.setServerUrl(null);
			server.setUsername("FakeUser");

			final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
			final TestBuildListener listener = new TestBuildListener(currStream);
			scanner.setLogger(new ProtexJenkinsLogger(listener));

			scanner.runProtexScan(server, "fakeId", testWorkspace.getCanonicalPath(), false);
		} else {
			Assert.fail();
		}
	}

	@Test
	public void testFacadeRunProtexScanProjectNullUserName() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to set the UserName in the ProtexServer object");

		if (testWorkspace.exists()) {
			final ProtexServer server = new ProtexServer("FakePassword");
			server.setServerUrl("https://www.google.com");
			server.setUsername(null);

			final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
			final TestBuildListener listener = new TestBuildListener(currStream);
			scanner.setLogger(new ProtexJenkinsLogger(listener));

			scanner.runProtexScan(server, "fakeId", testWorkspace.getCanonicalPath(), false);
		} else {
			Assert.fail();
		}
	}

	@Test
	public void testFacadeRunProtexScanProjectEmptyPassword() throws Exception {
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Need to set the Password in the ProtexServer object");

		if (testWorkspace.exists()) {
			final ProtexServer server = new ProtexServer("");
			server.setServerUrl("https://www.google.com");
			server.setUsername("FakeUser");

			final ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
			final TestBuildListener listener = new TestBuildListener(currStream);
			scanner.setLogger(new ProtexJenkinsLogger(listener));

			scanner.runProtexScan(server, "fakeId", testWorkspace.getCanonicalPath(), false);
		} else {
			Assert.fail();
		}
	}

}
