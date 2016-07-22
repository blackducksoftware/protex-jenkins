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
package com.blackducksoftware.integration.protex.jenkins.action;

import java.util.Arrays;
import java.util.Collection;

import com.blackducksoftware.integration.protex.jenkins.PostBuildProtexScan;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.Project;
import hudson.model.TransientProjectActionFactory;

/**
 * This action should add the Full Scan action to Jobs
 *
 * @author jrichard
 *
 */
@Extension
public class ProtexFullScanActionFactory extends TransientProjectActionFactory {

	@Override
	public Collection<? extends Action> createFor(final AbstractProject target) {
		if (target == null) {
			return null;
		}
		if (target.getAction(ProtexFullScanAction.class) == null) {
			// We only want to add the action if it does not already exist
			Project<?, ?> project = null;

			if (target instanceof Project<?, ?>) {
				project = (Project<?, ?>) target;
				if (project != null) {
					final PostBuildProtexScan protexScan = project.getPublishersList().get(PostBuildProtexScan.class);

					if (protexScan != null) {
						if (protexScan.isPluginConfigured()) {
							try {
								return Arrays.asList(new ProtexFullScanAction(project));
							} catch (final IllegalArgumentException e) {
								return null;
							}
						}
					}
				}
			}
		}
		return null;
	}
}
