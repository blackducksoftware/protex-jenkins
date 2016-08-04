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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import javax.servlet.ServletException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.jvnet.hudson.test.JenkinsRule;

import com.blackducksoftware.integration.protex.helper.TestHelper;
import com.blackducksoftware.integration.protex.jenkins.Messages;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;

import hudson.util.FormValidation;
import hudson.util.ListBoxModel;

public class ProtexServerInfoDescriptorTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Rule
	public JenkinsRule j = new JenkinsRule();


	@Test
	public void testDoCheckProtexPostServerName() throws Exception {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();
		assertEquals(Messages.ProtexPostScan_getPleaseSetServerName(), descriptor.doCheckProtexPostServerName("").getMessage());
	}

	@Test
	public void testDoCheckProtexPostServerUrl() throws Exception {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();
		assertEquals(Messages.ProtexPostScan_getPleaseSetServer(), descriptor.doCheckProtexPostServerUrl("").getMessage());
		assertEquals(Messages.ProtexPostScan_getNotAValidUrl(), descriptor.doCheckProtexPostServerUrl("123").getMessage());
		assertEquals(Messages.ProtexPostScan_getNotAValidUrl(), descriptor.doCheckProtexPostServerUrl("http:// ").getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckProtexPostServerUrl("http://example.com/"));
		assertEquals(Messages.ProtexPostScan_getCanNotReachThisServer_0_("java.net.UnknownHostException: HopefullyNobodyHasReservedThisReallyLongDomain.com"),
				descriptor.doCheckProtexPostServerUrl("http://HopefullyNobodyHasReservedThisReallyLongDomain.com").getMessage());

	}


	@Test
	public void testDoFillProtexTestCredentialsIdItems() {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();
		ListBoxModel dropDownList = descriptor.doFillProtexTestCredentialsIdItems();
		assertNotNull(dropDownList);
		assertEquals("- none -", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		String credentialId = null;
		try {
			credentialId = TestHelper.addCredentialsToStore(new UserFacingAction(), "TEST_USERNAME", "TEST_PASSWORD");
		} catch (final IOException e) {
			e.printStackTrace();
			assertNull(e);
		}
		dropDownList = descriptor.doFillProtexTestCredentialsIdItems();
		assertEquals("- none -", dropDownList.get(0).name);
		assertEquals("", dropDownList.get(0).value);
		final String credentials = dropDownList.get(1).value;
		assertEquals(credentialId, credentials);
	}

	@Test
	public void testDoCheckProtexPostServerTimeOut() throws IOException, ServletException {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();
		assertEquals(Messages.ProtexPostScan_getPleaseSetTimeout(), descriptor.doCheckProtexPostServerTimeOut("").getMessage());
		assertEquals(Messages.ProtexPostScan_getTimeoutMustBeInteger(), descriptor.doCheckProtexPostServerTimeOut("timeout").getMessage());
		assertEquals(Messages.ProtexPostScan_getTimeoutGreaterThanOne(), descriptor.doCheckProtexPostServerTimeOut("0").getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckProtexPostServerTimeOut(ProtexServerInfoDescriptor.DEFAULT_TIMEOUT));
	}

	@Test
	public void testDoCheckProtexTestCredentialsId() throws IOException, ServletException {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();

		final String noUserName = TestHelper.addCredentialsToStore(new UserFacingAction(), "", "TEST_PASSWORD");
		final String noPassword = TestHelper.addCredentialsToStore(new UserFacingAction(), "TEST_USERNAME", "");
		final String noCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "", "");
		final String validCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "TEST_USERNAME",
				"TEST_PASSWORD");

		assertEquals(FormValidation.ok(), descriptor.doCheckProtexTestCredentialsId(""));
		assertEquals(Messages.ProtexPostScan_getNoUserNameProvided(), descriptor.doCheckProtexTestCredentialsId(noUserName).getMessage());
		assertEquals(Messages.ProtexPostScan_getNoPasswordProvided(), descriptor.doCheckProtexTestCredentialsId(noPassword).getMessage());
		assertEquals(Messages.ProtexPostScan_getNoCredentialsSelected(), descriptor.doCheckProtexTestCredentialsId(noCredentials).getMessage());
		assertEquals(FormValidation.ok(), descriptor.doCheckProtexTestCredentialsId(validCredentials));

	}

	@Test
	public void testDoTestConnection() throws IOException {
		final ProtexServerInfoDescriptor descriptor = new ProtexServerInfoDescriptor();

		final String noUserName = TestHelper.addCredentialsToStore(new UserFacingAction(), "", "TEST_PASSWORD");
		final String noPassword = TestHelper.addCredentialsToStore(new UserFacingAction(), "TEST_USERNAME", "");
		final String noCredentials = TestHelper.addCredentialsToStore(new UserFacingAction(), "", "");

		FormValidation result = null;

		result = descriptor.doTestConnection("", "", null);
		assertEquals(FormValidation.Kind.ERROR, result.kind);
		assertEquals(Messages.ProtexPostScan_getPleaseSetServer(), result.getMessage());

		result = descriptor.doTestConnection("https://www.google.com", "", null);
		assertEquals(FormValidation.Kind.ERROR, result.kind);
		assertEquals(Messages.ProtexPostScan_getNoCredentialsSelected(), result.getMessage());

		result = descriptor.doTestConnection("https://www.google.com", noUserName, null);
		assertEquals(FormValidation.Kind.ERROR, result.kind);
		assertEquals(Messages.ProtexPostScan_getNoUserNameProvided(), result.getMessage());

		result = descriptor.doTestConnection("https://www.google.com", noPassword, null);
		assertEquals(FormValidation.Kind.ERROR, result.kind);
		assertEquals(Messages.ProtexPostScan_getNoPasswordProvided(), result.getMessage());

		result = descriptor.doTestConnection("https://www.google.com", noCredentials, null);
		assertEquals(FormValidation.Kind.ERROR, result.kind);
		assertEquals(Messages.ProtexPostScan_getNoCredentialsSelected(), result.getMessage());

	}
}
