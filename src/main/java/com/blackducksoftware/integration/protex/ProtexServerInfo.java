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

import hudson.model.AbstractDescribableImpl;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;

import com.google.common.base.Objects;

public class ProtexServerInfo extends AbstractDescribableImpl<ProtexServerInfo> {

    private String protexPostServerName;

    private String protexPostServerUrl;

    private String protexPostServerTimeOut;

    private String protexServerId;

    /**
     *
     * @param protexServerId
     *            Required UUID to identify the server by. If null one will be randomly generated
     */
    public ProtexServerInfo(String protexServerId) {
        this(null, null, null, protexServerId);
    }

    /**
     *
     * @param protexPostServerName
     * @param protexPostServerUrl
     * @param protexPostServerTimeOut
     * @param protexServerId
     *            Required UUID to identify the server by. If null one will be randomly generated
     */
    @DataBoundConstructor
    public ProtexServerInfo(String protexPostServerName, String protexPostServerUrl, String protexPostServerTimeOut, String protexServerId) {
        this.protexPostServerName = protexPostServerName;
        this.protexPostServerUrl = protexPostServerUrl;
        this.protexPostServerTimeOut = protexPostServerTimeOut;
        if (StringUtils.isEmpty(protexServerId)) {
            this.protexServerId = UUID.randomUUID().toString();
        } else {
            this.protexServerId = protexServerId;
        }
    }

    public String getProtexPostServerName() {
        return protexPostServerName;
    }

    public String getProtexPostServerUrl() {
        return protexPostServerUrl;
    }

    public String getProtexPostServerTimeOut() {
        return protexPostServerTimeOut;
    }

    public void setProtexPostServerName(String protexPostServerName) {
        this.protexPostServerName = protexPostServerName;
    }

    public void setProtexPostServerUrl(String protexPostServerUrl) {
        this.protexPostServerUrl = protexPostServerUrl;
    }

    public void setProtexPostServerTimeOut(String protexServerTimeout) {
        protexPostServerTimeOut = protexServerTimeout;
    }

    public String getProtexServerId() {
        return protexServerId;
    }

    @Override
    public ProtexServerInfoDescriptor getDescriptor() {
        return (ProtexServerInfoDescriptor) super.getDescriptor();
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("protexPostServerName", protexPostServerName)
                .add("protexPostServerUrl", protexPostServerUrl)
                .add("protexServerTimeout", protexPostServerTimeOut)
                .add("protexServerId", protexServerId)
                .toString();
    }
}
