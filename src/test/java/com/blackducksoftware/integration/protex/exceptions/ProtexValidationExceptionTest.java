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
package com.blackducksoftware.integration.protex.exceptions;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import com.blackducksoftware.integration.protex.jenkins.ProtexJenkinsLogger;

public class ProtexValidationExceptionTest {
    @Test
    public void testPrintSmallStackTrace_fiveStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(5));

        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream currentErr = System.err;
        try {
            System.setErr(mockedPrintStream);

            protexValidationException.printSmallStackTrace();
            Mockito.verify(mockedPrintStream, Mockito.times(6)).println(Mockito.anyString());

            Mockito.verifyNoMoreInteractions(mockedPrintStream);
        } finally {
            System.setErr(currentErr);
        }
    }

    @Test
    public void testPrintSmallStackTrace_tenStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(10));

        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream currentErr = System.err;
        try {
            System.setErr(mockedPrintStream);

            protexValidationException.printSmallStackTrace();
            Mockito.verify(mockedPrintStream, Mockito.times(11)).println(Mockito.anyString());

            Mockito.verifyNoMoreInteractions(mockedPrintStream);
        } finally {
            System.setErr(currentErr);
        }
    }

    @Test
    public void testPrintSmallStackTrace_twelveStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(12));

        PrintStream mockedPrintStream = Mockito.mock(PrintStream.class);
        PrintStream currentErr = System.err;
        try {
            System.setErr(mockedPrintStream);

            protexValidationException.printSmallStackTrace();
            Mockito.verify(mockedPrintStream, Mockito.times(11)).println(Mockito.anyString());

            Mockito.verifyNoMoreInteractions(mockedPrintStream);
        } finally {
            System.setErr(currentErr);
        }
    }

    @Test
    public void testPrintSmallStackTrace_withLogger_fiveStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(5));

        ProtexJenkinsLogger mockedProtexJenkinsLogger = Mockito.mock(ProtexJenkinsLogger.class);
        protexValidationException.printSmallStackTrace(mockedProtexJenkinsLogger);
        Mockito.verify(mockedProtexJenkinsLogger, Mockito.times(6)).error(Mockito.anyString());

        Mockito.verifyNoMoreInteractions(mockedProtexJenkinsLogger);
    }

    @Test
    public void testPrintSmallStackTrace_withLogger_tenStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(10));

        ProtexJenkinsLogger mockedProtexJenkinsLogger = Mockito.mock(ProtexJenkinsLogger.class);
        protexValidationException.printSmallStackTrace(mockedProtexJenkinsLogger);
        Mockito.verify(mockedProtexJenkinsLogger, Mockito.times(11)).error(Mockito.anyString());

        Mockito.verifyNoMoreInteractions(mockedProtexJenkinsLogger);
    }

    @Test
    public void testPrintSmallStackTrace_withLogger_twelveStackTraceElements() {
        ProtexValidationException protexValidationException = new ProtexValidationException();
        protexValidationException.setStackTrace(getStackTraceElements(12));

        ProtexJenkinsLogger mockedProtexJenkinsLogger = Mockito.mock(ProtexJenkinsLogger.class);
        protexValidationException.printSmallStackTrace(mockedProtexJenkinsLogger);
        Mockito.verify(mockedProtexJenkinsLogger, Mockito.times(11)).error(Mockito.anyString());

        Mockito.verifyNoMoreInteractions(mockedProtexJenkinsLogger);
    }

    private StackTraceElement[] getStackTraceElements(int totalElementCount) {
        List<StackTraceElement> stackTraceElements = new ArrayList<StackTraceElement>();

        for (int elementCount = 0; elementCount < totalElementCount; elementCount++) {
            String countString = Integer.toString(elementCount);
            stackTraceElements.add(new StackTraceElement("declaringClass" + countString, "methodName" + countString, "fileName" + countString, elementCount));
        }

        return stackTraceElements.toArray(new StackTraceElement[stackTraceElements.size()]);
    }

}
