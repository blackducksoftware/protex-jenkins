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
package com.blackducksoftware.integration.protex.helper;

import hudson.console.ConsoleNote;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.Cause;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.List;

public class TestBuildListener implements BuildListener {

    private static PrintStream stream = null;

    public TestBuildListener(PrintStream stream) {
        this.stream = stream;
    }

    public PrintWriter error(String txt) {
        if (txt != null) {
            stream.println(txt);
        }
        return null;
    }

    public PrintStream getLogger() {
        return stream;
    }

    public void annotate(ConsoleNote ann) throws IOException {
        // TODO Auto-generated function stub

    }

    public void hyperlink(String url, String text) throws IOException {
        // TODO Auto-generated function stub

    }

    public PrintWriter error(String format, Object... args) {
        // TODO Auto-generated function stub
        return null;
    }

    public PrintWriter fatalError(String msg) {
        // TODO Auto-generated function stub
        return null;
    }

    public PrintWriter fatalError(String format, Object... args) {
        // TODO Auto-generated function stub
        return null;
    }

    public void started(List<Cause> causes) {
        // TODO Auto-generated function stub

    }

    public void finished(Result result) {
        // TODO Auto-generated function stub

    }
}
