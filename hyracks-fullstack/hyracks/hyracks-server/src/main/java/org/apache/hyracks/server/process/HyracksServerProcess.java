/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.hyracks.server.process;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class HyracksServerProcess {
    private static final Logger LOGGER = Logger.getLogger(HyracksServerProcess.class.getName());

    protected Process process;
    protected File configFile = null;
    protected File logFile = null;
    protected File appHome = null;
    protected File workingDir = null;

    public void start() throws IOException {
        String[] cmd = buildCommand();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info("Starting command: " + Arrays.toString(cmd));
        }

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        if (logFile != null) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Logging to: " + logFile.getCanonicalPath());
            }
            logFile.getParentFile().mkdirs();
            logFile.delete();
            pb.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile));
        } else {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("Logfile not set, subprocess will output to stdout");
            }
        }
        pb.directory(workingDir);
        process = pb.start();
    }

    public void stop() {
        process.destroy();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String[] buildCommand() {
        List<String> cList = new ArrayList<String>();
        cList.add(getJavaCommand());
        addJvmArgs(cList);
        cList.add("-Dapp.home=" + appHome.getAbsolutePath());
        cList.add("-classpath");
        cList.add(getClasspath());
        cList.add(getMainClassName());
        if (configFile != null) {
            cList.add("-config-file");
            cList.add(configFile.getAbsolutePath());
        }
        addCmdLineArgs(cList);
        return cList.toArray(new String[cList.size()]);
    }

    protected void addJvmArgs(List<String> cList) {
    }

    protected void addCmdLineArgs(List<String> cList) {
    }

    protected abstract String getMainClassName();

    private final String getClasspath() {
        return System.getProperty("java.class.path");
    }

    private final String getJavaCommand() {
        return System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
    }
}
