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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;
import org.restlet.resource.ResourceException;

import com.blackducksoftware.integration.phone.home.PhoneHomeClient;
import com.blackducksoftware.integration.phone.home.enums.BlackDuckName;
import com.blackducksoftware.integration.phone.home.enums.ThirdPartyName;
import com.blackducksoftware.integration.phone.home.exception.PhoneHomeException;
import com.blackducksoftware.integration.phone.home.exception.PropertiesLoaderException;
import com.blackducksoftware.integration.protex.ProtexFacade;
import com.blackducksoftware.integration.protex.ProtexFacadeException;
import com.blackducksoftware.integration.protex.ProtexServerInfo;
import com.blackducksoftware.integration.protex.exceptions.ProtexValidationException;
import com.blackducksoftware.integration.protex.jenkins.action.ProtexFullScanAction;
import com.blackducksoftware.integration.protex.jenkins.action.ProtexReportAction;
import com.blackducksoftware.integration.protex.jenkins.action.ProtexVariableContributorAction;
import com.blackducksoftware.integration.protex.jenkins.action.ScanRunAction;
import com.blackducksoftware.integration.protex.jenkins.remote.GetCanonicalPath;
import com.blackducksoftware.integration.protex.jenkins.remote.GetHostName;
import com.blackducksoftware.integration.protex.jenkins.remote.GetHostNameFromNetworkInterfaces;
import com.blackducksoftware.integration.protex.jenkins.remote.GetSystemProperty;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;
import com.blackducksoftware.sdk.protex.report.Report;
import com.blackducksoftware.sdk.protex.report.ReportFormat;
import com.blackducksoftware.sdk.protex.report.ReportTemplate;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.security.ACL;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;
import jenkins.model.Jenkins;

public class PostBuildProtexScan extends Recorder {

	// Init Variables for Protex Plugin
	private final String protexServerId;

	private final String protexPostCredentials;

	private final String protexPostTemplateProjectName;

	private final String protexPostProjectName;

	private final String protexPostProjectSourcePath;

	private final Double protexScanMemory;

	private final String protexReportTemplate;

	private transient Result result;

	private transient String javaName;

	private transient String javaPath;

	// Fields in config.jelly must match the parameter names in the
	// "DataBoundConstructor"
	@DataBoundConstructor
	public PostBuildProtexScan(final String protexServerId, final String protexPostCredentials, final String protexPostProjectName,
			final String protexPostTemplateProjectName,
			final String protexPostProjectSourcePath, final Double protexScanMemory, final String protexReportTemplate) {
		// These are the fields for the plugin

		this.protexServerId = protexServerId;

		this.protexPostCredentials = protexPostCredentials;

		if (protexPostProjectName != null) {
			this.protexPostProjectName = protexPostProjectName.trim();
		} else {
			this.protexPostProjectName = protexPostProjectName;
		}

		if (protexPostTemplateProjectName != null) {
			this.protexPostTemplateProjectName = protexPostTemplateProjectName.trim();
		} else {
			this.protexPostTemplateProjectName = protexPostTemplateProjectName;
		}
		if (protexPostProjectSourcePath != null) {
			this.protexPostProjectSourcePath = protexPostProjectSourcePath.trim();
		} else {
			this.protexPostProjectSourcePath = protexPostProjectSourcePath;
		}

		this.protexScanMemory = protexScanMemory;

		if (protexReportTemplate != null) {
			this.protexReportTemplate = protexReportTemplate.trim();
		} else {
			this.protexReportTemplate = protexReportTemplate;
		}
	}

	@Override
	public BuildStepMonitor getRequiredMonitorService() {
		return BuildStepMonitor.NONE;
	}

	public String getProtexServerId() {
		return protexServerId;
	}

	public String getProtexPostCredentials() {
		return protexPostCredentials;
	}

	public String getProtexPostProjectName() {
		return protexPostProjectName;
	}

	public String getProtexPostTemplateProjectName() {
		return protexPostTemplateProjectName;
	}

	public String getProtexPostProjectSourcePath() {
		return protexPostProjectSourcePath;
	}

	public Double getProtexScanMemory() {
		if (protexScanMemory == null) {
			// This way the User does not have to re-configure every Job with a Protex scan
			// If the value is null, we use the default amount of memory
			return 2.0;
		}
		return protexScanMemory;
	}

	public String getProtexReportTemplate() {
		return protexReportTemplate;
	}

	@Override
	public PostBuildProtexScanDescriptor getDescriptor() {
		return (PostBuildProtexScanDescriptor) super.getDescriptor();
	}

	private void setResult(final Result result) {
		this.result = result;
	}

	/**
	 * Overrides the Recorder perform method. This is the method that gets called by Jenkins to run as a Post Build
	 * Action
	 *
	 * @param build
	 *            AbstractBuild<?, ?>
	 * @param launcher
	 *            Launcher
	 * @param listener
	 *            BuildListener
	 * @throws InterruptedException
	 * @throws IOException
	 *
	 */
	@Override
	public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher,
			final BuildListener listener) throws IOException, InterruptedException {
		setResult(build.getResult());
		final ProtexJenkinsLogger logger = new ProtexJenkinsLogger(listener);
		logger.setLogLevel(LogLevel.DEBUG); // TODO make the log level configurable

		if (result.equals(Result.SUCCESS)) {

			final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

			boolean changed = false;
			try {

				if (PostBuildProtexScan.class.getClassLoader() != originalClassLoader) {
					changed = true;
					Thread.currentThread().setContextClassLoader(PostBuildProtexScan.class.getClassLoader());
				}

				if (isPluginConfigured()) {

					if (getProtexScanMemory() < 2) {
						logger.error("Did not provide enough memory for the Protex Scan : " + getProtexScanMemory() + " GB");
						build.setResult(Result.UNSTABLE);
						return true;
					}
					final ProtexServerInfo currentServer = getProtexServerInfo(getProtexServerId());
					if (currentServer == null) {
						logger.error("Can not find the defined Protex Server with the Id : " + protexServerId);
						build.setResult(Result.UNSTABLE);
						return true;
					}
					ProtexFacade facade = null;
					try {
						facade = getProtexFacade(logger);
						if (facade == null) {
							logger.error("There was a problem creating the ProtexFacade");
							build.setResult(Result.UNSTABLE);
							return true;
						}
					} catch (final Exception e1) {
						// Catch all errors and print stack trace of any errors that
						// happened in creation of the ProtexFacade
						logger.error(e1);
						build.setResult(Result.UNSTABLE);
						return true;
					}
					final ProtexFullScanAction protexFullScanAction = getProtexFullScanAction(logger, build);
					boolean fullScanRequired = false;
					if (protexFullScanAction != null) {
						fullScanRequired = protexFullScanAction.isFullScanRequired();
					}
					final EnvVars variables = build.getEnvironment(listener);

					String localHostName = "";
					try {
						localHostName = build.getBuiltOn().getChannel().call(new GetHostName());
					} catch (final IOException e) {
						// logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
						// ignore the error, try to get the host name from the network interfaces
					}
					if (StringUtils.isBlank(localHostName)) {
						try {
							localHostName = build.getBuiltOn().getChannel().call(new GetHostNameFromNetworkInterfaces());
						} catch (final IOException e) {
							logger.error("Problem getting the Local Host name : " + e.getMessage(), e);
						}
					}

					setJava(logger, build);

					// These lines resolve any variables that the User may have used in the
					// project names or in the source path
					// Works for variables defined as $VARIABLE or as ${VARIABLE}
					// See this link for a list of Jenkins defined variables
					// https://wiki.jenkins-ci.org/display/JENKINS/Building+a+software+project#Buildingasoftwareproject-JenkinsSetEnvironmentVariables
					final String projectName = handleVariableReplacement(build, logger, variables, getProtexPostProjectName());
					final String templateName = handleVariableReplacement(build, logger, variables, getProtexPostTemplateProjectName());
					String sourcePath = handleVariableReplacement(build, logger, variables, getProtexPostProjectSourcePath());
					printConfiguration(build, localHostName, currentServer, projectName, getProtexScanMemory(), sourcePath, protexFullScanAction, logger);

					build.addAction(new ProtexVariableContributorAction(currentServer.getProtexPostServerUrl(), projectName, templateName, sourcePath));

					if (!build.getResult().equals(Result.SUCCESS)) {
						return true;
					}

					// Creates the Project during the build
					if (!facade.checkProjectExists(projectName)) {
						facade.createProtexProject(projectName, templateName);
					} else {
						logger.warn("The Project : " + projectName + ", already exists! The scan will use the already exisiting Project.");
					}

					// Check if the sourcepath is empty, if it is scan the whole workspace
					// otherwise the path provided should be a sub directory in the workspace
					try {
						sourcePath = validateSourcePath(build, build.getBuiltOn().getChannel(), logger, sourcePath);
					} catch (final ProtexValidationException e) {
						e.printSmallStackTrace(logger);
						build.setResult(Result.UNSTABLE);
						return true;
					}

					// Phone-Home
					try {
						String protexHostName = null;
						try {
							final URL url = new URL(currentServer.getProtexPostServerUrl());
							protexHostName = url.getHost();
						} catch (final Exception e) {
							logger.debug("Could not get the Protex Host name.");
						}
						final String protexVersion = facade.getProtexVersion();
						bdPhoneHome(protexHostName, protexVersion);
					} catch (final Exception e) {
						logger.debug("Unable to phone-home", e);
					}

					logger.info("Starting Protex Scan...");
					final String projectId = facade.getProtexProjectId(projectName);
					if (projectId == null) {
						logger.error("The project Id was returned as null");
						build.setResult(Result.UNSTABLE);
						return true;
					}
					facade.protexPrepScanProject(projectId, localHostName, sourcePath);

					String bdsToolJavaOptions = null;

					// This feature should not be documented and should only be exposed to customers
					// if they have issues similiar to the issues in https://jira.dc2.lan/browse/PROTEX-20822
					// This should only be used as a workaround to an issue with bdstool
					if (variables.containsKey("BDSTOOLJAVAOPTIONS")) {
						// Try to get the BDSTOOLJAVAOPTIONS from the Build environment
						bdsToolJavaOptions = variables.get("BDSTOOLJAVAOPTIONS");
					} else {
						// Try to get the BDSTOOLJAVAOPTIONS from the System environment
						bdsToolJavaOptions = System.getenv("BDSTOOLJAVAOPTIONS");
					}

					final ProtexScanner scanner = createProtexScanner(javaPath, facade.getServerUrl(), projectId, projectName, sourcePath, bdsToolJavaOptions, logger);

					scanner.setForceScan(fullScanRequired);

					final File logDirectory = new File(build.getWorkspace().getRemote(), "BDSToolLog");
					scanner.setLogDirectory(logDirectory);
					scanner.setProtexScanMemory(getProtexScanMemory());

					// Will run scan on master or remote node
					if (build.getBuiltOn().getChannel().call(scanner)) {
						// check the build result if successful reset the full scan action to
						// prevent running a full scan.
						if (protexFullScanAction != null && fullScanRequired == true) {
							// reset so that the
							protexFullScanAction.setFullScanRequired(false);
						}
						logger.debug("Completed Protex Scan");
						build.addAction(new ScanRunAction());

						if (!generateProtexReport(build, logger, facade, projectId, getProtexReportTemplate())) {
							build.setResult(Result.UNSTABLE);
							return true;
						}
					} else {
						logger.error("Protex Scan did not run.");
					}
				} else {
					logger.error("This Protex scan was not configured correctly. Missing required fields!");
					build.setResult(Result.UNSTABLE);
					return true;
				}
			} catch (final ServerConnectionException e) {
				logger.error(e.getMessage(), e);
				build.setResult(Result.UNSTABLE);
				return true;
			} catch (final ProtexFacadeException e) {
				if (e.getSdkFaultErrorCode() != null) {
					logger.error("SdkFault ErrorCode : " + e.getSdkFaultErrorCode().toString());
				}
				if (e.getSdkFaultMessage() != null) {
					logger.error("SdkFault ErrorMessage : " + e.getSdkFaultMessage());
				}
				logger.error(e.getMessage(), e);
				build.setResult(Result.UNSTABLE);
				return true;
			} catch (final ServerConfigException e) {
				logger.error(e.getMessage(), e);
				build.setResult(Result.UNSTABLE);
				return true;
			} catch (final Exception e) {
				logger.error(e.getMessage(), e);
				build.setResult(Result.UNSTABLE);
				return true;
			} finally {
				if (changed) {
					Thread.currentThread().setContextClassLoader(
							originalClassLoader);
				}
			}
		} else {
			logger.warn("Build was not successful. Will not run Protex Scan.");
		}

		logger.info("Finished running Protex Post Build Step.");
		build.setResult(result);
		return true;
	}

	public void printConfiguration(final AbstractBuild<?, ?> build, final String localHost, final ProtexServerInfo currentServer,
			final String projectName, final Double scanMemory, final String sourcePath, final ProtexFullScanAction protexFullScanAction, final ProtexJenkinsLogger logger)
					throws IOException,
					InterruptedException {
		logger.info(
				"Initializing - Protex Jenkins Plugin - "
						+ getDescriptor().getPluginVersion());
		logger.info("-> Running on : " + localHost);
		logger.info("-> Using Url : " + currentServer.getProtexPostServerUrl());
		logger.info("-> Using Username : " + getProtexUsernamePassword().getUsername());
		logger.info("-> Using Protex Global TimeOut : " + currentServer.getProtexPostServerTimeOut());
		logger.info(
				"-> Using Build Full Name : " + build.getFullDisplayName());
		logger.info(
				"-> Using Build Number : " + build.getNumber());

		if (build.getWorkspace() == null) {
			// if the build workspace is null then the there might be a custom workspace set
			logger.info(
					"-> Using Build Workspace Path : "
							+ build.getProject().getCustomWorkspace());
		} else {
			logger.info(
					"-> Using Build Workspace Path : "
							+ build.getWorkspace().getRemote());
		}
		logger.info(
				"-> Using Protex Project Name : " + projectName);
		logger.info(
				"-> Using Protex Source Path  : " + sourcePath);

		logger.info(
				"-> Using Protex Scan Memory  : " + scanMemory);

		logger.info(
				"-> Using JDK : " + javaName + " at : " + javaPath);

		if (protexFullScanAction != null) {

			if (protexFullScanAction.isFullScanRequired() == true) {
				logger.info(
						"-> Force Protex Full Scan : yes");
			} else {
				logger.info(
						"-> Force Protex Full Scan : no");
			}
		}
		else {
			logger.info(
					"-> Force Protex Full Scan : no");
		}
	}

	/**
	 * Generates the Protex Report for the given Project using the specified Report Template. Once the report is
	 * generated it will attach the report to the Build.
	 * Returns true if successful or skipped. Returns false if there was an error.
	 *
	 *
	 *
	 * @param build
	 * @param logger
	 * @param facade
	 * @param projectId
	 * @param reportTemplate
	 * @return
	 */
	public boolean generateProtexReport(final AbstractBuild<?, ?> build, final IntLogger logger, final ProtexFacade facade, final String projectId, final String reportTemplate) {
		if (StringUtils.isBlank(reportTemplate)) {
			return true;
		}
		logger.info("Creating Protex Report from template : " + reportTemplate);
		try {
			ReportTemplate template = null;
			try {
				template = facade.getReportTemplate(reportTemplate);
			} catch (final ProtexFacadeException e) {
				logger.error(e.getMessage());
				return false;
			}
			final Report report = facade.createReportFromTemplate(projectId, template.getReportTemplateId(), ReportFormat.HTML, true);
			final InputStream reportInputStream = report.getFileContent().getInputStream();

			final String htmlContent = IOUtils.toString(reportInputStream);

			final ProtexReportAction reportAction = new ProtexReportAction(build, htmlContent);
			build.addAction(reportAction);
		} catch (final ProtexFacadeException e) {
			logger.error(e.getMessage());
			return false;
		} catch (final ServerConnectionException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (final ServerConfigException e) {
			logger.error(e.getMessage(), e);
			return false;
		} catch (final Exception e) {
			logger.error(e.getMessage(), e);
			return false;
		}
		return true;
	}

	public ProtexServerInfo getProtexServerInfo(final String protexServerId) {
		ProtexServerInfo currentServer = null;
		for (final ProtexServerInfo server : getDescriptor().getProtexServers()) {
			if (server.getProtexServerId().equals(protexServerId)) {
				currentServer = server;
				break;
			}
		}
		return currentServer;
	}

	/**
	 *
	 * @param build
	 *            AbstractBuild
	 * @param variables
	 *            Map of variables
	 * @param value
	 *            String to check for variables
	 * @return the new Value with the variables replaced
	 */
	public String handleVariableReplacement(final AbstractBuild<?, ?> build, final ProtexJenkinsLogger logger, final Map<String, String> variables, final String value) {
		if (value != null) {

			final String newValue = Util.replaceMacro(value, variables);

			if (newValue.contains("$")) {
				logger.error("Variable was not properly replaced. Value : " + value + ", Result : " + newValue);
				logger.error("Make sure the variable has been properly defined.");
				build.setResult(Result.UNSTABLE);
			}
			return newValue;
		} else {
			return null;
		}
	}

	public ProtexFacade getProtexFacade(final IntLogger logger) throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
	IllegalBlockSizeException, BadPaddingException, IOException, ServerConfigException {

		final ProtexServerInfo currentServer = getProtexServerInfo(getProtexServerId());
		if (currentServer == null) {
			logger.error("Can not find the defined Protex Server with the Id : " + protexServerId);
			return null;
		}

		final ProtexFacade facade = new ProtexFacade(currentServer.getProtexPostServerUrl(), getProtexUsernamePassword().getUsername(),
				getProtexUsernamePassword().getPassword().getPlainText(), Long.valueOf(currentServer.getProtexPostServerTimeOut()));
		facade.setLogger(logger);
		// facade.setAutoContextLoading(true);

		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null && jenkins.proxy != null) {
			final URL url = new URL(currentServer.getProtexPostServerUrl());
			final Proxy proxy = ProxyConfiguration.createProxy(url.getHost(), jenkins.proxy.name, jenkins.proxy.port,
					jenkins.proxy.noProxyHost);
			if (proxy.address() != null) {
				final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
				if (!StringUtils.isEmpty(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {
					if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
						facade.setProxySettings(proxyAddress.getHostName(), proxyAddress.getPort(), ProxyServerType.HTTP, true, jenkins.proxy.getUserName(),
								jenkins.proxy.getPassword());
					} else {
						facade.setProxySettings(proxyAddress.getHostName(), proxyAddress.getPort(), ProxyServerType.HTTP, true);
					}
				}

			}
		}

		return facade;
	}

	public ProtexScanner createProtexScanner(final String javaPath, final String protexUrl, final String projectId, final String projectName, final String sourcePath,
			final String bdsToolJavaOptions, final ProtexJenkinsLogger logger)
					throws MalformedURLException {
		if (javaPath == null) {
			logger.info("Java is null");
		} else {
			logger.info("Will scan with Java : " + javaPath);
		}

		final ProtexScanner scanner = new ProtexScanner(javaPath, protexUrl, getProtexUsernamePassword(), projectName,
				sourcePath, projectId);
		scanner.setLogger(logger);

		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null && jenkins.proxy != null) {
			final URL url = new URL(protexUrl);
			final Proxy proxy = ProxyConfiguration.createProxy(url.getHost(), jenkins.proxy.name, jenkins.proxy.port,
					jenkins.proxy.noProxyHost);

			if (proxy.address() != null) {
				final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
				if (!StringUtils.isEmpty(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {

					scanner.setProxyHost(proxyAddress.getHostName());
					scanner.setProxyPort(proxyAddress.getPort());

					if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
						scanner.setProxyUser(jenkins.proxy.getUserName());
						scanner.setProxyPassword(jenkins.proxy.getPassword());
					}
				}

			}
		}
		scanner.setBdsToolJavaOptions(bdsToolJavaOptions);
		// getMatchingNoProxyHostPatterns
		return scanner;
	}

	private ProtexFullScanAction getProtexFullScanAction(final ProtexJenkinsLogger logger, final AbstractBuild<?, ?> build) {
		final ProtexFullScanAction fullScanAction = build.getProject().getAction(ProtexFullScanAction.class);
		if (fullScanAction == null) {
			logger.warn("No ProtexFullScanAction found for this Project.");
		}
		return fullScanAction;
	}

	/**
	 * Sets the Java Home that is to be used for running the Shell script
	 *
	 * @param build
	 *            AbstractBuild<?, ?>
	 * @param listener
	 *            BuildListener
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProtexValidationException
	 * @throws HubConfigurationException
	 */
	private void setJava(final ProtexJenkinsLogger logger, final AbstractBuild<?, ?> build) throws IOException, InterruptedException, ProtexValidationException {
		final EnvVars envVars = build.getEnvironment(logger.getJenkinsListener());
		JDK javaHomeTemp = null;
		if (StringUtils.isEmpty(build.getBuiltOn().getNodeName())) {
			logger.info("Getting Jdk on master  : " + build.getBuiltOn().getNodeName());
			// Empty node name indicates master
			final String byteCodeVersion = System.getProperty("java.class.version");
			final Double majorVersion = Double.valueOf(byteCodeVersion);

			final String arch = SystemUtils.OS_ARCH;

			final boolean unsupportedArch = arch.endsWith("86");
			if (unsupportedArch) {
				logger.info("The JRE that is running this Node is a non supported architecture type : " + arch);
			}

			if (!unsupportedArch && majorVersion >= 51.0) {
				// Java 7 bytecode
				// If the master is running Java 7 or higher we use that Java
				// instead of the one the User selected in the job configuration
				final String javaHome = System.getProperty("java.home");
				javaHomeTemp = new JDK("Java running master agent", javaHome);
			} else {
				javaHomeTemp = build.getProject().getJDK();
			}
		} else {
			logger.info("Getting Jdk on node  : " + build.getBuiltOn().getNodeName());
			final String byteCodeVersion = build.getBuiltOn().getChannel().call(new
					GetSystemProperty("java.class.version"));
			final Double majorVersion = Double.valueOf(byteCodeVersion);

			final String arch = build.getBuiltOn().getChannel().call(new
					GetSystemProperty("os.arch"));

			final boolean unsupportedArch = arch.endsWith("86");
			if (unsupportedArch) {
				logger.info("The JRE that is running this Node is a non supported architecture type : " + arch);
			}

			if (!unsupportedArch && majorVersion >= 51.0) {
				// Java 7 bytecode
				// If the slave is running Java 7 or higher we use that Java
				// instead of the one the User selected in the job configuration
				final String javaHome = build.getBuiltOn().getChannel().call(new GetSystemProperty("java.home"));
				javaHomeTemp = new JDK("Java running slave agent", javaHome);
			} else {
				javaHomeTemp = build.getProject().getJDK().forNode(build.getBuiltOn(), logger.getJenkinsListener());
			}
		}
		if (javaHomeTemp != null && javaHomeTemp.getHome() != null) {
			logger.info("JDK home : " + javaHomeTemp.getHome());
		}

		if (javaHomeTemp == null || StringUtils.isEmpty(javaHomeTemp.getHome())) {
			logger.info("Could not find the specified Java installation, checking the JAVA_HOME variable.");
			if (envVars.get("JAVA_HOME") == null || envVars.get("JAVA_HOME") == "") {
				throw new ProtexValidationException("Need to define a JAVA_HOME or select an installed JDK.");
			}
			// In case the user did not select a java installation, set to the environment variable JAVA_HOME
			javaHomeTemp = new JDK("Default Java", envVars.get("JAVA_HOME"));
		}
		setJavaName(javaHomeTemp.getName());

		final String osName = build.getBuiltOn().getChannel().call(new GetSystemProperty("os.name"));

		final FilePath javaHome = new FilePath(build.getBuiltOn().getChannel(), javaHomeTemp.getHome());
		FilePath javaExec = new FilePath(javaHome, "bin");

		if (osName.toLowerCase().contains("windows")) {
			javaExec = new FilePath(javaExec, "java.exe");
		} else {
			javaExec = new FilePath(javaExec, "java");
		}
		if (!javaExec.exists()) {
			throw new ProtexValidationException("Could not find the specified Java installation at: " +
					javaExec.getRemote());
		}
		javaPath = javaExec.getRemote();
	}

	public String getJavaPath() {
		return javaPath;
	}

	public String getJavaName() {
		return javaName;
	}

	public void setJavaName(final String javaName) {
		this.javaName = javaName;
	}

	protected UsernamePasswordCredentialsImpl getProtexUsernamePassword() {
		UsernamePasswordCredentialsImpl credential = null;

		final AbstractProject<?, ?> nullProject = null;
		final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
				nullProject, ACL.SYSTEM,
				Collections.<DomainRequirement> emptyList());

		final IdMatcher matcher = new IdMatcher(getProtexPostCredentials());
		for (final StandardCredentials c : credentials) {
			if (matcher.matches(c)) {
				if (c instanceof UsernamePasswordCredentialsImpl) {
					credential = (UsernamePasswordCredentialsImpl) c;
				}
			}
		}
		return credential;
	}

	/**
	 * Validates that the source target exists
	 *
	 * @param build
	 *            AbstractBuild<?, ?>
	 * @param channel
	 *            VirtualChannel
	 * @param sourcePath
	 *            String
	 *
	 * @throws IOException
	 * @throws InterruptedException
	 * @throws ProtexValidationException
	 */
	public String validateSourcePath(final AbstractBuild<?, ?> build, final VirtualChannel channel, final ProtexJenkinsLogger logger,
			final String sourcePath) throws IOException, InterruptedException, ProtexValidationException {
		FilePath workspace = null;
		String target = "";
		if (build.getWorkspace() == null) {
			// May have a custom workspace set
			workspace = new FilePath(channel, build.getProject().getCustomWorkspace());

		} else {
			workspace = build.getWorkspace();
		}

		final String workspacePath = channel.call(new GetCanonicalPath(new File(workspace.getRemote())));

		if (StringUtils.isBlank(sourcePath) || sourcePath.equals(workspacePath)) {
			// The User either included the path to the workspace or they include the $WORKSPACE variable in the
			// path
			target = workspacePath;
		} else if (sourcePath.startsWith(workspacePath)) {
			// The User either included the path to the workspace or they include the $WORKSPACE variable in the
			// path
			target = channel.call(new GetCanonicalPath(new File(sourcePath)));
		} else {
			target = channel.call(new GetCanonicalPath(new File(workspacePath
					+ File.separator + sourcePath)));
		}

		if (target.equals(workspacePath)) {
			// Scanning the workspace so we want to put .bdsignore in the workspace so it ignores the logs
			final FilePath bdsIgnore = new FilePath(workspace, ".bdsignore");
			if (!bdsIgnore.exists()) {
				// This will cause bdstool to ignore the directory that we are putting the bdstool logs in
				bdsIgnore.write(".*BDSToolLog.*", "UTF-8");
			}
		}

		if (!target.contains(workspacePath)) {
			throw new ProtexValidationException("Can not specify a source path outside of the workspace.");
		}

		final FilePath workspaceTarget = new FilePath(channel, target);
		if (!workspaceTarget.exists()) {
			throw new ProtexValidationException("Source path could not be found : " + target);
		} else {
			logger.debug(
					"Source path exists at : " + target);
		}
		return target;
	}

	public boolean isPluginConfigured() {
		boolean serverIdConfigured = false;
		boolean credentialsIdConfigured = false;
		boolean projectNameConfigured = false;
		boolean scanMemoryConfigured = false;

		if (!StringUtils.isEmpty(getProtexServerId())) {
			serverIdConfigured = true;
		}
		if (!StringUtils.isEmpty(getProtexPostCredentials())) {
			credentialsIdConfigured = true;
		}
		if (!StringUtils.isEmpty(getProtexPostProjectName())) {
			projectNameConfigured = true;
		}
		if (getProtexScanMemory() != null && getProtexScanMemory() > 0) {
			scanMemoryConfigured = true;
		}

		return serverIdConfigured && credentialsIdConfigured && projectNameConfigured && scanMemoryConfigured;
	}

	/**
	 * @param protexHostName
	 *            Host name of the protex instance that this plugin uses
	 *
	 *            This method "phones-home" to the internal BlackDuck
	 *            Integrations server. Every time a build is kicked off,
	 */
	public void bdPhoneHome(final String protexHostName, final String protexVersion)
			throws IOException, PhoneHomeException, PropertiesLoaderException, ResourceException, JSONException {
		if (StringUtils.isNotBlank(protexHostName)) {
			final String thirdPartyVersion = Jenkins.getVersion().toString();
			final String pluginVersion = getDescriptor().getPluginVersion();
			if (StringUtils.isNotBlank(thirdPartyVersion) && StringUtils.isNotBlank(pluginVersion)) {
				final PhoneHomeClient phClient = new PhoneHomeClient();
				phClient.callHomeIntegrations(null, protexHostName, BlackDuckName.PROTEX, protexVersion,
						ThirdPartyName.JENKINS,
						thirdPartyVersion, pluginVersion);
			}
		}
	}

}
