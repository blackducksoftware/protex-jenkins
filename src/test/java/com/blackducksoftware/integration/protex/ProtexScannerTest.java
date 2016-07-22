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
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URL;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.protex.helper.TestHelper;
import com.blackducksoftware.integration.protex.jenkins.ProtexJenkinsLogger;
import com.blackducksoftware.integration.protex.jenkins.ProtexScanner;
import com.blackducksoftware.protex.plugin.ProtexServer;

public class ProtexScannerTest {

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private static Properties testProperties;

    private static File passwordFile;

    private static PrintStream orgStream = null;

    private static PrintStream orgErrStream = null;

    private ByteArrayOutputStream byteOutput = null;

    private static PrintStream currStream = null;

    private static TestHelper helper = null;

    private static File testWorkspace = new File("testWorkspace");

    @BeforeClass
    public static void init() throws Exception {
        testProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("test.properties");

        URL passFileResourceUrl = classLoader.getResource("encryptedPasswordFile.txt");
        passwordFile = new File(passFileResourceUrl.toURI());
        ;

        try {
            testProperties.load(is);

        } catch (IOException e) {
            System.err.println("reading test.properties failed!");
        }
        orgStream = System.out;
        orgErrStream = System.err;

        helper = new TestHelper(testProperties.getProperty("TEST_PROTEX_SERVER_URL"), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));

        TestBuildListener listener = new TestBuildListener(orgStream);
        helper.setLogger(new ProtexJenkinsLogger(listener));

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

    @Test
    public void testFacadeRunProtexScanProjectNullProtexServer() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to provide the ProtexServer that the scan will use to connect to the server.");

        ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
        TestBuildListener listener = new TestBuildListener(currStream);
        scanner.setLogger(new ProtexJenkinsLogger(listener));

        scanner.runProtexScan(null, "fakeId", "fakeSourcePath", false);
    }

    @Test
    public void testFacadeRunProtexScanProjectNullId() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to provide the Id of the Protex Project to scan.");

        ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
        server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
        server.setUsername(testProperties.getProperty("TEST_USERNAME"));

        ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
        TestBuildListener listener = new TestBuildListener(currStream);
        scanner.setLogger(new ProtexJenkinsLogger(listener));

        scanner.runProtexScan(server, null, "fakeSourcePath", false);
    }

    @Test
    public void testFacadeRunProtexScanProjectNullSourcePath() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to provide the source path to be scanned.");

        ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
        server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
        server.setUsername(testProperties.getProperty("TEST_USERNAME"));

        ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
        TestBuildListener listener = new TestBuildListener(currStream);
        scanner.setLogger(new ProtexJenkinsLogger(listener));

        scanner.runProtexScan(server, "fakeId", null, false);
    }

    @Test
    public void testFacadeRunProtexScanProjectEmptyId() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to provide the Id of the Protex Project to scan.");

        ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
        server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
        server.setUsername(testProperties.getProperty("TEST_USERNAME"));

        ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
        TestBuildListener listener = new TestBuildListener(currStream);
        scanner.setLogger(new ProtexJenkinsLogger(listener));

        scanner.runProtexScan(server, "", "fakeSourcePath", false);
    }

    @Test
    public void testFacadeRunProtexScanProjectEmptySourcePath() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to provide the source path to be scanned.");

        ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
        server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
        server.setUsername(testProperties.getProperty("TEST_USERNAME"));

        ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
        TestBuildListener listener = new TestBuildListener(currStream);
        scanner.setLogger(new ProtexJenkinsLogger(listener));

        scanner.runProtexScan(server, "fakeId", "", false);
    }

    @Test
    public void testFacadeRunProtexScanProjectNullUrl() throws Exception {
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Need to set the Url to connect to in the ProtexServer object");

        if (testWorkspace.exists()) {

            ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
            server.setServerUrl(null);
            server.setUsername(testProperties.getProperty("TEST_USERNAME"));

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            TestBuildListener listener = new TestBuildListener(currStream);
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
            ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
            server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
            server.setUsername(null);

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            TestBuildListener listener = new TestBuildListener(currStream);
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
            ProtexServer server = new ProtexServer("");
            server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
            server.setUsername(testProperties.getProperty("TEST_USERNAME"));

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            TestBuildListener listener = new TestBuildListener(currStream);
            scanner.setLogger(new ProtexJenkinsLogger(listener));

            scanner.runProtexScan(server, "fakeId", testWorkspace.getCanonicalPath(), false);
        } else {
            Assert.fail();
        }
    }

    @Test
    public void testFacadeRunProtexScanProject() throws Exception {
        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexFacade facade = new ProtexFacade(testProperties.getProperty("TEST_PROTEX_SERVER_URL"), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        facade.setLogger(new ProtexJenkinsLogger(listener));
        if (testWorkspace.exists()) {
            String id = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));
            String hostName = InetAddress.getLocalHost().getHostName();
            facade.protexPrepScanProject(id, hostName, testWorkspace.getCanonicalPath());

            ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
            server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
            server.setUsername(testProperties.getProperty("TEST_USERNAME"));

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            scanner.setLogger(new ProtexJenkinsLogger(listener));

            scanner.runProtexScan(server, id, testWorkspace.getCanonicalPath(), false);
        } else {
            Assert.fail();
        }

        String line = new String(byteOutput.toByteArray());
        String[] split = line.split(System.getProperty("line.separator"));
        Assert.assertTrue(line, line.contains("[INFO] Running Scan analyze command on"));
        Assert.assertTrue(line, line.contains("[INFO] End Of Log Scan"));
        Assert.assertTrue(line, line.contains("[INFO] Project files"));
        Assert.assertTrue(split.length > 4);
    }

    @Test
    public void testFacadeRunProtexScanProjectWithObserver() throws Exception {
        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexFacade facade = new ProtexFacade(testProperties.getProperty("TEST_PROTEX_SERVER_URL"), testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        facade.setLogger(new ProtexJenkinsLogger(listener));
        if (testWorkspace.exists()) {
            String id = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));
            String hostName = InetAddress.getLocalHost().getHostName();
            facade.protexPrepScanProject(id, hostName, testWorkspace.getCanonicalPath());

            ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
            server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
            server.setUsername(testProperties.getProperty("TEST_USERNAME"));

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            scanner.setLogger(new ProtexJenkinsLogger(listener));

            scanner.runProtexScan(server, id, testWorkspace.getCanonicalPath(), true);
        } else {
            Assert.fail();
        }

        String line = new String(byteOutput.toByteArray());
        String[] split = line.split(System.getProperty("line.separator"));
        Assert.assertTrue(line, line.contains("[INFO] Running Scan analyze command on"));
        Assert.assertTrue(line, line.contains("[INFO] End Of Log Scan")); // Second to last string in the
                                                                          // array
        Assert.assertTrue(line, line.contains("[INFO] Project files")); // Last string in the array
        Assert.assertTrue(split.length > 4);
    }

    @Test
    public void testFacadeRunProtexScanProjectThroughPassThroughProxy() throws Exception {
        TestBuildListener listener = new TestBuildListener(currStream);
        ProtexFacade facade = new ProtexFacade(testProperties.getProperty("TEST_PROTEX_SERVER_URL_REQUIRES_PROXY"),
                testProperties.getProperty("TEST_USERNAME"),
                testProperties.getProperty("TEST_PASSWORD"));
        facade.setLogger(new ProtexJenkinsLogger(listener));
        String proxyHost = testProperties.getProperty("TEST_PROXY_HOST_PASSTHROUGH");
        int proxyPort = Integer.valueOf(testProperties
                .getProperty("TEST_PROXY_PORT_PASSTHROUGH"));

        facade.setProxySettings(proxyHost, proxyPort, ProxyServerType.HTTP, true);

        if (testWorkspace.exists()) {
            String id = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));
            String hostName = InetAddress.getLocalHost().getHostName();
            facade.protexPrepScanProject(id, hostName, testWorkspace.getCanonicalPath());

            ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
            server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
            server.setUsername(testProperties.getProperty("TEST_USERNAME"));

            InetSocketAddress address = new InetSocketAddress(proxyHost, proxyPort);
            server.setProxy(new Proxy(Type.HTTP, address));

            ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
            scanner.setLogger(new ProtexJenkinsLogger(listener));

            scanner.runProtexScan(server, id, testWorkspace.getCanonicalPath(), true);
        } else {
            Assert.fail();
        }

        String line = new String(byteOutput.toByteArray());
        String[] split = line.split(System.getProperty("line.separator"));
        Assert.assertTrue(line, line.contains("[INFO] Running Scan analyze command on"));
        Assert.assertTrue(line, line.contains("[INFO] End Of Log Scan")); // Second to last string in the
                                                                          // array
        Assert.assertTrue(line, line.contains("[INFO] Project files")); // Last string in the array
        Assert.assertTrue(split.length > 4);
    }

    @Test
    public void testFacadeRunProtexScanProjectThroughBasicProxy() throws Exception {
        try {
            TestBuildListener listener = new TestBuildListener(currStream);

            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                    testProperties.getProperty("TEST_PROXY_USER_BASIC"), testProperties.getProperty("TEST_PROXY_PASSWORD_BASIC").toCharArray());
                        }
                    }
                    );

            ProtexFacade facade = new ProtexFacade(testProperties.getProperty("TEST_PROTEX_SERVER_URL_REQUIRES_PROXY"),
                    testProperties.getProperty("TEST_USERNAME"),
                    testProperties.getProperty("TEST_PASSWORD"));
            facade.setLogger(new ProtexJenkinsLogger(listener));
            String proxyHost = testProperties.getProperty("TEST_PROXY_HOST_BASIC");
            int proxyPort = Integer.valueOf(testProperties
                    .getProperty("TEST_PROXY_PORT_BASIC"));

            facade.setProxySettings(proxyHost, proxyPort, ProxyServerType.HTTP, true,
                    testProperties.getProperty("TEST_PROXY_USER_BASIC"),
                    testProperties.getProperty("TEST_PROXY_PASSWORD_BASIC"));

            if (testWorkspace.exists()) {
                String id = facade.getProtexProjectId(testProperties.getProperty("TEST_PROJECT_NAME"));
                String hostName = InetAddress.getLocalHost().getHostName();
                facade.protexPrepScanProject(id, hostName, testWorkspace.getCanonicalPath());

                ProtexServer server = new ProtexServer(testProperties.getProperty("TEST_PASSWORD"));
                server.setServerUrl(testProperties.getProperty("TEST_PROTEX_SERVER_URL"));
                server.setUsername(testProperties.getProperty("TEST_USERNAME"));

                InetSocketAddress address = new InetSocketAddress(proxyHost, proxyPort);
                server.setProxy(new Proxy(Type.HTTP, address));

                ProtexScanner scanner = new ProtexScanner(null, null, null, null, null, null);
                scanner.setLogger(new ProtexJenkinsLogger(listener));

                scanner.runProtexScan(server, id, testWorkspace.getCanonicalPath(), true);
            } else {
                Assert.fail();
            }

            String line = new String(byteOutput.toByteArray());
            String[] split = line.split(System.getProperty("line.separator"));
            Assert.assertTrue(line, line.contains("[INFO] Running Scan analyze command on"));
            Assert.assertTrue(line, line.contains("[INFO] End Of Log Scan")); // Second to last string in the
                                                                              // array
            Assert.assertTrue(line, line.contains("[INFO] Project files")); // Last string in the array
            Assert.assertTrue(split.length > 4);
        } finally {
            String line = new String(byteOutput.toByteArray());
        }
    }
}
