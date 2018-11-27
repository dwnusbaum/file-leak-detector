/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.kohsuke.file_leak_detector.instrumented;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kohsuke.file_leak_detector.Listener;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class SetContextClassLoaderDemo {
    private StringWriter output;

    @BeforeClass
    public static void setupClass() {
        assertTrue(Listener.isAgentInstalled());
    }

    @Before
    public void setup() {
        output = new StringWriter();
        Listener.TRACE = new PrintWriter(output);
    }

    @Test
    @Ignore("Current only logging calls from the SynchronousNonBlockingStepExecution thread pool")
    public void setClassLoader() throws Exception {
        final Thread t = Thread.currentThread();
        ClassLoader old = t.getContextClassLoader();
        try {
            t.setContextClassLoader(SetContextClassLoaderDemo.class.getClassLoader());
            assertNotNull("No class loader record found", findClassLoaderRecord(t));

            t.setContextClassLoader(null);
            assertNotNull("No class loader record found", findClassLoaderRecord(t));
            
            Thread other = new Thread(new Runnable() {
                public void run() {
                    t.setContextClassLoader(null);
                }
            }, "test");
            other.start();
            other.join();
            assertNotNull("No class loader record found", findClassLoaderRecord(other));
        } finally {
            t.setContextClassLoader(old);
        }

        String traceOutput = output.toString();
        assertTrue(traceOutput.contains("Set context class loader to:sun.misc.Launcher$AppClassLoader"));
        assertTrue(traceOutput.contains("Set context class loader to:null"));
        assertTrue(traceOutput.contains("by thread:test"));
    }

    private static Listener.ClassLoaderRecord findClassLoaderRecord(Thread t) {
        for (Listener.Record record : Listener.getCurrentOpenFiles()) {
            if (record instanceof Listener.ClassLoaderRecord) {
                Listener.ClassLoaderRecord clRecord = (Listener.ClassLoaderRecord) record;
                if (clRecord.threadName.equals(t.getName())) {
                    return clRecord;
                }
            }
        }
        return null;
    }
}
