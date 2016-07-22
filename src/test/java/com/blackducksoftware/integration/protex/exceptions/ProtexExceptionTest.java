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
package com.blackducksoftware.integration.protex.exceptions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProtexExceptionTest {
    private Class<? extends Exception> protexExceptionClass;

    public ProtexExceptionTest(Class<? extends Exception> protexExceptionClass) {
        this.protexExceptionClass = protexExceptionClass;
    }

    @Parameters
    public static List<Object[]> getExceptionClassesToTest() {
        List<Object[]> exceptionClassesToTest = new ArrayList<Object[]>();

        exceptionClassesToTest.add(new Object[] { ProtexValidationException.class });
        exceptionClassesToTest.add(new Object[] { ProtexScannerException.class });
        exceptionClassesToTest.add(new Object[] { ProtexCredentialsValidationException.class });

        return exceptionClassesToTest;
    }

    @Test
    public void testProtexException_DefaultConstructor() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        Exception e = protexExceptionClass.getConstructor().newInstance();
        Assert.assertNull(e.getMessage());
        Assert.assertNull(e.getCause());
    }

    @Test
    public void testProtexException_Constructor_withMessage() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        String message = "test message";

        Exception e = protexExceptionClass.getConstructor(String.class).newInstance(message);
        Assert.assertNotNull(e.getMessage());
        Assert.assertEquals("test message", e.getMessage());
        Assert.assertNull(e.getCause());
    }

    @Test
    public void testProtexException_Constructor_withThrowable() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        Throwable t = new Throwable("test throwable message");

        Exception e = protexExceptionClass.getConstructor(Throwable.class).newInstance(t);
        Assert.assertNotNull(e.getMessage());
        Assert.assertEquals("java.lang.Throwable: test throwable message", e.getMessage());
        Assert.assertNotNull(e.getCause());
        Assert.assertEquals("test throwable message", e.getCause().getMessage());
    }

    @Test
    public void testProtexException_Constructor_withMessageAndThrowable() throws InstantiationException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException, NoSuchMethodException, SecurityException {
        String message = "test message";
        Throwable t = new Throwable("test throwable message");

        Exception e = protexExceptionClass.getConstructor(String.class, Throwable.class).newInstance(message, t);
        Assert.assertNotNull(e.getMessage());
        Assert.assertEquals("test message", e.getMessage());
        Assert.assertNotNull(e.getCause());
        Assert.assertEquals("test throwable message", e.getCause().getMessage());
    }

}
