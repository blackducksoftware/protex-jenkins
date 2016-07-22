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

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.kohsuke.stapler.QueryParameter;

import com.blackducksoftware.integration.protex.exceptions.ProtexCredentialsValidationException;
import com.blackducksoftware.integration.protex.jenkins.Messages;
import com.blackducksoftware.integration.protex.jenkins.PostBuildProtexScanDescriptor;
import com.blackducksoftware.integration.protex.jenkins.ProtexJenkinsLogger;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;

@Extension
public class ProtexServerInfoDescriptor extends Descriptor<ProtexServerInfo> {

    public static final String DEFAULT_TIMEOUT = PostBuildProtexScanDescriptor.DEFAULT_TIMEOUT;

    /**
     * In order to load the persisted global configuration, you have to call
     * load() in the constructor.
     */
    public ProtexServerInfoDescriptor() {
        super(ProtexServerInfo.class);
        load();
    }

    public String getDEFAULT_TIMEOUT() {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public String getDisplayName() {
        return "";
    }

    public FormValidation doCheckProtexPostServerName(@QueryParameter("protexPostServerName") final String protexPostServerName)
            throws IOException, ServletException {
        if (protexPostServerName == null || protexPostServerName.length() == 0) {
            return FormValidation.error(Messages.ProtexPostScan_getPleaseSetServerName());
        }
        return FormValidation.ok();
    }

    private void attemptResetProxyCache() {
        try {
            // works, and resets the cache when using sun classes
            // sun.net.www.protocol.http.AuthCacheValue.setAuthCache(new
            // sun.net.www.protocol.http.AuthCacheImpl());

            // Attempt the same thing using reflection in case they are not using a jdk with sun classes

            Class<?> sunAuthCacheValue;
            Class<?> sunAuthCache;
            Class<?> sunAuthCacheImpl;
            try {
                sunAuthCacheValue = Class.forName("sun.net.www.protocol.http.AuthCacheValue");
                sunAuthCache = Class.forName("sun.net.www.protocol.http.AuthCache");
                sunAuthCacheImpl = Class.forName("sun.net.www.protocol.http.AuthCacheImpl");
            } catch (final Exception e) {
                // Must not be using a JDK with sun classes so we abandon this reset since it is sun specific
                return;
            }

            final java.lang.reflect.Method m = sunAuthCacheValue.getDeclaredMethod("setAuthCache", sunAuthCache);

            final Constructor<?> authCacheImplConstr = sunAuthCacheImpl.getConstructor();
            final Object authCachImp = authCacheImplConstr.newInstance();

            m.invoke(null, authCachImp);

        } catch (final Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public FormValidation doCheckProtexPostServerUrl(@QueryParameter final String value)
            throws IOException, ServletException {
        if (value == null || value.length() == 0) {
            return FormValidation.error(Messages.ProtexPostScan_getPleaseSetServer());
        }
        URL url;
        try {
            url = new URL(value);
            try {
                url.toURI();
            } catch (final URISyntaxException e) {
                return FormValidation.error(Messages
                        .ProtexPostScan_getNotAValidUrl());
            }
        } catch (final MalformedURLException e) {
            return FormValidation.error(Messages
                    .ProtexPostScan_getNotAValidUrl());
        }
        try {
            attemptResetProxyCache();
            Proxy proxy = null;
            final ProxyConfiguration proxyConfig = Jenkins.getInstance().proxy;
            if (proxyConfig != null) {
                if (StringUtils.isNotBlank(proxyConfig.name) && proxyConfig.port != -1) {
                    proxy = new Proxy(Type.HTTP, InetSocketAddress.createUnresolved(proxyConfig.name, proxyConfig.port));
                    if (StringUtils.isNotBlank(proxyConfig.getUserName())
                            && StringUtils.isNotBlank(proxyConfig.getPassword())) {
                        Authenticator.setDefault(
                                new Authenticator() {
                                    @Override
                                    public PasswordAuthentication getPasswordAuthentication() {
                                        if (getRequestorType().equals(RequestorType.PROXY)) {
                                            return new PasswordAuthentication(
                                                    proxyConfig.getUserName(), proxyConfig.getPassword().toCharArray());
                                        }
                                        return null;
                                    }
                                }
                                );
                    } else {
                        Authenticator.setDefault(null);
                    }
                }
            }
            URLConnection connection = null;
            if (proxy != null) {
                connection = url.openConnection(proxy);
            } else {
                connection = url.openConnection();
            }

            connection.getContent();
        } catch (final IOException ioe) {
            return FormValidation.warning(Messages
                    .ProtexPostScan_getCanNotReachThisServer_0_(ioe.toString()));
        } catch (final RuntimeException e) {
            return FormValidation.error(Messages
                    .ProtexPostScan_getNotAValidUrl());
        }

        return FormValidation.ok();
    }

    public ListBoxModel doFillProtexTestCredentialsIdItems() {

        ListBoxModel boxModel = null;

        final ClassLoader originalClassLoader = Thread.currentThread()
                .getContextClassLoader();
        boolean changed = false;
        try {

            if (ProtexServerInfoDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(ProtexServerInfoDescriptor.class.getClassLoader());
            }
            final CredentialsMatcher credentialsMatcher = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
            // Dont want to limit the search to a particular project for the drop down menu
            final AbstractProject<?, ?> project = null;
            boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher,
                    CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList()));
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(
                        originalClassLoader);
            }
        }
        return boxModel;
    }

    public FormValidation doCheckProtexPostServerTimeOut(@QueryParameter final String value)
            throws IOException, ServletException {
        if (value.length() == 0) {
            return FormValidation.error(Messages.ProtexPostScan_getPleaseSetTimeout());
        }
        Integer i = 0;
        try {
            i = Integer.valueOf(value);
        } catch (final NumberFormatException e) {
            return FormValidation
                    .error(Messages.ProtexPostScan_getTimeoutMustBeInteger());
        }
        if (i.equals(0)) {
            return FormValidation
                    .error(Messages.ProtexPostScan_getTimeoutGreaterThanOne());
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckProtexTestCredentialsId(@QueryParameter final String protexTestCredentialsId)
            throws IOException, ServletException {

        if (protexTestCredentialsId.length() == 0) {
            return FormValidation.ok();
        }

        final AbstractProject<?, ?> nullProject = null;
        final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                nullProject, ACL.SYSTEM,
                Collections.<DomainRequirement> emptyList());
        final IdMatcher matcher = new IdMatcher(protexTestCredentialsId);
        String credentialUserName = null;
        String credentialPassword = null;
        for (final StandardCredentials c : credentials) {
            if (matcher.matches(c)) {
                if (c instanceof UsernamePasswordCredentialsImpl) {
                    final UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                    credentialUserName = credential.getUsername();
                    credentialPassword = credential.getPassword().getPlainText();
                }
            }
        }
        if (StringUtils.isEmpty(credentialUserName) && StringUtils.isEmpty(credentialPassword)) {
            return FormValidation
                    .error(Messages.ProtexPostScan_getNoCredentialsSelected());
        }
        if (StringUtils.isEmpty(credentialUserName)) {
            return FormValidation
                    .error(Messages.ProtexPostScan_getNoUserNameProvided());
        }
        if (StringUtils.isEmpty(credentialPassword)) {
            return FormValidation
                    .error(Messages.ProtexPostScan_getNoPasswordProvided());
        }

        // This is commented out becasue we should accept UserNames other than email addresses
        // This was pointed out by Olga in IJP-32
        // if (!credentialUserName.matches("(.*@.*){1,1}")) {
        // return FormValidation
        // .warning("Malformed login, Protex Login should be an email");
        // }
        return FormValidation.ok();
    }

    public FormValidation doTestConnection(@QueryParameter("protexPostServerUrl") final String protexServerUrl,
            @QueryParameter("protexTestCredentialsId") final String protexTestCredentialsId,
            @QueryParameter("protexPostServerTimeOut") final String protexPostServerTimeOut) {

        // ClassLoader originalClassLoader = Thread.currentThread()
        // .getContextClassLoader();
        // boolean changed = false;
        try {
            // if (ProtexServerInfoDescriptor.class.getClassLoader() != originalClassLoader) {
            // changed = true;
            // Thread.currentThread().setContextClassLoader(ProtexServerInfoDescriptor.class.getClassLoader());
            // }

            if (StringUtils.isEmpty(protexServerUrl)) {
                return FormValidation
                        .error(Messages.ProtexPostScan_getPleaseSetServer());
            }

            if (StringUtils.isEmpty(protexTestCredentialsId)) {
                return FormValidation
                        .error(Messages.ProtexPostScan_getNoCredentialsSelected());
            }

            final AbstractProject<?, ?> nullProject = null;
            final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
                    nullProject, ACL.SYSTEM,
                    Collections.<DomainRequirement> emptyList());
            final IdMatcher matcher = new IdMatcher(protexTestCredentialsId);
            String credentialUserName = null;
            String credentialPassword = null;
            for (final StandardCredentials c : credentials) {
                if (matcher.matches(c)) {
                    if (c instanceof UsernamePasswordCredentialsImpl) {
                        final UsernamePasswordCredentialsImpl credential = (UsernamePasswordCredentialsImpl) c;
                        credentialUserName = credential.getUsername();
                        credentialPassword = credential.getPassword().getPlainText();
                    }
                }
            }

            if (StringUtils.isEmpty(credentialUserName) && StringUtils.isEmpty(credentialPassword)) {
                return FormValidation
                        .error(Messages.ProtexPostScan_getNoCredentialsSelected());
            }
            if (StringUtils.isEmpty(credentialUserName)) {
                return FormValidation
                        .error(Messages.ProtexPostScan_getNoUserNameProvided());
            }
            if (StringUtils.isEmpty(credentialPassword)) {
                return FormValidation
                        .error(Messages.ProtexPostScan_getNoPasswordProvided());
            }

            Long timeout = null;
            if (!StringUtils.isEmpty(protexPostServerTimeOut)) {
                timeout = Long.valueOf(protexPostServerTimeOut);
            } else {
                timeout = Long.valueOf(getDEFAULT_TIMEOUT());
            }
            final ProtexFacade facade = new ProtexFacade(protexServerUrl, credentialUserName,
                    credentialPassword, timeout);

            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null && jenkins.proxy != null) {
                final URL url = new URL(protexServerUrl);
                final Proxy proxy = ProxyConfiguration.createProxy(url.getHost(), jenkins.proxy.name, jenkins.proxy.port,
                        jenkins.proxy.noProxyHost);
                if (proxy.address() != null) {
                    final InetSocketAddress proxyAddress = (InetSocketAddress) proxy.address();
                    if (!StringUtils.isEmpty(proxyAddress.getHostName()) && proxyAddress.getPort() != 0) {

                        if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
                            facade.setProxySettings(proxyAddress.getHostName(), proxyAddress.getPort(), ProxyServerType.HTTP, true,
                                    jenkins.proxy.getUserName(),
                                    jenkins.proxy.getPassword());
                        } else {
                            facade.setProxySettings(proxyAddress.getHostName(), proxyAddress.getPort(), ProxyServerType.HTTP, true);
                        }
                    }

                }
            }
            facade.setLogger(new ProtexJenkinsLogger(null));

            facade.validateConnection();
        } catch (final ProtexCredentialsValidationException e) {

            return FormValidation.error(e.getMessage());

        } catch (final ServerConfigException e) {
            return FormValidation.error(e.getMessage());
        } catch (final ServerConnectionException e) {
            return FormValidation.error(e.getMessage());

        } catch (final IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
        } catch (final Exception e) {
            e.printStackTrace(System.err);
            if (e.getCause() != null) {
                if (e.getCause().getCause() != null) {
                    return FormValidation.error(e.getCause().getCause().toString());
                } else {
                    return FormValidation.error(e.getCause().toString());
                }
            } else {
                return FormValidation.error(e.toString());
            }
            // } finally {
            // if (changed) {
            // Thread.currentThread().setContextClassLoader(
            // originalClassLoader);
            // }
        }
        return FormValidation.ok(Messages.ProtexPostScan_getConnectionSuccessful());
    }
}
