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
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.protex.helper.TestBuildListener;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class ProtexJenkinsLoggerTest {

    private static PrintStream orgStream = null;

    private static PrintStream orgErrStream = null;

    private ByteArrayOutputStream systemOutput = null;

    private static PrintStream currStream = null;

    private ByteArrayOutputStream listenerOutput = null;

    private static PrintStream listenerStream = null;

    private TestBuildListener listener = null;

    private ProtexJenkinsLogger logger = null;

    @BeforeClass
    public static void init() {
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
        systemOutput = new ByteArrayOutputStream();
        currStream = new PrintStream(systemOutput);
        System.setOut(currStream);
        System.setErr(currStream);

        listenerOutput = new ByteArrayOutputStream();
        listenerStream = new PrintStream(listenerOutput);
        listener = new TestBuildListener(listenerStream);
        logger = new ProtexJenkinsLogger(listener);
    }

    @After
    public void testTearDown() throws Exception {
        System.setOut(orgStream);
        System.setErr(orgErrStream);
        currStream.close();
        currStream = null;

        systemOutput.close();
        systemOutput = null;
        listenerOutput.close();
        listenerOutput = null;
        listenerStream.close();
        listenerStream = null;
        listener = null;
        logger = null;
    }

    @Test
    public void testSetLogLevel() {
        assertEquals(LogLevel.INFO, logger.getLogLevel());
        logger.setLogLevel(LogLevel.WARN);
        assertEquals(LogLevel.WARN, logger.getLogLevel());
        logger.setLogLevel(LogLevel.ERROR);
        assertEquals(LogLevel.ERROR, logger.getLogLevel());
        logger.setLogLevel(LogLevel.TRACE);
        assertEquals(LogLevel.TRACE, logger.getLogLevel());
        logger.setLogLevel(LogLevel.INFO);
        assertEquals(LogLevel.INFO, logger.getLogLevel());
    }

    @Test
    public void testDebugLoggingIgnored() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.INFO);
        logger.debug("DEBUG MESSAGE");
        assertTrue(listenerOutput.size() == 0);
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testDebugLogging() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.DEBUG);
        logger.debug("DEBUG MESSAGE");
        assertEquals("[DEBUG] DEBUG MESSAGE", listenerOutput.toString().trim());
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testDebugLoggingNullListener() {
        logger = new ProtexJenkinsLogger(null);
        logger.setLogLevel(LogLevel.DEBUG);
        logger.debug("DEBUG MESSAGE");
        assertEquals("[DEBUG] DEBUG MESSAGE", systemOutput.toString().trim());
        assertTrue(listenerOutput.size() == 0);
    }

    @Test
    public void testErrorExceptionLogging() throws Exception {
        logger = new ProtexJenkinsLogger(listener);
        logger.error(new Exception("THIS IS AN EXCEPTION"));

        String output = listenerOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(systemOutput.size() == 0);
        assertEquals("[ERROR] java.lang.Exception: THIS IS AN EXCEPTION", split[0]);
    }

    @Test
    public void testErrorExceptionLoggingNullListener() throws Exception {
        logger = new ProtexJenkinsLogger(null);
        logger.error(new Exception("THIS IS AN EXCEPTION"));

        String output = systemOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(listenerOutput.size() == 0);
        assertEquals("[ERROR] java.lang.Exception: THIS IS AN EXCEPTION", split[0]);
    }

    @Test
    public void testErrorMessageLogging() throws Exception {
        logger = new ProtexJenkinsLogger(listener);
        logger.error("THIS IS AN ERROR");

        assertEquals("[ERROR] THIS IS AN ERROR", listenerOutput.toString().trim());
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testErrorMessageLoggingNullListener() throws Exception {
        logger = new ProtexJenkinsLogger(null);
        logger.error("THIS IS AN ERROR");

        assertEquals("[ERROR] THIS IS AN ERROR", systemOutput.toString().trim());
        assertTrue(listenerOutput.size() == 0);
    }

    @Test
    public void testErrorExceptionWithMessageLogging() throws Exception {
        logger = new ProtexJenkinsLogger(listener);
        logger.error("ERROR MESSAGE", new Exception("THIS IS AN EXCEPTION"));

        String output = listenerOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(systemOutput.size() == 0);
        assertEquals("[ERROR] ERROR MESSAGE", split[0]);
        assertEquals("[ERROR] java.lang.Exception: THIS IS AN EXCEPTION", split[1]);
    }

    @Test
    public void testErrorExceptionWithMessageLoggingNullListener() throws Exception {
        logger = new ProtexJenkinsLogger(null);
        logger.error("ERROR MESSAGE", new Exception("THIS IS AN EXCEPTION"));

        String output = systemOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(listenerOutput.size() == 0);
        assertEquals("[ERROR] ERROR MESSAGE", split[0]);
        assertEquals("[ERROR] java.lang.Exception: THIS IS AN EXCEPTION", split[1]);
    }

    @Test
    public void testInfoLoggingIgnored() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.ERROR);
        logger.info("INFO MESSAGE");
        assertTrue(listenerOutput.size() == 0);
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testInfoLogging() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.INFO);
        logger.info("INFO MESSAGE");
        assertEquals("[INFO] INFO MESSAGE", listenerOutput.toString().trim());
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testInfoLoggingNullListener() {
        logger = new ProtexJenkinsLogger(null);
        logger.setLogLevel(LogLevel.INFO);
        logger.info("INFO MESSAGE");
        assertEquals("[INFO] INFO MESSAGE", systemOutput.toString().trim());
        assertTrue(listenerOutput.size() == 0);
    }

    @Test
    public void testTraceLoggingIgnored() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.ERROR);
        logger.trace("TRACE MESSAGE");
        assertTrue(listenerOutput.size() == 0);
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testTraceLogging() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.TRACE);
        logger.trace("TRACE MESSAGE");
        assertEquals("[TRACE] TRACE MESSAGE", listenerOutput.toString().trim());
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testTraceLoggingNullListener() {
        logger = new ProtexJenkinsLogger(null);
        logger.setLogLevel(LogLevel.TRACE);
        logger.trace("TRACE MESSAGE");
        assertEquals("[TRACE] TRACE MESSAGE", systemOutput.toString().trim());
        assertTrue(listenerOutput.size() == 0);
    }

    @Test
    public void testTraceMessageWithExceptionLoggingIgnored() throws Exception {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.ERROR);
        logger.trace("TRACE MESSAGE", new Exception("THIS IS AN EXCEPTION"));

        assertTrue(listenerOutput.size() == 0);
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testTraceMessageWithExceptionLogging() throws Exception {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.TRACE);
        logger.trace("TRACE MESSAGE", new Exception("THIS IS AN EXCEPTION"));

        String output = listenerOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(systemOutput.size() == 0);
        assertEquals("[TRACE] TRACE MESSAGE", split[0]);
        assertEquals("[TRACE] java.lang.Exception: THIS IS AN EXCEPTION", split[1]);
    }

    @Test
    public void testTraceMessageWithExceptionLoggingNullListener() throws Exception {
        logger = new ProtexJenkinsLogger(null);
        logger.setLogLevel(LogLevel.TRACE);
        logger.trace("TRACE MESSAGE", new Exception("THIS IS AN EXCEPTION"));

        String output = systemOutput.toString("UTF-8");
        String split[] = null;
        split = output.split("\\n");
        assertTrue(split.length > 1);
        assertTrue(listenerOutput.size() == 0);
        assertEquals("[TRACE] TRACE MESSAGE", split[0]);
        assertEquals("[TRACE] java.lang.Exception: THIS IS AN EXCEPTION", split[1]);
    }

    @Test
    public void testWarningLoggingIgnored() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.ERROR);
        logger.warn("WARN MESSAGE");
        assertTrue(listenerOutput.size() == 0);
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testWarningLogging() {
        logger = new ProtexJenkinsLogger(listener);
        logger.setLogLevel(LogLevel.WARN);
        logger.warn("WARN MESSAGE");
        assertEquals("[WARN] WARN MESSAGE", listenerOutput.toString().trim());
        assertTrue(systemOutput.size() == 0);
    }

    @Test
    public void testWarningLoggingNullListener() {
        logger = new ProtexJenkinsLogger(null);
        logger.setLogLevel(LogLevel.WARN);
        logger.warn("WARN MESSAGE");
        assertEquals("[WARN] WARN MESSAGE", systemOutput.toString().trim());
        assertTrue(listenerOutput.size() == 0);
    }
}
