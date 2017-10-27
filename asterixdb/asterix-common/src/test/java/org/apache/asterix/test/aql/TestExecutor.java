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
package org.apache.asterix.test.aql;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.asterix.common.config.GlobalConfig;
import org.apache.asterix.common.utils.ServletUtil.Servlets;
import org.apache.asterix.test.server.ITestServer;
import org.apache.asterix.test.server.TestServerProvider;
import org.apache.asterix.testframework.context.TestCaseContext;
import org.apache.asterix.testframework.context.TestCaseContext.OutputFormat;
import org.apache.asterix.testframework.context.TestFileContext;
import org.apache.asterix.testframework.xml.TestCase;
import org.apache.asterix.testframework.xml.TestCase.CompilationUnit;
import org.apache.asterix.testframework.xml.TestGroup;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.json.JSONObject;

public class TestExecutor {

    /*
     * Static variables
     */
    protected static final Logger LOGGER = Logger.getLogger(TestExecutor.class.getName());
    // see
    // https://stackoverflow.com/questions/417142/what-is-the-maximum-length-of-a-url-in-different-browsers/417184
    private static final long MAX_URL_LENGTH = 2000l;
    private static Method managixExecuteMethod = null;
    private static final HashMap<Integer, ITestServer> runningTestServers = new HashMap<>();

    /*
     * Instance members
     */
    private String host;
    private int port;
    private ITestLibrarian librarian;

    public TestExecutor() {
        host = "127.0.0.1";
        port = 19002;
    }

    public TestExecutor(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setLibrarian(ITestLibrarian librarian) {
        this.librarian = librarian;
    }

    /**
     * Probably does not work well with symlinks.
     */
    public boolean deleteRec(File path) {
        if (path.isDirectory()) {
            for (File f : path.listFiles()) {
                if (!deleteRec(f)) {
                    return false;
                }
            }
        }
        return path.delete();
    }

    public void runScriptAndCompareWithResult(File scriptFile, PrintWriter print, File expectedFile, File actualFile)
            throws Exception {
        System.err.println("Expected results file: " + expectedFile.toString());
        BufferedReader readerExpected = new BufferedReader(
                new InputStreamReader(new FileInputStream(expectedFile), "UTF-8"));
        BufferedReader readerActual = new BufferedReader(
                new InputStreamReader(new FileInputStream(actualFile), "UTF-8"));
        String lineExpected, lineActual;
        int num = 1;
        try {
            while ((lineExpected = readerExpected.readLine()) != null) {
                lineActual = readerActual.readLine();
                // Assert.assertEquals(lineExpected, lineActual);
                if (lineActual == null) {
                    if (lineExpected.isEmpty()) {
                        continue;
                    }
                    throw new Exception(
                            "Result for " + scriptFile + " changed at line " + num + ":\n< " + lineExpected + "\n> ");
                }

                // Comparing result equality but ignore "Time"-prefixed fields. (for metadata tests.)
                String[] lineSplitsExpected = lineExpected.split("Time");
                String[] lineSplitsActual = lineActual.split("Time");
                if (lineSplitsExpected.length != lineSplitsActual.length) {
                    throw new Exception("Result for " + scriptFile + " changed at line " + num + ":\n< " + lineExpected
                            + "\n> " + lineActual);
                }
                if (!equalStrings(lineSplitsExpected[0], lineSplitsActual[0])) {
                    throw new Exception("Result for " + scriptFile + " changed at line " + num + ":\n< " + lineExpected
                            + "\n> " + lineActual);
                }

                for (int i = 1; i < lineSplitsExpected.length; i++) {
                    String[] splitsByCommaExpected = lineSplitsExpected[i].split(",");
                    String[] splitsByCommaActual = lineSplitsActual[i].split(",");
                    if (splitsByCommaExpected.length != splitsByCommaActual.length) {
                        throw new Exception("Result for " + scriptFile + " changed at line " + num + ":\n< "
                                + lineExpected + "\n> " + lineActual);
                    }
                    for (int j = 1; j < splitsByCommaExpected.length; j++) {
                        if (splitsByCommaExpected[j].indexOf("DatasetId") >= 0) {
                            // Ignore the field "DatasetId", which is different for different runs.
                            // (for metadata tests)
                            continue;
                        }
                        if (!equalStrings(splitsByCommaExpected[j], splitsByCommaActual[j])) {
                            throw new Exception("Result for " + scriptFile + " changed at line " + num + ":\n< "
                                    + lineExpected + "\n> " + lineActual);
                        }
                    }
                }

                ++num;
            }
            lineActual = readerActual.readLine();
            if (lineActual != null) {
                throw new Exception("Result for " + scriptFile + " changed at line " + num + ":\n< \n> " + lineActual);
            }
        } catch (Exception e) {
            System.err.println("Actual results file: " + actualFile.toString());
            throw e;
        } finally {
            readerExpected.close();
            readerActual.close();
        }

    }

    private boolean equalStrings(String s1, String s2) {
        String[] rowsOne = s1.split("\n");
        String[] rowsTwo = s2.split("\n");

        for (int i = 0; i < rowsOne.length; i++) {
            String row1 = rowsOne[i];
            String row2 = rowsTwo[i];

            if (row1.equals(row2)) {
                continue;
            }

            String[] fields1 = row1.split(" ");
            String[] fields2 = row2.split(" ");

            boolean bagEncountered = false;
            Set<String> bagElements1 = new HashSet<String>();
            Set<String> bagElements2 = new HashSet<String>();

            for (int j = 0; j < fields1.length; j++) {
                if (j >= fields2.length) {
                    return false;
                } else if (fields1[j].equals(fields2[j])) {
                    bagEncountered = fields1[j].equals("{{");
                    if (fields1[j].startsWith("}}")) {
                        if (!bagElements1.equals(bagElements2)) {
                            return false;
                        }
                        bagEncountered = false;
                        bagElements1.clear();
                        bagElements2.clear();
                    }
                    continue;
                } else if (fields1[j].indexOf('.') < 0) {
                    if (bagEncountered) {
                        bagElements1.add(fields1[j].replaceAll(",$", ""));
                        bagElements2.add(fields2[j].replaceAll(",$", ""));
                        continue;
                    }
                    return false;
                } else {
                    // If the fields are floating-point numbers, test them
                    // for equality safely
                    fields1[j] = fields1[j].split(",")[0];
                    fields2[j] = fields2[j].split(",")[0];
                    try {
                        Double double1 = Double.parseDouble(fields1[j]);
                        Double double2 = Double.parseDouble(fields2[j]);
                        float float1 = (float) double1.doubleValue();
                        float float2 = (float) double2.doubleValue();

                        if (Math.abs(float1 - float2) == 0) {
                            continue;
                        } else {
                            return false;
                        }
                    } catch (NumberFormatException ignored) {
                        // Guess they weren't numbers - must simply not be equal
                        return false;
                    }
                }
            }
        }
        return true;
    }

    // For tests where you simply want the byte-for-byte output.
    private static void writeOutputToFile(File actualFile, InputStream resultStream) throws Exception {
        try (FileOutputStream out = new FileOutputStream(actualFile)) {
            IOUtils.copy(resultStream, out);
        }
    }

    private int executeHttpMethod(HttpMethod method) throws Exception {
        HttpClient client = new HttpClient();
        int statusCode;
        try {
            statusCode = client.executeMethod(method);
        } catch (Exception e) {
            GlobalConfig.ASTERIX_LOGGER.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
        if (statusCode != HttpStatus.SC_OK) {
            // QQQ For now, we are indeed assuming we get back JSON errors.
            // In future this may be changed depending on the requested
            // output format sent to the servlet.
            String errorBody = method.getResponseBodyAsString();
            JSONObject result = new JSONObject(errorBody);
            String[] errors = { result.getJSONArray("error-code").getString(0), result.getString("summary"),
                    result.getString("stacktrace") };
            GlobalConfig.ASTERIX_LOGGER.log(Level.SEVERE, errors[2]);
            String exceptionMsg = "HTTP operation failed: " + errors[0] + "\nSTATUS LINE: " + method.getStatusLine()
                    + "\nSUMMARY: " + errors[1] + "\nSTACKTRACE: " + errors[2];
            throw new Exception(exceptionMsg);
        }
        return statusCode;
    }

    public InputStream executeQuery(String str, OutputFormat fmt, String url,
            List<CompilationUnit.Parameter> params) throws Exception {
        HttpMethod method = constructHttpMethod(str, url, "query", false, params);
        // Set accepted output response type
        method.setRequestHeader("Accept", fmt.mimeType());
        executeHttpMethod(method);
        return method.getResponseBodyAsStream();
    }

    public InputStream executeQueryService(String str, OutputFormat fmt, String url,
            List<CompilationUnit.Parameter> params) throws Exception {
        setFormatParam(params, fmt);
        HttpMethod method = constructHttpMethod(str, url, "statement", true, params);
        // Set accepted output response type
        method.setRequestHeader("Accept", OutputFormat.CLEAN_JSON.mimeType());
        executeHttpMethod(method);
        return method.getResponseBodyAsStream();
    }

    private void setFormatParam(List<CompilationUnit.Parameter> params, OutputFormat fmt) {
        boolean formatSet = false;
        for (CompilationUnit.Parameter param : params) {
            if ("format".equals(param.getName())) {
                param.setValue(fmt.mimeType());
                formatSet = true;
            }
        }
        if (!formatSet) {
            CompilationUnit.Parameter formatParam = new CompilationUnit.Parameter();
            formatParam.setName("format");
            formatParam.setValue(fmt.mimeType());
            params.add(formatParam);
        }
    }

    private HttpMethod constructHttpMethod(String statement, String endpoint, String stmtParam, boolean postStmtAsParam,
            List<CompilationUnit.Parameter> otherParams) {
        HttpMethod method;
        if (statement.length() + endpoint.length() < MAX_URL_LENGTH) {
            // Use GET for small-ish queries
            GetMethod getMethod = new GetMethod(endpoint);
            NameValuePair[] parameters = new NameValuePair[otherParams.size() + 1];
            parameters[0] = new NameValuePair(stmtParam, statement);
            int i = 1;
            for (CompilationUnit.Parameter param : otherParams) {
                parameters[i++] = new NameValuePair(param.getName(), param.getValue());
            }
            getMethod.setQueryString(parameters);
            method = getMethod;
        } else {
            // Use POST for bigger ones to avoid 413 FULL_HEAD
            PostMethod postMethod = new PostMethod(endpoint);
            if (postStmtAsParam) {
                for (CompilationUnit.Parameter param : otherParams) {
                    postMethod.setParameter(param.getName(), param.getValue());
                }
                postMethod.setParameter("statement", statement);
            } else {
                // this seems pretty bad - we should probably fix the API and not the client
                postMethod.setRequestEntity(new StringRequestEntity(statement));
            }
            method = postMethod;
        }
        // Provide custom retry handler is necessary
        HttpMethodParams httpMethodParams = method.getParams();
        httpMethodParams.setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        httpMethodParams.setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, StandardCharsets.UTF_8.name());
        return method;
    }

    public InputStream executeClusterStateQuery(OutputFormat fmt, String url) throws Exception {
        HttpMethodBase method = new GetMethod(url);

        // Set accepted output response type
        method.setRequestHeader("Accept", fmt.mimeType());
        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        executeHttpMethod(method);
        return method.getResponseBodyAsStream();
    }

    // To execute Update statements
    // Insert and Delete statements are executed here
    public void executeUpdate(String str, String url) throws Exception {
        // Create a method instance.
        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new StringRequestEntity(str));

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        // Execute the method.
        executeHttpMethod(method);
    }

    // Executes AQL in either async or async-defer mode.
    public InputStream executeAnyAQLAsync(String str, boolean defer, OutputFormat fmt, String url) throws Exception {
        // Create a method instance.
        PostMethod method = new PostMethod(url);
        if (defer) {
            method.setQueryString(new NameValuePair[] { new NameValuePair("mode", "asynchronous-deferred") });
        } else {
            method.setQueryString(new NameValuePair[] { new NameValuePair("mode", "asynchronous") });
        }
        method.setRequestEntity(new StringRequestEntity(str));
        method.setRequestHeader("Accept", fmt.mimeType());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));
        executeHttpMethod(method);
        InputStream resultStream = method.getResponseBodyAsStream();

        String theHandle = IOUtils.toString(resultStream, "UTF-8");

        // take the handle and parse it so results can be retrieved
        InputStream handleResult = getHandleResult(theHandle, fmt);
        return handleResult;
    }

    private InputStream getHandleResult(String handle, OutputFormat fmt) throws Exception {
        final String url = "http://" + host + ":" + port + Servlets.QUERY_RESULT.getPath();

        // Create a method instance.
        GetMethod method = new GetMethod(url);
        method.setQueryString(new NameValuePair[] { new NameValuePair("handle", handle) });
        method.setRequestHeader("Accept", fmt.mimeType());

        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        executeHttpMethod(method);
        return method.getResponseBodyAsStream();
    }

    // To execute DDL and Update statements
    // create type statement
    // create dataset statement
    // create index statement
    // create dataverse statement
    // create function statement
    public void executeDDL(String str, String url) throws Exception {
        // Create a method instance.
        PostMethod method = new PostMethod(url);
        method.setRequestEntity(new StringRequestEntity(str));
        // Provide custom retry handler is necessary
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new DefaultHttpMethodRetryHandler(3, false));

        // Execute the method.
        executeHttpMethod(method);
    }

    // Method that reads a DDL/Update/Query File
    // and returns the contents as a string
    // This string is later passed to REST API for execution.
    public String readTestFile(File testFile) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(testFile));
        String line = null;
        StringBuilder stringBuilder = new StringBuilder();
        String ls = System.getProperty("line.separator");
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line);
            stringBuilder.append(ls);
        }
        reader.close();
        return stringBuilder.toString();
    }

    public static void executeManagixCommand(String command) throws ClassNotFoundException, NoSuchMethodException,
            SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (managixExecuteMethod == null) {
            Class<?> clazz = Class.forName("org.apache.asterix.installer.test.AsterixInstallerIntegrationUtil");
            managixExecuteMethod = clazz.getMethod("executeCommand", String.class);
        }
        managixExecuteMethod.invoke(null, command);
    }

    public static String executeScript(ProcessBuilder pb, String scriptPath) throws Exception {
        pb.command(scriptPath);
        Process p = pb.start();
        p.waitFor();
        return getProcessOutput(p);
    }

    private static String executeVagrantScript(ProcessBuilder pb, String node, String scriptName) throws Exception {
        pb.command("vagrant", "ssh", node, "--", pb.environment().get("SCRIPT_HOME") + scriptName);
        Process p = pb.start();
        p.waitFor();
        InputStream input = p.getInputStream();
        return IOUtils.toString(input, StandardCharsets.UTF_8.name());
    }

    private static String executeVagrantManagix(ProcessBuilder pb, String command) throws Exception {
        pb.command("vagrant", "ssh", "cc", "--", pb.environment().get("MANAGIX_HOME") + command);
        Process p = pb.start();
        p.waitFor();
        InputStream input = p.getInputStream();
        return IOUtils.toString(input, StandardCharsets.UTF_8.name());
    }

    private static String getScriptPath(String queryPath, String scriptBasePath, String scriptFileName) {
        String targetWord = "queries" + File.separator;
        int targetWordSize = targetWord.lastIndexOf(File.separator);
        int beginIndex = queryPath.lastIndexOf(targetWord) + targetWordSize;
        int endIndex = queryPath.lastIndexOf(File.separator);
        String prefix = queryPath.substring(beginIndex, endIndex);
        String scriptPath = scriptBasePath + prefix + File.separator + scriptFileName;
        return scriptPath;
    }

    private static String getProcessOutput(Process p) throws Exception {
        StringBuilder s = new StringBuilder();
        BufferedInputStream bisIn = new BufferedInputStream(p.getInputStream());
        StringWriter writerIn = new StringWriter();
        IOUtils.copy(bisIn, writerIn, "UTF-8");
        s.append(writerIn.toString());

        BufferedInputStream bisErr = new BufferedInputStream(p.getErrorStream());
        StringWriter writerErr = new StringWriter();
        IOUtils.copy(bisErr, writerErr, "UTF-8");
        s.append(writerErr.toString());
        if (writerErr.toString().length() > 0) {
            StringBuilder sbErr = new StringBuilder();
            sbErr.append("script execution failed - error message:\n");
            sbErr.append("-------------------------------------------\n");
            sbErr.append(s.toString());
            sbErr.append("-------------------------------------------\n");
            LOGGER.info(sbErr.toString().trim());
            throw new Exception(s.toString().trim());
        }
        return s.toString();
    }

    public void executeTest(String actualPath, TestCaseContext testCaseCtx, ProcessBuilder pb,
            boolean isDmlRecoveryTest) throws Exception {
        executeTest(actualPath, testCaseCtx, pb, isDmlRecoveryTest, null);
    }


    public void executeTest(TestCaseContext testCaseCtx, TestFileContext ctx, String statement,
            boolean isDmlRecoveryTest, ProcessBuilder pb, CompilationUnit cUnit, MutableInt queryCount,
            List<TestFileContext> expectedResultFileCtxs, File testFile, String actualPath) throws Exception {
        File qbcFile;
        boolean failed = false;
        File expectedResultFile;
        switch (ctx.getType()) {
            case "ddl":
                if (ctx.getFile().getName().endsWith("aql")) {
                    executeDDL(statement, "http://" + host + ":" + port + Servlets.AQL_DDL.getPath());
                } else {
                    executeDDL(statement, "http://" + host + ":" + port + Servlets.SQLPP_DDL.getPath());
                }
                break;
            case "update":
                // isDmlRecoveryTest: set IP address
                if (isDmlRecoveryTest && statement.contains("nc1://")) {
                    statement = statement.replaceAll("nc1://", "127.0.0.1://../../../../../../asterix-app/");
                }
                if (ctx.getFile().getName().endsWith("aql")) {
                    executeUpdate(statement, "http://" + host + ":" + port + Servlets.AQL_UPDATE.getPath());
                } else {
                    executeUpdate(statement, "http://" + host + ":" + port + Servlets.SQLPP_UPDATE.getPath());
                }
                break;
            case "query":
            case "async":
            case "asyncdefer":
                // isDmlRecoveryTest: insert Crash and Recovery
                if (isDmlRecoveryTest) {
                    executeScript(pb, pb.environment().get("SCRIPT_HOME") + File.separator + "dml_recovery"
                            + File.separator + "kill_cc_and_nc.sh");
                    executeScript(pb, pb.environment().get("SCRIPT_HOME") + File.separator + "dml_recovery"
                            + File.separator + "stop_and_start.sh");
                }
                InputStream resultStream = null;
                OutputFormat fmt = OutputFormat.forCompilationUnit(cUnit);
                if (ctx.getFile().getName().endsWith("aql")) {
                    if (ctx.getType().equalsIgnoreCase("query")) {
                        resultStream = executeQuery(statement, fmt,
                                "http://" + host + ":" + port + Servlets.AQL_QUERY.getPath(), cUnit.getParameter());
                    } else if (ctx.getType().equalsIgnoreCase("async")) {
                        resultStream = executeAnyAQLAsync(statement, false, fmt,
                                "http://" + host + ":" + port + Servlets.AQL.getPath());
                    } else if (ctx.getType().equalsIgnoreCase("asyncdefer")) {
                        resultStream = executeAnyAQLAsync(statement, true, fmt,
                                "http://" + host + ":" + port + Servlets.AQL.getPath());
                    }
                } else {
                    if (ctx.getType().equalsIgnoreCase("query")) {
                        resultStream = executeQueryService(statement, fmt,
                                "http://" + host + ":" + port + Servlets.QUERY_SERVICE.getPath(), cUnit.getParameter());
                        resultStream = ResultExtractor.extract(resultStream);
                    } else if (ctx.getType().equalsIgnoreCase("async")) {
                        resultStream = executeAnyAQLAsync(statement, false, fmt,
                                "http://" + host + ":" + port + Servlets.SQLPP.getPath());
                    } else if (ctx.getType().equalsIgnoreCase("asyncdefer")) {
                        resultStream = executeAnyAQLAsync(statement, true, fmt,
                                "http://" + host + ":" + port + Servlets.SQLPP.getPath());
                    }
                }
                if (queryCount.intValue() >= expectedResultFileCtxs.size()) {
                    throw new IllegalStateException("no result file for " + testFile.toString() + "; queryCount: "
                            + queryCount + ", filectxs.size: " + expectedResultFileCtxs.size());
                }
                expectedResultFile = expectedResultFileCtxs.get(queryCount.intValue()).getFile();

                File actualResultFile = testCaseCtx.getActualResultFile(cUnit, expectedResultFile,
                        new File(actualPath));
                actualResultFile.getParentFile().mkdirs();
                writeOutputToFile(actualResultFile, resultStream);

                runScriptAndCompareWithResult(testFile, new PrintWriter(System.err), expectedResultFile,
                        actualResultFile);
                queryCount.increment();

                // Deletes the matched result file.
                actualResultFile.getParentFile().delete();
                break;
            case "mgx":
                executeManagixCommand(statement);
                break;
            case "txnqbc": // qbc represents query before crash
                resultStream = executeQuery(statement, OutputFormat.forCompilationUnit(cUnit),
                        "http://" + host + ":" + port + Servlets.AQL_QUERY.getPath(), cUnit.getParameter());
                qbcFile = getTestCaseQueryBeforeCrashFile(actualPath, testCaseCtx, cUnit);
                qbcFile.getParentFile().mkdirs();
                writeOutputToFile(qbcFile, resultStream);
                break;
            case "txnqar": // qar represents query after recovery
                resultStream = executeQuery(statement, OutputFormat.forCompilationUnit(cUnit),
                        "http://" + host + ":" + port + Servlets.AQL_QUERY.getPath(), cUnit.getParameter());
                File qarFile = new File(actualPath + File.separator
                        + testCaseCtx.getTestCase().getFilePath().replace(File.separator, "_") + "_" + cUnit.getName()
                        + "_qar.adm");
                qarFile.getParentFile().mkdirs();
                writeOutputToFile(qarFile, resultStream);
                qbcFile = getTestCaseQueryBeforeCrashFile(actualPath, testCaseCtx, cUnit);
                runScriptAndCompareWithResult(testFile, new PrintWriter(System.err), qbcFile, qarFile);
                break;
            case "txneu": // eu represents erroneous update
                try {
                    executeUpdate(statement, "http://" + host + ":" + port + Servlets.AQL_UPDATE.getPath());
                } catch (Exception e) {
                    // An exception is expected.
                    failed = true;
                    e.printStackTrace();
                }
                if (!failed) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n  An exception" + "is expected.");
                }
                System.err.println("...but that was expected.");
                break;
            case "script":
                try {
                    String output = executeScript(pb, getScriptPath(testFile.getAbsolutePath(),
                            pb.environment().get("SCRIPT_HOME"), statement.trim()));
                    if (output.contains("ERROR")) {
                        throw new Exception(output);
                    }
                } catch (Exception e) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n", e);
                }
                break;
            case "sleep":
                String[] lines = statement.split("\n");
                Thread.sleep(Long.parseLong(lines[lines.length - 1].trim()));
                break;
            case "errddl": // a ddlquery that expects error
                try {
                    executeDDL(statement, "http://" + host + ":" + port + Servlets.AQL_DDL.getPath());
                } catch (Exception e) {
                    // expected error happens
                    failed = true;
                    e.printStackTrace();
                }
                if (!failed) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n  An exception is expected.");
                }
                System.err.println("...but that was expected.");
                break;
            case "vscript": // a script that will be executed on a vagrant virtual node
                try {
                    String[] command = statement.trim().split(" ");
                    if (command.length != 2) {
                        throw new Exception("invalid vagrant script format");
                    }
                    String nodeId = command[0];
                    String scriptName = command[1];
                    String output = executeVagrantScript(pb, nodeId, scriptName);
                    if (output.contains("ERROR")) {
                        throw new Exception(output);
                    }
                } catch (Exception e) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n", e);
                }
                break;
            case "vmgx": // a managix command that will be executed on vagrant cc node
                try {
                    String output = executeVagrantManagix(pb, statement);
                    if (output.contains("ERROR")) {
                        throw new Exception(output);
                    }
                } catch (Exception e) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n", e);
                }
                break;
            case "cstate": // cluster state query
                try {
                    fmt = OutputFormat.forCompilationUnit(cUnit);
                    resultStream = executeClusterStateQuery(fmt,
                            "http://" + host + ":" + port + Servlets.CLUSTER_STATE.getPath());
                    expectedResultFile = expectedResultFileCtxs.get(queryCount.intValue()).getFile();
                    actualResultFile = testCaseCtx.getActualResultFile(cUnit, expectedResultFile, new File(actualPath));
                    actualResultFile.getParentFile().mkdirs();
                    writeOutputToFile(actualResultFile, resultStream);
                    runScriptAndCompareWithResult(testFile, new PrintWriter(System.err), expectedResultFile,
                            actualResultFile);
                    queryCount.increment();
                } catch (Exception e) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n", e);
                }
                break;
            case "server": // (start <test server name> <port>
                               // [<arg1>][<arg2>][<arg3>]...|stop (<port>|all))
                try {
                    lines = statement.trim().split("\n");
                    String[] command = lines[lines.length - 1].trim().split(" ");
                    if (command.length < 2) {
                        throw new Exception("invalid server command format. expected format ="
                                + " (start <test server name> <port> [<arg1>][<arg2>][<arg3>]"
                                + "...|stop (<port>|all))");
                    }
                    String action = command[0];
                    if (action.equals("start")) {
                        if (command.length < 3) {
                            throw new Exception("invalid server start command. expected format ="
                                    + " (start <test server name> <port> [<arg1>][<arg2>][<arg3>]...");
                        }
                        String name = command[1];
                        Integer port = new Integer(command[2]);
                        if (runningTestServers.containsKey(port)) {
                            throw new Exception("server with port " + port + " is already running");
                        }
                        ITestServer server = TestServerProvider.createTestServer(name, port);
                        server.configure(Arrays.copyOfRange(command, 3, command.length));
                        server.start();
                        runningTestServers.put(port, server);
                    } else if (action.equals("stop")) {
                        String target = command[1];
                        if (target.equals("all")) {
                            for (ITestServer server : runningTestServers.values()) {
                                server.stop();
                            }
                            runningTestServers.clear();
                        } else {
                            Integer port = new Integer(command[1]);
                            ITestServer server = runningTestServers.get(port);
                            if (server == null) {
                                throw new Exception("no server is listening to port " + port);
                            }
                            server.stop();
                            runningTestServers.remove(port);
                        }
                    } else {
                        throw new Exception("unknown server action");
                    }
                } catch (Exception e) {
                    throw new Exception("Test \"" + testFile + "\" FAILED!\n", e);
                }
                break;
            case "lib": // expected format <dataverse-name> <library-name>
                            // <library-directory>
                        // TODO: make this case work well with entity names containing spaces by
                        // looking for \"
                lines = statement.split("\n");
                String lastLine = lines[lines.length - 1];
                String[] command = lastLine.trim().split(" ");
                if (command.length < 3) {
                    throw new Exception("invalid library format");
                }
                String dataverse = command[1];
                String library = command[2];
                switch (command[0]) {
                    case "install":
                        if (command.length != 4) {
                            throw new Exception("invalid library format");
                        }
                        String libPath = command[3];
                        librarian.install(dataverse, library, libPath);
                        break;
                    case "uninstall":
                        if (command.length != 3) {
                            throw new Exception("invalid library format");
                        }
                        librarian.uninstall(dataverse, library);
                        break;
                    default:
                        throw new Exception("invalid library format");
                }
                break;
            default:
                throw new IllegalArgumentException("No statements of type " + ctx.getType());
        }
    }

    public void executeTest(String actualPath, TestCaseContext testCaseCtx, ProcessBuilder pb,
            boolean isDmlRecoveryTest, TestGroup failedGroup) throws Exception {
        File testFile;
        String statement;
        List<TestFileContext> expectedResultFileCtxs;
        List<TestFileContext> testFileCtxs;
        MutableInt queryCount = new MutableInt(0);
        int numOfErrors = 0;
        int numOfFiles = 0;
        List<CompilationUnit> cUnits = testCaseCtx.getTestCase().getCompilationUnit();
        for (CompilationUnit cUnit : cUnits) {
            LOGGER.info(
                    "Starting [TEST]: " + testCaseCtx.getTestCase().getFilePath() + "/" + cUnit.getName() + " ... ");
            testFileCtxs = testCaseCtx.getTestFiles(cUnit);
            expectedResultFileCtxs = testCaseCtx.getExpectedResultFiles(cUnit);
            for (TestFileContext ctx : testFileCtxs) {
                numOfFiles++;
                testFile = ctx.getFile();
                statement = readTestFile(testFile);
                try {
                    executeTest(testCaseCtx, ctx, statement, isDmlRecoveryTest, pb, cUnit, queryCount,
                            expectedResultFileCtxs, testFile, actualPath);
                } catch (Exception e) {
                    System.err.println("testFile " + testFile.toString() + " raised an exception:");
                    boolean unExpectedFailure = false;
                    numOfErrors++;
                    if (cUnit.getExpectedError().size() < numOfErrors) {
                        unExpectedFailure = true;
                    } else {
                        // Get the expected exception
                        String expectedError = cUnit.getExpectedError().get(numOfErrors - 1);
                        System.err.println("+++++\n" + expectedError + "\n+++++\n");
                        if (e.toString().contains(expectedError)) {
                            System.err.println("...but that was expected.");
                        } else {
                            unExpectedFailure = true;
                        }
                    }
                    if (unExpectedFailure) {
                        e.printStackTrace();
                        System.err.println("...Unexpected!");
                        if (failedGroup != null) {
                            failedGroup.getTestCase().add(testCaseCtx.getTestCase());
                        }
                        throw new Exception("Test \"" + testFile + "\" FAILED!", e);
                    }
                } finally {
                    if (numOfFiles == testFileCtxs.size() && numOfErrors < cUnit.getExpectedError().size()) {
                        System.err.println("...Unexpected!");
                        Exception e = new Exception(
                                "Test \"" + cUnit.getName() + "\" FAILED!\nExpected error was not thrown...");
                        e.printStackTrace();
                        throw e;
                    } else if (numOfFiles == testFileCtxs.size()) {
                        LOGGER.info("[TEST]: " + testCaseCtx.getTestCase().getFilePath() + "/" + cUnit.getName()
                                + " PASSED ");
                    }
                }
            }
        }
    }

    private static File getTestCaseQueryBeforeCrashFile(String actualPath, TestCaseContext testCaseCtx,
            CompilationUnit cUnit) {
        return new File(
                actualPath + File.separator + testCaseCtx.getTestCase().getFilePath().replace(File.separator, "_") + "_"
                        + cUnit.getName() + "_qbc.adm");
    }
}
