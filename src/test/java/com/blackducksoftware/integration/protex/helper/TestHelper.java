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

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.protex.ProtexFacade;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.Project;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.UserFacingAction;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class TestHelper extends ProtexFacade {

    private IntLogger logger;

    public TestHelper(String serverUrl, String username, String password) throws InvalidKeyException,
            NoSuchAlgorithmException, NoSuchPaddingException,
            IllegalBlockSizeException, BadPaddingException, IOException, ServerConfigException {
        super(serverUrl, username, password, 300L);
    }

    @Override
    public void setLogger(IntLogger logger) {
        this.logger = logger;
        super.setLogger(logger);
    }

    public void deleteProject(String projectName) throws Exception {
        if (StringUtils.isEmpty(projectName)) {
            throw new IllegalArgumentException(
                    "Need to provide a name for the Protex Project to be deleted.");
        }

        try {
            if (!checkProjectExists(projectName)) {
                throw new Exception("Error while deleting project : The project '" + projectName + "' does not exist.");
            }
            Project project = serverProxy.getProjectApi().getProjectByName(projectName);
            serverProxy.getProjectApi().deleteProject(project.getProjectId());
        } catch (SdkFault e) {
            if (e.getFaultInfo() != null && e.getFaultInfo().getErrorCode() != null) {
                logger.error(e.getFaultInfo().getErrorCode().toString(), e);
                throw new Exception("Error while deleting project :" + e.getFaultInfo().getErrorCode().toString(), e);
            } else {
                logger.error(e.getMessage(), e);
                throw new Exception("Error while deleting project :" + e.getMessage(), e);
            }
        } catch (ServerConnectionException e) {
            throw e;
        }
    }

    public static String addCredentialsToStore(UserFacingAction action, String userName, String password) throws IOException {
        UsernamePasswordCredentialsImpl credential = new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, null, null, userName, password);
        action.getStore().addCredentials(Domain.global(), credential);
        return credential.getId();
    }

}
