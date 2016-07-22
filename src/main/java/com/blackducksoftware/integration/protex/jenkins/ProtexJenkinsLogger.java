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

import hudson.model.BuildListener;

import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import com.blackducksoftware.integration.suite.sdk.logging.IntLogger;
import com.blackducksoftware.integration.suite.sdk.logging.LogLevel;

public class ProtexJenkinsLogger implements IntLogger, Serializable {

    private final BuildListener jenkinsLogger;

    private LogLevel level = LogLevel.INFO; // default is INFO

    public ProtexJenkinsLogger(BuildListener jenkinsLogger) {
        this.jenkinsLogger = jenkinsLogger;
    }

    public BuildListener getJenkinsListener() {
        return jenkinsLogger;
    }

    @Override
    public void setLogLevel(LogLevel level) {
        this.level = level;
    }

    @Override
    public LogLevel getLogLevel() {
        return level;
    }

    @Override
    public void debug(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.DEBUG)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[DEBUG] " + txt);
                } else {
                    System.out.println("[DEBUG] " + txt);
                }
            }
        }
    }

    @Override
    public void debug(String txt, Throwable t) {
        if (LogLevel.isLoggable(level, LogLevel.DEBUG)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[DEBUG] " + txt);
                } else {
                    System.err.println("[DEBUG] " + txt);
                }
            }
            if (t != null) {
                StringWriter sw = new StringWriter();
                t.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[DEBUG] " + sw.toString());
                } else {
                    System.err.println("[DEBUG] " + sw.toString());
                }
            }
        }
    }

    @Override
    public void error(Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[ERROR] " + sw.toString());
                } else {
                    System.err.println("[ERROR] " + sw.toString());
                }
            }
        }
    }

    @Override
    public void error(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[ERROR] " + txt);
                } else {
                    System.err.println("[ERROR] " + txt);
                }

            }
        }
    }

    @Override
    public void error(String txt, Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.ERROR)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[ERROR] " + txt);
                } else {
                    System.err.println("[ERROR] " + txt);
                }
            }
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.error("[ERROR] " + sw.toString());
                } else {
                    System.err.println("[ERROR] " + sw.toString());
                }
            }
        }
    }

    @Override
    public void info(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.INFO)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[INFO] " + txt);
                } else {
                    System.out.println("[INFO] " + txt);
                }
            }
        }
    }

    @Override
    public void trace(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.TRACE)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[TRACE] " + txt);
                } else {
                    System.out.println("[TRACE] " + txt);
                }
            }
        }
    }

    @Override
    public void trace(String txt, Throwable e) {
        if (LogLevel.isLoggable(level, LogLevel.TRACE)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[TRACE] " + txt);
                } else {
                    System.out.println("[TRACE] " + txt);
                }
            }
            if (e != null) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[TRACE] " + sw.toString());
                } else {
                    System.out.println("[TRACE] " + sw.toString());
                }
            }
        }
    }

    @Override
    public void warn(String txt) {
        if (LogLevel.isLoggable(level, LogLevel.WARN)) {
            if (txt != null) {
                if (jenkinsLogger != null) {
                    jenkinsLogger.getLogger().println("[WARN] " + txt);
                } else {
                    System.out.println("[WARN] " + txt);
                }
            }
        }
    }

}
