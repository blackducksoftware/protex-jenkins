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

import hudson.remoting.Callable;

import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.interceptor.Fault;

import com.blackducksoftware.integration.protex.exceptions.ProtexScannerException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.blackducksoftware.protex.plugin.BlackDuckCommand;
import com.blackducksoftware.protex.plugin.BlackDuckCommand.State;
import com.blackducksoftware.protex.plugin.BlackDuckCommandBuilder;
import com.blackducksoftware.protex.plugin.BlackDuckCommandBuilder.AnalyzeCommandBuilder;
import com.blackducksoftware.protex.plugin.BuildToolIntegrationException;
import com.blackducksoftware.protex.plugin.ProtexServer;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

public class ProtexScanner implements Callable<Boolean, Exception> {

    private final String protexUrl;

    private final String projectId;

    private final String protexProjectName;

    private final String protexProjectSourcePath;

    private final String javaPath;

    private final UsernamePasswordCredentialsImpl protexCredentials;

    private String proxyHost;

    private int proxyPort;

    private String proxyUser;

    private String proxyPassword;

    private ProtexJenkinsLogger logger;

    private File logDirectory;

    private Double protexScanMemory;

    private String bdsToolJavaOptions;

    private boolean forceScan;

    public ProtexScanner(String javaPath, String protexUrl, UsernamePasswordCredentialsImpl protexCredentials, String protexProjectName,
            String protexProjectSourcePath, String projectId) {
        this.javaPath = javaPath;
        this.protexUrl = protexUrl;
        this.protexCredentials = protexCredentials;
        this.protexProjectName = protexProjectName;
        this.projectId = projectId;
        this.protexProjectSourcePath = protexProjectSourcePath;
    }

    public void setBdsToolJavaOptions(String bdsToolJavaOptions) {
        this.bdsToolJavaOptions = bdsToolJavaOptions;
    }

    public Double getProtexScanMemory() {
        return protexScanMemory;
    }

    public void setProtexScanMemory(Double protexScanMemory) {
        this.protexScanMemory = protexScanMemory;
    }

    public File getLogDirectory() {
        return logDirectory;
    }

    public void setLogDirectory(File logDirectory) {
        this.logDirectory = logDirectory;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return proxyUser;
    }

    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

    public UsernamePasswordCredentialsImpl getProtexCredentials() {
        return protexCredentials;
    }

    public String getProjectId() {
        return projectId;
    }

    public ProtexJenkinsLogger getLogger() {
        return logger;
    }

    public void setLogger(ProtexJenkinsLogger logger) {
        this.logger = logger;
    }

    public String getProtexProjectName() {
        return protexProjectName;
    }

    public String getProtexProjectSourcePath() {
        return protexProjectSourcePath;
    }

    public boolean isForceScan() {
        return forceScan;
    }

    public void setForceScan(boolean forceScan) {
        this.forceScan = forceScan;
    }

    @Override
    public Boolean call() throws ServerConnectionException, ServerConfigException, ProtexScannerException, IOException, InterruptedException {
        ProtexServer protexServer = new ProtexServer(protexCredentials.getPassword().getPlainText());
        protexServer.setServerUrl(protexUrl);
        protexServer.setUsername(protexCredentials.getUsername());

        if (StringUtils.isNotBlank(getProxyHost()) && getProxyPort() != -1) {

            InetSocketAddress address = new InetSocketAddress(getProxyHost(), getProxyPort());
            protexServer.setProxy(new Proxy(Type.HTTP, address));

            // protexServer.setProxy(new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(getProxyHost(),
            // getProxyPort())));

        }
        if (StringUtils.isNotBlank(getProxyUser()) && StringUtils.isNotBlank(getProxyPassword())) {
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            if (getRequestorType().equals(RequestorType.PROXY)) {

                                return new PasswordAuthentication(
                                        getProxyUser(), getProxyPassword().toCharArray());
                            }

                            return null;
                        }
                    }
                    );
        } else {
            // IJP-179 potential fix
            Authenticator.setDefault(
                    new Authenticator() {
                        @Override
                        public PasswordAuthentication getPasswordAuthentication() {
                            return null;
                        }
                    }
                    );
        }

        runProtexScan(protexServer, getProjectId(), getProtexProjectSourcePath(), true);

        return true;
    }

    /**
     * This runs the Protex scan on the given source path
     *
     * @throws ProtexScannerException
     * @throws ServerConnectionException
     */
    public void runProtexScan(ProtexServer protexServer, String projectId, String localProtexProjectSourcePath, boolean attachObserver)
            throws ProtexScannerException, ServerConnectionException {
        if (protexServer == null) {
            throw new IllegalArgumentException("Need to provide the ProtexServer that the scan will use to connect to the server.");
        } else if (StringUtils.isEmpty(protexServer.getServerUrl())) {
            throw new IllegalArgumentException("Need to set the Url to connect to in the ProtexServer object");
        } else if (StringUtils.isEmpty(protexServer.getUsername())) {
            throw new IllegalArgumentException("Need to set the UserName in the ProtexServer object");
        } else if (StringUtils.isEmpty(protexServer.getPassword())) {
            throw new IllegalArgumentException("Need to set the Password in the ProtexServer object");
        } else if (StringUtils.isEmpty(projectId)) {
            throw new IllegalArgumentException("Need to provide the Id of the Protex Project to scan.");
        } else if (StringUtils.isEmpty(localProtexProjectSourcePath)) {
            throw new IllegalArgumentException("Need to provide the source path to be scanned.");
        }

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (ProtexScanner.class.getClassLoader() != originalClassLoader) {
                Thread.currentThread().setContextClassLoader(ProtexScanner.class.getClassLoader());
                changed = true;
            }

            JenkinsProtexLogObserver observer = new JenkinsProtexLogObserver(logger);
            try {
                // Preparing BDSTOOL Command to analyze
                AnalyzeCommandBuilder cmdBuilder = BlackDuckCommandBuilder.analyze();

                if (StringUtils.isNotBlank(javaPath)) {
                    File javaHome = new File(javaPath);
                    logger.info("Using this java for the command : " + javaHome.getAbsolutePath());

                    cmdBuilder.setJava(javaHome);
                }

                // The Scan Logs do not provide much output
                cmdBuilder.observingScanLog(observer);

                // This is fairly chatty output on the Scan Progress
                // if (observer != null) {
                cmdBuilder.observingProgress(observer);

                // This provides the end scan output with number of pending
                // ID
                cmdBuilder.observingMessages(observer);

                // This provides the entire command output
                cmdBuilder.observingOutput(observer);

                // Defining Project ID and Server
                cmdBuilder.projectId(projectId);
                // Connecting to Protex
                cmdBuilder.connectedTo(protexServer);
                // Defining file to scan
                File localProtexProjectSourcePathFile = new File(localProtexProjectSourcePath);
                cmdBuilder.directory(localProtexProjectSourcePathFile);

                if (getLogDirectory() != null) {
                    if (!getLogDirectory().exists()) {
                        if (!getLogDirectory().mkdirs()) {
                            throw new ProtexScannerException(
                                    "Could not create the log directory in the workspace. Please check to make sure the Jenkins User has access to/owns this workspace");
                        }
                    } else if (!getLogDirectory().canWrite()) {
                        throw new ProtexScannerException(
                                "The Jenkins user does not have the correct permissions to access the log directory : " + getLogDirectory().getAbsolutePath());
                    }
                    cmdBuilder.setLogFile(getLogDirectory());
                }

                if (getProtexScanMemory() != null) {
                    // Convert the # of GB to Bytes
                    Long mem = (long) (Math.floor(Math.pow(1024, 3) * getProtexScanMemory()));
                    // logger.debug("Using this many bytes as the memory : " + mem);
                    cmdBuilder.withMaxHeapSize(mem);
                }

                if (bdsToolJavaOptions != null) {
                    cmdBuilder.setBdsToolJavaOptions(bdsToolJavaOptions);
                }

                // force the scan if the boolean is set.
                cmdBuilder.force(isForceScan());

                // executing the bdstool command
                BlackDuckCommand blackDuckCommand = cmdBuilder.build();

                logger.info("Running Scan analyze command on " + localProtexProjectSourcePath);
                logger.debug("Command STATE : " + blackDuckCommand.state());

                // blackDuckCommand.addObserver(observer);
                blackDuckCommand.run();

                if (blackDuckCommand.state() == State.FAILED) {
                    // please check .bdstool.log file
                    throw new ProtexScannerException("BuildToolIntegrationException failed in Analyzing Project : " + projectId);
                }
                logger.info("End Of Log Scan .... " + localProtexProjectSourcePath);
            } catch (Fault e) {
                e.printStackTrace();
                throw new ProtexScannerException("BuildToolIntegrationException failed: " + e.getMessage(), e);
            } catch (BuildToolIntegrationException e) {
                if (e.getSuppressed() != null && e.getSuppressed().length != 0) {
                    for (Throwable e1 : e.getSuppressed()) {
                        logger.error("Suppressed error : " + e1.getMessage(), e1);
                    }
                }
                Throwable t = e;
                if ("An unknown error occurred".equals(e.getMessage())
                        && e.getCause() != null) {
                    t = e.getCause();
                }
                logger.error("BuildToolIntegrationException : " + t.getMessage());
                t.printStackTrace();
                throw new ProtexScannerException("BuildToolIntegrationException : " + t.getMessage(), t);
            }
            // End of Processing
            logger.info("Project files  " + localProtexProjectSourcePath + " Done !");
        } catch (ServerConnectionException e) {
            throw e;
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }
}
