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
package org.apache.asterix.installer.command;

import org.kohsuke.args4j.Option;

public class HelpCommand extends AbstractCommand {

    @Override
    protected void execCommand() throws Exception {
        HelpConfig helpConfig = (HelpConfig) config;
        String command = helpConfig.command;
        CommandType cmdType = CommandType.valueOf(command.toUpperCase());
        String helpMessage = null;
        switch (cmdType) {
            case CREATE:
                helpMessage = new CreateCommand().getUsageDescription();
                break;
            case CONFIGURE:
                helpMessage = new ConfigureCommand().getUsageDescription();
                break;
            case DELETE:
                helpMessage = new DeleteCommand().getUsageDescription();
                break;
            case DESCRIBE:
                helpMessage = new DescribeCommand().getUsageDescription();
                break;
            case RESTORE:
                helpMessage = new RestoreCommand().getUsageDescription();
                break;
            case START:
                helpMessage = new StartCommand().getUsageDescription();
                break;
            case SHUTDOWN:
                helpMessage = new ShutdownCommand().getUsageDescription();
                break;
            case BACKUP:
                helpMessage = new BackupCommand().getUsageDescription();
                break;
            case STOP:
                helpMessage = new StopCommand().getUsageDescription();
                break;
            case VALIDATE:
                helpMessage = new ValidateCommand().getUsageDescription();
                break;
            case INSTALL:
                helpMessage = new InstallCommand().getUsageDescription();
                break;
            case UNINSTALL:
                helpMessage = new UninstallCommand().getUsageDescription();
                break;
            case ALTER:
                helpMessage = new AlterCommand().getUsageDescription();
                break;
            case LOG:
                helpMessage = new LogCommand().getUsageDescription();
                break;
            case STOPNODE:
                helpMessage = new StopNodeCommand().getUsageDescription();
                break;
            case STARTNODE:
                helpMessage = new StartNodeCommand().getUsageDescription();
                break;
            case VERSION:
                helpMessage = new VersionCommand().getUsageDescription();
                break;
            default:
                helpMessage = "Unknown command " + command;
        }

        System.out.println(helpMessage);
    }

    @Override
    protected CommandConfig getCommandConfig() {
        return new HelpConfig();
    }

    @Override
    protected String getUsageDescription() {
        return "\nAlter the instance's configuration settings."
                + "\nPrior to running this command, the instance is required to be INACTIVE state."
                + "\n\nAvailable arguments/options" + "\n-n name of the ASTERIX instance"
                + "\n-conf path to the ASTERIX configuration file.";
    }

}

class HelpConfig extends CommandConfig {

    @Option(name = "-cmd", required = true, usage = "Name of Asterix Instance")
    public String command;

}
