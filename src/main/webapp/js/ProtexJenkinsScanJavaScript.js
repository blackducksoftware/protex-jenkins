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
function adjustConfig() {

	var projectNameElement = document
			.getElementsByName('_.protexPostProjectName')[0];
	var templateProjectNameElement = document
			.getElementsByName('_.protexPostTemplateProjectName')[0];
	var publishers = document.getElementsByName('publisher');
	var protexPublisher = null;
	for (i = 0; i < publishers.length; i++) { 
		var descriptorId = publishers[i].getAttribute("descriptorid");
		if (descriptorId != null || descriptorId.indexOf("com.blackducksoftware.integration.protex.jenkins.PostBuildProtexScan") > -1) {
			protexPublisher = publishers[i];
		}
	}
	
	var createProjectButton = protexPublisher.getElementsByTagName('button')[1]; //The first button is the add credentials button, so we get the second
	var projectName = projectNameElement.value;
	var templateProjectName = templateProjectNameElement.value;

	if (projectName != null || projectName != "" || projectName != " ") {
		if (projectName.indexOf("$") > -1) {
			createProjectButton.disabled = true;
		} else if (templateProjectName != null || templateProjectName != ""
				|| templateProjectName != " ") {
			if (templateProjectName.indexOf("$") > -1) {
				createProjectButton.disabled = true;
			}

		} else {
			createProjectButton.disabled = false;
		}
	} else {
		createProjectButton.disabled = false;
	}

};
var old = window.onload;
window.onload = function() {
	old();
	//adjustConfig(); //uncomment this and it will disable the create project button when the job config loads if the project name or template name contatins a $ sign
};
