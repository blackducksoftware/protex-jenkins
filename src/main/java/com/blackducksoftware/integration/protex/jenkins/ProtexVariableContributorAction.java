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

/**
 * Do not use this action. It has been moved to a new package. We keep these
 * around for now for compatibility.
 *
 * @deprecated since 1.3.3, use
 *             com.blackducksoftware.integration.protex.jenkins.action.
 *             ProtexVariableContributorAction instead
 *
 */
@Deprecated
public class ProtexVariableContributorAction extends com.blackducksoftware.integration.protex.jenkins.action.ProtexVariableContributorAction {

	public ProtexVariableContributorAction(final String protexServerUrl, final String protexProjectName, final String protexTemplateProjectName, final String protexProjectSourcePath) {
		super(protexServerUrl, protexProjectName, protexTemplateProjectName, protexProjectSourcePath);
	}

}
