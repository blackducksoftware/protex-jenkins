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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transports.http.configuration.ProxyServerType;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.blackducksoftware.integration.protex.ProtexFacade;
import com.blackducksoftware.integration.protex.ProtexFacadeException;
import com.blackducksoftware.integration.protex.ProtexServerInfo;
import com.blackducksoftware.integration.protex.ProtexServerInfoDescriptor;
import com.blackducksoftware.integration.protex.exceptions.ProtexCredentialsValidationException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConfigException;
import com.blackducksoftware.integration.protex.sdk.exceptions.ServerConnectionException;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;
import com.blackducksoftware.sdk.fault.ErrorCode;
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
import hudson.Plugin;
import hudson.PluginWrapper;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * Descriptor for {@link PostBuildProtexScan}. Used as a singleton. The
 * class is marked as public so that it can be accessed from views.
 *
 * <p>
 * See <tt>src/main/resources/hudson/plugins/hello_world/HelloWorldBuilder/*.jelly</tt> for the actual HTML fragment for
 * the configuration screen.
 */
@Extension
// This indicates to Jenkins that this is an implementation of an extension
// point.
public class PostBuildProtexScanDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

	private static final String FORM_SERVERS = "protexServers";

	private static final String FORM_SERVER_URL = "protexPostServerUrl";

	private static final String FORM_SERVER_NAME = "protexPostServerName";

	private static final String FORM_TIMEOUT = "protexPostServerTimeOut";

	private static final String SERVER_ID = "protexServerId";

	public static final String DEFAULT_TIMEOUT = "300";

	public static final Double DEFAULT_MEMORY = 2.0;

	private List<ProtexServerInfo> protexServers = new ArrayList<ProtexServerInfo>();

	/**
	 * In order to load the persisted global configuration, you have to call
	 * load() in the constructor.
	 */
	public PostBuildProtexScanDescriptor() {
		super(PostBuildProtexScan.class);
		load();
	}

	@Override
	public boolean isApplicable(final Class aClass) {
		// Indicates that this builder can be used with all kinds of project
		// types
		return true;
	}

	/**
	 * This human readable name is used in the configuration screen.
	 */
	@Override
	public String getDisplayName() {
		return Messages.ProtexPostScan_getDisplayName();
	}

	public String getPluginVersion() {
		final Plugin p = Jenkins.getInstance().getPlugin("protex-jenkins");
		final PluginWrapper pw = p.getWrapper();
		return pw.getVersion();
	}

	public Double getDefaultMemory() {
		return DEFAULT_MEMORY;
	}

	/**
	 * Code from https://github.com/jenkinsci/jenkins/blob/master/core/src/main/java/hudson/model/AbstractItem.java#L602
	 *
	 * @throws TransformerException
	 * @throws hudson.model.Descriptor.FormException
	 * @throws SAXException
	 * @throws ParserConfigurationException
	 */
	@WebMethod(name = "config.xml")
	public void doConfigDotXml(final StaplerRequest req, final StaplerResponse rsp)
			throws IOException, TransformerException, hudson.model.Descriptor.FormException, ParserConfigurationException, SAXException {
		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			if (req.getMethod().equals("GET")) {
				// read
				// checkPermission(EXTENDED_READ);
				rsp.setContentType("application/xml");
				IOUtils.copy(getConfigFile().getFile(), rsp.getOutputStream());
				return;
			}
			if (req.getMethod().equals("POST")) {
				// submission
				updateByXml(new StreamSource(req.getReader()));

				return;
			}

			// huh?
			rsp.sendError(javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);

		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
	}

	public void updateByXml(final Source source) throws IOException, TransformerException, ParserConfigurationException, SAXException {

		final TransformerFactory tFactory = TransformerFactory.newInstance();
		final Transformer transformer = tFactory.newTransformer();
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

		final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

		// StreamResult result = new StreamResult(new OutputStreamWriter(System.out, "UTF-8"));
		final StreamResult result = new StreamResult(byteOutput);
		transformer.transform(source, result);

		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final InputSource is = new InputSource(new StringReader(byteOutput.toString("UTF-8")));
		final Document doc = builder.parse(is);

		protexServers = new ArrayList<ProtexServerInfo>();

		if (doc.getElementsByTagName("protexServers").getLength() > 0) {
			final Node protexServersNode = doc.getElementsByTagName("protexServers").item(0);
			if (protexServersNode != null && protexServersNode.getNodeType() == Node.ELEMENT_NODE) {
				final Element protexServersElement = (Element) protexServersNode;
				if (protexServersElement.getElementsByTagName("com.blackducksoftware.integration.protex.ProtexServerInfo").getLength() > 0) {
					final NodeList serverInfoList = doc.getElementsByTagName("com.blackducksoftware.integration.protex.ProtexServerInfo");
					for (int i = 0; i < serverInfoList.getLength(); i++) {
						if (serverInfoList.item(i) != null && serverInfoList.item(i).getNodeType() == Node.ELEMENT_NODE) {
							final Element serverInfoElement = (Element) serverInfoList.item(i);

							final Node protexNameNode = serverInfoElement.getElementsByTagName("protexPostServerName").item(0);
							String serverName = "";
							if (protexNameNode != null && protexNameNode.getChildNodes() != null && protexNameNode.getChildNodes().item(0) != null) {
								serverName = protexNameNode.getChildNodes().item(0).getNodeValue();
								if (serverName != null) {
									serverName = serverName.trim();
								}
							}

							final Node protexUrlNode = serverInfoElement.getElementsByTagName("protexPostServerUrl").item(0);
							String serverUrl = "";
							if (protexUrlNode != null && protexUrlNode.getChildNodes() != null && protexUrlNode.getChildNodes().item(0) != null) {
								serverUrl = fixServerUrl(protexUrlNode.getChildNodes().item(0).getNodeValue());
								if (serverUrl == null) {
									serverUrl = "";
								}
							}
							final Node protexTimeoutNode = serverInfoElement.getElementsByTagName("protexPostServerTimeOut").item(0);
							String serverTimeout = "300"; // default timeout
							if (protexTimeoutNode != null && protexTimeoutNode.getChildNodes() != null && protexTimeoutNode.getChildNodes().item(0) != null) {
								serverTimeout = protexTimeoutNode.getChildNodes().item(0).getNodeValue();
								if (serverTimeout != null) {
									serverTimeout = serverTimeout.trim();
								}
							}

							final Node protexServerIdNode = serverInfoElement.getElementsByTagName("protexServerId").item(0);
							String protexServerId = "";
							if (protexServerIdNode != null && protexServerIdNode.getChildNodes() != null && protexServerIdNode.getChildNodes().item(0) != null) {
								protexServerId = protexServerIdNode.getChildNodes().item(0).getNodeValue();
								if (protexServerId != null) {
									protexServerId = protexServerId.trim();
								}
							}
							if (!StringUtils.isEmpty(serverName) && !StringUtils.isEmpty(serverUrl)) {
								final ProtexServerInfo server = new ProtexServerInfo(serverName, serverUrl,
										serverTimeout, protexServerId);
								if (!isServerPresent(protexServers, server)) {
									protexServers.add(server);
								}
							}
						}
					}
				}
			}
		}

		save();
	}

	// UUID.randomUUID().toString()

	@Override
	public boolean configure(final StaplerRequest req, final JSONObject formData)
			throws Descriptor.FormException {
		// To persist global configuration information,
		// set that to properties and call save().

		protexServers = new ArrayList<ProtexServerInfo>();
		final Object servers = formData.get(FORM_SERVERS);
		if (servers != null) {
			if (servers instanceof JSONObject) {
				// Only one server defined
				final JSONObject jsonObject = (JSONObject) servers;
				String protexPostServerName = (String) jsonObject.get(FORM_SERVER_NAME);
				if (protexPostServerName != null) {
					protexPostServerName = protexPostServerName.trim();
				}
				// Name is unique because this is the only defined server
				final String protexPostServerUrl = fixServerUrl((String) jsonObject.get(FORM_SERVER_URL));

				String protexPostServerTimeOut = (String) jsonObject.get(FORM_TIMEOUT);
				if (protexPostServerTimeOut != null) {
					protexPostServerTimeOut = protexPostServerTimeOut.trim();
				}
				if (protexPostServerTimeOut == null || protexPostServerTimeOut.equals("") || protexPostServerTimeOut.equals("0")) {
					protexPostServerTimeOut = DEFAULT_TIMEOUT;
				}

				String protexServerId = (String) jsonObject.get(SERVER_ID);
				if (protexServerId != null) {
					protexServerId = protexServerId.trim();
				}
				if ((!StringUtils.isEmpty(protexPostServerName) || !StringUtils.isEmpty(protexPostServerUrl)) && !StringUtils.isEmpty(protexPostServerUrl)) {
					final ProtexServerInfo server = new ProtexServerInfo(protexPostServerName, protexPostServerUrl,
							protexPostServerTimeOut, protexServerId);
					if (!isServerPresent(protexServers, server)) {
						protexServers.add(server);
					}
				}
			}
			if (servers instanceof JSONArray) {
				// Multiple servers defined
				final JSONArray jsonArray = (JSONArray) servers;
				for (final Object o : jsonArray) {
					final JSONObject jsonObject = (JSONObject) o;
					String protexPostServerName = (String) jsonObject.get(FORM_SERVER_NAME);
					if (protexPostServerName != null) {
						protexPostServerName = protexPostServerName.trim();
					}
					final String protexPostServerUrl = fixServerUrl((String) jsonObject.get(FORM_SERVER_URL));

					String protexPostServerTimeOut = (String) jsonObject.get(FORM_TIMEOUT);
					if (protexPostServerTimeOut != null) {
						protexPostServerTimeOut = protexPostServerTimeOut.trim();
					}
					if (protexPostServerTimeOut == null || protexPostServerTimeOut.equals("") || protexPostServerTimeOut.equals("0")) {
						protexPostServerTimeOut = DEFAULT_TIMEOUT;
					}
					String protexServerId = (String) jsonObject.get(SERVER_ID);
					if (protexServerId != null) {
						protexServerId = protexServerId.trim();
					}
					if ((!StringUtils.isEmpty(protexPostServerName) || !StringUtils.isEmpty(protexPostServerUrl)) && !StringUtils.isEmpty(protexPostServerUrl)) {
						final ProtexServerInfo server = new ProtexServerInfo(protexPostServerName, protexPostServerUrl,
								protexPostServerTimeOut, protexServerId);
						if (!isServerPresent(protexServers, server)) {
							protexServers.add(server);
						}
					}

				}
			}
		}

		// ^Can also use req.bindJSON(this, formData);
		// (easier when there are many fields; need set* methods for this,
		// like setUseFrench)
		save();
		return super.configure(req, formData);
	}

	private String fixServerUrl(final String serverUrl) {
		String newServerUrl = StringUtils.trimToNull(serverUrl);
		if (newServerUrl != null) {
			String urlPath = null;
			try {
				final URL serverURL = new URL(newServerUrl);
				urlPath = serverURL.getPath();
			} catch (final MalformedURLException e) {
			}
			if (StringUtils.isNotBlank(urlPath)) {
				newServerUrl = newServerUrl.substring(0, newServerUrl.indexOf(urlPath));
			}
		}
		return newServerUrl;
	}

	private boolean isServerPresent(final List<ProtexServerInfo> protexServerList, final ProtexServerInfo serverToLookFor) {
		boolean serverIsPresent = false;
		if (!protexServerList.isEmpty()) {
			for (final ProtexServerInfo currServer : protexServerList) {
				boolean sameName = false;
				boolean sameUrl = false;
				if (currServer.getProtexPostServerName() != null) {
					if (currServer.getProtexPostServerName().equals(serverToLookFor.getProtexPostServerName())) {
						sameName = true;
					}
				} else {
					if (serverToLookFor.getProtexPostServerName() == null) {
						sameName = true;
					}
				}
				if (currServer.getProtexPostServerUrl() != null) {
					if (currServer.getProtexPostServerUrl().equals(serverToLookFor.getProtexPostServerUrl())) {
						sameUrl = true;
					}
				} else {
					if (serverToLookFor.getProtexPostServerUrl() == null) {
						sameUrl = true;
					}
				}

				if ((sameName && sameUrl) || sameUrl) {
					serverIsPresent = true;
					return serverIsPresent;
				}
			}
		} else {
			return serverIsPresent;
		}

		return serverIsPresent;
	}

	public List<ProtexServerInfo> getProtexServers() {
		return protexServers;
	}

	public ListBoxModel doFillProtexServerIdItems() {

		final ListBoxModel boxModel = new ListBoxModel();
		boxModel.add("- none -", "");
		if (!getProtexServers().isEmpty()) {
			for (final ProtexServerInfo server : getProtexServers()) {
				boxModel.add(server.getProtexPostServerName() + "  (" + server.getProtexPostServerUrl() + ")", server.getProtexServerId());
			}
		}
		return boxModel;
	}

	public ListBoxModel doFillProtexPostCredentialsItems() {

		ListBoxModel boxModel = null;

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
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

	/**
	 * Performs on-the-fly validation of the form field
	 *
	 * @param value
	 *            This parameter receives the value that the user has typed.
	 * @return Indicates the outcome of the validation. This is sent to the
	 *         browser.
	 */
	public FormValidation doCheckProtexServerId(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials) throws IOException,
	ServletException {
		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			if (protexServerId == null || protexServerId.length() == 0) {
				return FormValidation
						.error(Messages.ProtexPostScan_getPleaseChooseProtexServer());
			}
			ProtexServerInfo currentServer = null;
			for (final ProtexServerInfo server : getProtexServers()) {
				if (server.getProtexServerId().equals(protexServerId)) {
					currentServer = server;
					break;
				}
			}
			if (currentServer == null) {
				// User selected a blank entry from the drop-down list Ex: '- none -'
				return FormValidation
						.error(Messages.ProtexPostScan_getPleaseChooseProtexServer());
			}
			URL url;
			try {
				url = new URL(currentServer.getProtexPostServerUrl());
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
											return new PasswordAuthentication(
													proxyConfig.getUserName(), proxyConfig.getPassword().toCharArray());
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
				return FormValidation.warning(ioe, Messages
						.ProtexPostScan_getCanNotReachThisServer_0_(ioe.toString()));
			} catch (final RuntimeException e) {
				return FormValidation.error(e, Messages
						.ProtexPostScan_getNotAValidUrl());
			}

		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
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

	public FormValidation doCheckProtexPostCredentials(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials) throws IOException,
	ServletException {

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			if (protexPostCredentials == null || protexPostCredentials.length() == 0) {
				return FormValidation
						.error(Messages.ProtexPostScan_getNoCredentialsSelected());
			}

			final AbstractProject<?, ?> nullProject = null;
			final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
					nullProject, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());
			final IdMatcher matcher = new IdMatcher(protexPostCredentials);
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

			ProtexServerInfo currentServer = null;
			for (final ProtexServerInfo server : getProtexServers()) {
				if (server.getProtexServerId().equals(protexServerId)) {
					currentServer = server;
					break;
				}
			}
			if (currentServer == null) {
				// IJP-118 This error should be caught by the server url validation
				return FormValidation.ok();
			}

			try {
				Long timeout = null;
				if (!StringUtils.isEmpty(currentServer.getProtexPostServerTimeOut())) {
					timeout = Long.valueOf(currentServer.getProtexPostServerTimeOut());
				} else {
					timeout = Long.valueOf(ProtexServerInfoDescriptor.DEFAULT_TIMEOUT);
				}
				final ProtexFacade facade = new ProtexFacade(currentServer.getProtexPostServerUrl(), credentialUserName,
						credentialPassword, timeout);

				final Jenkins jenkins = Jenkins.getInstance();
				if (jenkins != null && jenkins.proxy != null) {
					final URL url = new URL(currentServer.getProtexPostServerUrl());
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
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.error(e.getMessage());
			} catch (final ServerConfigException e) {
				// IJP-118
				return FormValidation.ok();
			} catch (final ServerConnectionException e) {
				// IJP-118
				return FormValidation.ok();
			} catch (final IllegalArgumentException e) {
				// IJP-118
				return FormValidation.ok();
			} catch (final MalformedURLException e) {
				// IJP-118
				return FormValidation.ok();
			} catch (final Exception e) {
				// IJP-118 Only showing relevant errors to this field
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
			}
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
		return FormValidation.okWithMarkup(Messages.ProtexPostScan_getCredentialsAreValid());
	}

	public FormValidation doCheckProtexPostProjectName(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials,
			@QueryParameter("protexPostProjectName") final String protexPostProjectName) throws IOException,
	ServletException {

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			if (protexPostProjectName.length() == 0) {
				return FormValidation
						.error(Messages.ProtexPostScan_getPleaseProvideProjectName());
			}
			if (protexPostProjectName.length() >= 250) {
				return FormValidation
						.error(Messages.ProtexPostScan_getProjectNameTooLong());
			}

			if (protexPostProjectName.contains("$")) {
				return FormValidation.warningWithMarkup(Messages.ProtexPostScan_getProjectNameCreateAtRuntime());
			}

			// ClassLoader originalClassLoader = Thread.currentThread()
			// .getContextClassLoader();
			// boolean changed = false;
			try {

				// if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				// changed = true;
				// Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
				// }
				if (StringUtils.isEmpty(protexPostCredentials)) {
					// IJP-118 should be caught by credentials field
					return FormValidation.ok();
				}

				final AbstractProject<?, ?> nullProject = null;
				final List<StandardUsernamePasswordCredentials> credentials =
						CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
								nullProject, ACL.SYSTEM,
								Collections.<DomainRequirement> emptyList());
				final IdMatcher matcher = new IdMatcher(protexPostCredentials);
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
				if (StringUtils.isEmpty(credentialUserName) || StringUtils.isEmpty(credentialPassword)) {
					// IJP-118 should be caught by credentials field
					return FormValidation.ok();
				}

				ProtexServerInfo currentServer = null;
				for (final ProtexServerInfo server : getProtexServers()) {
					if (server.getProtexServerId().equals(protexServerId)) {
						currentServer = server;
						break;
					}
				}
				if (StringUtils.isEmpty(protexServerId) || currentServer == null) {
					// IJP-118
					return FormValidation.ok();
				}

				final ProtexFacade protexFacade = getProtexFacade(currentServer.getProtexPostServerUrl(), credentialUserName, credentialPassword,
						Long.valueOf(currentServer.getProtexPostServerTimeOut()), LogLevel.TRACE);

				if (protexFacade.checkProjectExists(protexPostProjectName.trim())) {
					return FormValidation.okWithMarkup(Messages.ProtexPostScan_getProjectAlreadyExists());
				} else {
					return FormValidation.warning(Messages.ProtexPostScan_getProjectDoesNotExist());
				}
			} catch (final ProtexFacadeException e) {
				if (e.getSdkFaultErrorCode() != null) {
					if (e.getSdkFaultErrorCode() == ErrorCode.INVALID_CREDENTIALS) {
						// IJP-118 Only showing relevant errors to this field
						// Should be caught by the credentials field
						return FormValidation.ok();
					}
				}

				return FormValidation.error(e.getMessage());
			} catch (final IllegalArgumentException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final ServerConfigException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final ServerConnectionException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final MalformedURLException e) {
				// IJP-118
				return FormValidation.ok();
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
			}
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
	}

	public FormValidation doCheckProtexPostTemplateProjectName(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials,
			@QueryParameter("protexPostTemplateProjectName") final String protexPostTemplateProjectName) throws IOException,
	ServletException {

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			if (protexPostTemplateProjectName.length() == 0) {
				return FormValidation
						.okWithMarkup(Messages.ProtexPostScan_getOptionalProjectCloneName());
			}
			if (protexPostTemplateProjectName.contains("$")) {
				return FormValidation.warningWithMarkup(Messages.ProtexPostScan_getProjectTemplateNameCreateAtRuntime());
			}

			try {

				if (StringUtils.isEmpty(protexPostCredentials)) {
					// IJP-118 should be caught by credentials field
					return FormValidation.ok();
				}

				final AbstractProject<?, ?> nullProject = null;
				final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
						nullProject, ACL.SYSTEM,
						Collections.<DomainRequirement> emptyList());
				final IdMatcher matcher = new IdMatcher(protexPostCredentials);
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
				if (StringUtils.isEmpty(credentialUserName) || StringUtils.isEmpty(credentialPassword)) {
					// IJP-118 should be caught by credentials field
					return FormValidation
							.ok();
				}

				ProtexServerInfo currentServer = null;
				for (final ProtexServerInfo server : getProtexServers()) {
					if (server.getProtexServerId().equals(protexServerId)) {
						currentServer = server;
						break;
					}
				}
				if (StringUtils.isEmpty(protexServerId) || currentServer == null) {
					// IJP-118
					return FormValidation.ok();
				}

				final ProtexFacade protexFacade = getProtexFacade(currentServer.getProtexPostServerUrl(), credentialUserName, credentialPassword,
						Long.valueOf(currentServer.getProtexPostServerTimeOut()), LogLevel.TRACE);

				if (protexFacade.checkProjectExists(protexPostTemplateProjectName.trim())) {
					return FormValidation.okWithMarkup(Messages.ProtexPostScan_getProjectAlreadyExists());
				} else {
					return FormValidation.error(Messages.ProtexPostScan_getTemplateProjectDoesNotExist());
				}
			} catch (final ProtexFacadeException e) {
				if (e.getSdkFaultErrorCode() != null) {
					if (e.getSdkFaultErrorCode() == ErrorCode.INVALID_CREDENTIALS) {
						// IJP-118 Only showing relevant errors to this field
						// Should be caught by the credentials field
						return FormValidation.ok();
					}
				}

				return FormValidation.error(e.getMessage());
			} catch (final IllegalArgumentException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final ServerConfigException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final ServerConnectionException e) {
				// IJP-118 Only showing relevant errors to this field
				return FormValidation.ok();
			} catch (final MalformedURLException e) {
				// IJP-118
				return FormValidation.ok();
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
			}
		} finally {
			if (changed) {
				Thread.currentThread().setContextClassLoader(
						originalClassLoader);
			}
		}
	}

	public FormValidation doCreateProject(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials,
			@QueryParameter("protexPostProjectName") final String protexPostProjectName,
			@QueryParameter("protexPostTemplateProjectName") final String protexPostTemplateProjectName) {

		final ClassLoader originalClassLoader = Thread.currentThread()
				.getContextClassLoader();
		boolean changed = false;
		try {

			if (PostBuildProtexScanDescriptor.class.getClassLoader() != originalClassLoader) {
				changed = true;
				Thread.currentThread().setContextClassLoader(PostBuildProtexScanDescriptor.class.getClassLoader());
			}

			// We indent the messages provided to differentiate the error from the errors from other fields
			// The alternative is to alter the fields with java script, but this close the Release
			// I'd rather not implement a complex solution

			if (StringUtils.isEmpty(protexServerId)) {
				return FormValidation.error(Messages.ProtexPostScan_getProjectCreateFailed());
			}
			if (StringUtils.isEmpty(protexPostCredentials)) {
				return FormValidation.error(Messages.ProtexPostScan_getProjectCreateFailed());
			}
			if (StringUtils.isEmpty(protexPostProjectName)) {
				return FormValidation.error(Messages.ProtexPostScan_getProjectCreateFailed());
			}

			if (protexPostProjectName.contains("$")) {
				return FormValidation.warningWithMarkup(Messages.ProtexPostScan_getProjectNameCreateAtRuntime());
			} else if (protexPostTemplateProjectName.contains("$")) {
				return FormValidation.warningWithMarkup(Messages.ProtexPostScan_getProjectTemplateNameCreateAtRuntime());
			}

			final AbstractProject<?, ?> nullProject = null;
			final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
					nullProject, ACL.SYSTEM,
					Collections.<DomainRequirement> emptyList());
			final IdMatcher matcher = new IdMatcher(protexPostCredentials);
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
			ProtexServerInfo currentServer = null;
			for (final ProtexServerInfo server : getProtexServers()) {
				if (server.getProtexServerId().equals(protexServerId)) {
					currentServer = server;
					break;
				}
			}
			if (StringUtils.isEmpty(protexServerId) || currentServer == null) {
				return FormValidation.error(Messages.ProtexPostScan_getPleaseChooseProtexServer());
			}

			final ProtexFacade protexFacade = getProtexFacade(currentServer.getProtexPostServerUrl(), credentialUserName, credentialPassword,
					Long.valueOf(currentServer.getProtexPostServerTimeOut()), LogLevel.TRACE);

			protexFacade.createProtexProject(protexPostProjectName.trim(), protexPostTemplateProjectName.trim());
		} catch (final ProtexFacadeException e) {
			if (e.getSdkFaultErrorCode() == null && e.getSdkFaultMessage() == null && e.getMessage().contains("already exists")) {
				return FormValidation.ok(e.getMessage());
			} else if (e.getSdkFaultErrorCode() == ErrorCode.INVALID_CREDENTIALS) {
				return FormValidation.error(Messages.ProtexPostScan_getProjectCreateFailed());
			} else if (e.getSdkFaultErrorCode() == ErrorCode.PROJECT_NOT_FOUND) {
				return FormValidation.error(Messages.ProtexPostScan_getProjectCreateFailed());
			}
			return FormValidation.error(e.getMessage());
		} catch (final IllegalArgumentException e) {
			return FormValidation.error(e.getMessage());
		} catch (final ServerConfigException e) {
			return FormValidation.error(e.getMessage());
		} catch (final ServerConnectionException e) {
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
		} finally {
			// if (changed) {
			// Thread.currentThread().setContextClassLoader(
			// originalClassLoader);
			// }
		}
		return FormValidation.ok(Messages.ProtexPostScan_getProjectCreatedSuccessfully_0_(protexPostProjectName));
	}

	public FormValidation doCheckProtexScanMemory(@QueryParameter("protexScanMemory") final Double protexScanMemory) throws IOException,
	ServletException {
		if (protexScanMemory == null) {
			return FormValidation
					.error(Messages.ProtexPostScan_getProtexScanMemoryEmpty());
		}
		if (protexScanMemory < 2) {
			return FormValidation
					.warning(Messages.ProtexPostScan_getProtexScanMemoryTooLowWarning());
		}

		// if (protexScanMemory > 20) {
		// return FormValidation
		// .warning(Messages.ProtexPostScan_getProtexScanMemoryTooHighWarning());
		// }

		return FormValidation.ok();
	}

	public FormValidation doCheckProtexPostProjectSourcePath(
			@QueryParameter final String value) throws IOException,
	ServletException {
		if (value.length() == 0) {
			return FormValidation
					.warning(Messages.ProtexPostScan_getProjectSourcePathWarning());
		}
		return FormValidation.ok();
	}

	public FormValidation doCheckProtexReportTemplate(@QueryParameter("protexServerId") final String protexServerId,
			@QueryParameter("protexPostCredentials") final String protexPostCredentials,
			@QueryParameter("protexReportTemplate") final String protexReportTemplate) throws IOException,
	ServletException {
		if (StringUtils.isBlank(protexReportTemplate)) {
			return FormValidation.ok();
		}

		try {

			if (StringUtils.isEmpty(protexPostCredentials)) {
				// IJP-118 should be caught by credentials field
				return FormValidation.ok();
			}

			final AbstractProject<?, ?> nullProject = null;
			final List<StandardUsernamePasswordCredentials> credentials =
					CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class,
							nullProject, ACL.SYSTEM,
							Collections.<DomainRequirement> emptyList());
			final IdMatcher matcher = new IdMatcher(protexPostCredentials);
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
			if (StringUtils.isEmpty(credentialUserName) || StringUtils.isEmpty(credentialPassword)) {
				// IJP-118 should be caught by credentials field
				return FormValidation.ok();
			}

			ProtexServerInfo currentServer = null;
			for (final ProtexServerInfo server : getProtexServers()) {
				if (server.getProtexServerId().equals(protexServerId)) {
					currentServer = server;
					break;
				}
			}
			if (StringUtils.isEmpty(protexServerId) || currentServer == null) {
				// IJP-118 Should be caught by the server selection field
				return FormValidation.ok();
			}

			final ProtexFacade protexFacade = getProtexFacade(currentServer.getProtexPostServerUrl(), credentialUserName, credentialPassword,
					Long.valueOf(currentServer.getProtexPostServerTimeOut()), LogLevel.TRACE);

			protexFacade.getReportTemplate(protexReportTemplate);

			return FormValidation.okWithMarkup(Messages.ProtexPostScan_getReportTemplateExists());
		} catch (final ProtexFacadeException e) {
			return FormValidation.error(e.getMessage());
		} catch (final IllegalArgumentException e) {
			// IJP-118 Only showing relevant errors to this field
			return FormValidation.ok();
		} catch (final ServerConfigException e) {
			// IJP-118 Only showing relevant errors to this field
			return FormValidation.ok();
		} catch (final ServerConnectionException e) {
			// IJP-118 Only showing relevant errors to this field
			return FormValidation.ok();
		} catch (final MalformedURLException e) {
			// IJP-118 Only showing relevant errors to this field
			return FormValidation.ok();
		} catch (final Exception e) {
			e.printStackTrace(System.err);
			// We try to get the root cause of the exception because we are not printing the entire stack trace
			// to the UI
			// We just want to print the relevant exeception allong with the error message
			if (e.getCause() != null) {
				if (e.getCause().getCause() != null) {
					return FormValidation.error(e.getCause().getCause().toString());
				} else {
					return FormValidation.error(e.getCause().toString());
				}
			} else {
				return FormValidation.error(e.toString());
			}
		}
	}

	public ProtexFacade getProtexFacade(final String protexUrl, final String userName, final String password, final Long timeout, final LogLevel loggingLevel) throws InvalidKeyException,
	NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, BadPaddingException, IOException, ServerConfigException {

		final ProtexFacade facade = new ProtexFacade(protexUrl, userName,
				password, timeout);
		// facade.setAutoContextLoading(true);
		facade.setLogger(new ProtexJenkinsLogger(null));

		final Jenkins jenkins = Jenkins.getInstance();
		if (jenkins != null && jenkins.proxy != null) {
			final URL url = new URL(protexUrl);
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

}
