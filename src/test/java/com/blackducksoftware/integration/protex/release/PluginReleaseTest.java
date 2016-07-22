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
package com.blackducksoftware.integration.protex.release;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.junit.Test;

public class PluginReleaseTest {

    @Test
    public void testProperties() throws Exception {
        Properties pluginProperties = new Properties();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        InputStream is = classLoader.getResourceAsStream("updateSite.properties");
        pluginProperties.load(is);

        Properties releaseProperties = new Properties();
        File releasePropertiesFile = new File("deployment.properties");
        System.out.println(releasePropertiesFile.getCanonicalPath());
        InputStream inputStream = new FileInputStream(releasePropertiesFile);
        releaseProperties.load(inputStream);

        assertEquals(releaseProperties.getProperty("update.site.url"), pluginProperties.getProperty("protex.update.site.url"));
    }

}
