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

import org.apache.asterix.installer.command.ICommand.CommandType;

public class CommandHandler {

    public void processCommand(String args[]) throws Exception {
        CommandType cmdType = CommandType.valueOf(args[0].toUpperCase());
        ICommand cmd = null;
        switch (cmdType) {
            case CREATE:
                cmd = new CreateCommand();
                break;
            case ALTER:
                cmd = new AlterCommand();
                break;
            case DELETE:
                cmd = new DeleteCommand();
                break;
            case DESCRIBE:
                cmd = new DescribeCommand();
                break;
            case BACKUP:
                cmd = new BackupCommand();
                break;
            case RESTORE:
                cmd = new RestoreCommand();
                break;
            case START:
                cmd = new StartCommand();
                break;
            case STOP:
                cmd = new StopCommand();
                break;
            case VALIDATE:
                cmd = new ValidateCommand();
                break;
            case CONFIGURE:
                cmd = new ConfigureCommand();
                break;
            case INSTALL:
                cmd = new InstallCommand();
                break;
            case UNINSTALL:
                cmd = new UninstallCommand();
                break;
            case LOG:
                cmd = new LogCommand();
                break;
            case SHUTDOWN:
                cmd = new ShutdownCommand();
                break;
            case HELP:
                cmd = new HelpCommand();
                break;
            case STOPNODE:
                cmd = new StopNodeCommand();
                break;
            case STARTNODE:
                cmd = new StartNodeCommand();
                break;
            case VERSION:
                cmd = new VersionCommand();
                break;
            default:
                break;
        }
        cmd.execute(args);
    }
}
