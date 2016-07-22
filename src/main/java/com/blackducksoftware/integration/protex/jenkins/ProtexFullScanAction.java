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

import hudson.model.AbstractProject;

/**
 * @deprecated since 1.4.1, use
 *             com.blackducksoftware.integration.protex.jenkins.action.
 *             ProtexFullScanAction instead
 */
@Deprecated
public class ProtexFullScanAction extends com.blackducksoftware.integration.protex.jenkins.action.ProtexFullScanAction {


	/**
	 * Constructor that specifies the project this action belongs to. The
	 * boolean to maintain state is set to false meaning no full scan action will be performed.
	 *
	 * @param project
	 *            The project that action belongs to.
	 */
	public ProtexFullScanAction(final AbstractProject<?, ?> project) {
		super(project, false);
	}

	/**
	 * Constructor that specifies the project and the initial boolean value to
	 * maintain the state whether a full scan should occur or not.
	 *
	 * @param project
	 *            The project that the action belongs to.
	 * @param fullScanRequired
	 *            The boolean to determine if a full scan should be run or not.
	 */
	public ProtexFullScanAction(final AbstractProject<?, ?> project, final boolean fullScanRequired) {
		super(project, fullScanRequired);
	}
}
