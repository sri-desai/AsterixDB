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
package org.apache.hyracks.control.cc;

import org.kohsuke.args4j.CmdLineParser;

import org.apache.hyracks.control.common.controllers.CCConfig;

public class CCDriver {
    public static void main(String args[]) throws Exception {
        try {
            CCConfig ccConfig = new CCConfig();
            CmdLineParser cp = new CmdLineParser(ccConfig);
            try {
                cp.parseArgument(args);
            } catch (Exception e) {
                System.err.println(e.getMessage());
                cp.printUsage(System.err);
                return;
            }
            ccConfig.loadConfigAndApplyDefaults();

            ClusterControllerService ccService = new ClusterControllerService(ccConfig);
            ccService.start();
            while (true) {
                Thread.sleep(100000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
