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
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class ProtexServerInfoTest {

    @Rule
    public JenkinsRule j = new JenkinsRule();

    @Test
    public void testProtexServerInfoCreationEmpty() {
        ProtexServerInfo serverInfo = new ProtexServerInfo(null);
        assertNull(serverInfo.getProtexPostServerName());
        assertNull(serverInfo.getProtexPostServerUrl());
        assertNull(serverInfo.getProtexPostServerTimeOut());
        assertNotNull(serverInfo.getProtexServerId());
        assertNotNull(serverInfo.getDescriptor());
    }

    @Test
    public void testProtexServerInfoCreation() {
        ProtexServerInfo serverInfo = new ProtexServerInfo("Test server", "http://Example", "999", null);
        assertEquals("Test server", serverInfo.getProtexPostServerName());
        assertEquals("http://Example", serverInfo.getProtexPostServerUrl());
        assertEquals("999", serverInfo.getProtexPostServerTimeOut());
        assertNotNull(serverInfo.getProtexServerId());
        assertNotNull(serverInfo.getDescriptor());
    }

    @Test
    public void testProtexServerInfoCreationSetValues() {
        ProtexServerInfo serverInfo = new ProtexServerInfo(null);
        serverInfo.setProtexPostServerName("Test server");
        serverInfo.setProtexPostServerUrl("http://Example");
        serverInfo.setProtexPostServerTimeOut("999");

        assertEquals("Test server", serverInfo.getProtexPostServerName());
        assertEquals("http://Example", serverInfo.getProtexPostServerUrl());
        assertEquals("999", serverInfo.getProtexPostServerTimeOut());
        assertNotNull(serverInfo.getProtexServerId());
        assertNotNull(serverInfo.getDescriptor());
    }

    @Test
    public void testProtexServerInfoToString() {
        ProtexServerInfo serverInfo = new ProtexServerInfo("Test server", "http://Example", "999", null);
        assertTrue(serverInfo.toString().contains(
                "ProtexServerInfo{protexPostServerName=Test server, protexPostServerUrl=http://Example, protexServerTimeout=999, protexServerId="));
    }
}
