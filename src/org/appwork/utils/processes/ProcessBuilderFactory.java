/**
 *
 * ====================================================================================================================================================
 *         "AppWork Utilities" License
 *         The "AppWork Utilities" will be called [The Product] from now on.
 * ====================================================================================================================================================
 *         Copyright (c) 2009-2015, AppWork GmbH <e-mail@appwork.org>
 *         Schwabacher Straße 117
 *         90763 Fürth
 *         Germany
 * === Preamble ===
 *     This license establishes the terms under which the [The Product] Source Code & Binary files may be used, copied, modified, distributed, and/or redistributed.
 *     The intent is that the AppWork GmbH is able to provide their utilities library for free to non-commercial projects whereas commercial usage is only permitted after obtaining a commercial license.
 *     These terms apply to all files that have the [The Product] License header (IN the file), a <filename>.license or <filename>.info (like mylib.jar.info) file that contains a reference to this license.
 *
 * === 3rd Party Licences ===
 *     Some parts of the [The Product] use or reference 3rd party libraries and classes. These parts may have different licensing conditions. Please check the *.license and *.info files of included libraries
 *     to ensure that they are compatible to your use-case. Further more, some *.java have their own license. In this case, they have their license terms in the java file header.
 *
 * === Definition: Commercial Usage ===
 *     If anybody or any organization is generating income (directly or indirectly) by using [The Product] or if there's any commercial interest or aspect in what you are doing, we consider this as a commercial usage.
 *     If your use-case is neither strictly private nor strictly educational, it is commercial. If you are unsure whether your use-case is commercial or not, consider it as commercial or contact us.
 * === Dual Licensing ===
 * === Commercial Usage ===
 *     If you want to use [The Product] in a commercial way (see definition above), you have to obtain a paid license from AppWork GmbH.
 *     Contact AppWork for further details: <e-mail@appwork.org>
 * === Non-Commercial Usage ===
 *     If there is no commercial usage (see definition above), you may use [The Product] under the terms of the
 *     "GNU Affero General Public License" (http://www.gnu.org/licenses/agpl-3.0.en.html).
 *
 *     If the AGPL does not fit your needs, please contact us. We'll find a solution.
 * ====================================================================================================================================================
 * ==================================================================================================================================================== */
package org.appwork.utils.processes;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.loggingv3.LogV3;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;

public class ProcessBuilderFactory {
    /**
     * @author Thomas
     * @date 17.10.2018
     *
     */
    public static class ProcessStreamReader extends Thread {
        /**
         *
         */
        private final AtomicReference<IOException> exception;
        /**
         *
         */
        private final Process                      process;
        /**
         *
         */
        private InputStream                        input;
        private OutputStream                       output;
        private volatile boolean                   processIsDead;

        /**
         * @param name
         * @param exception
         * @param process
         * @param input
         * @param errorStream
         */
        public ProcessStreamReader(String name, AtomicReference<IOException> exception, Process process, final InputStream input, OutputStream output) {
            super(name);
            this.exception = exception;
            this.process = process;
            this.input = input;
            this.output = output;
        }

        @Override
        public void run() {
            try {
                // System.out.println("Start Process-Reader-Error");
                readStreamToOutputStream();
            } catch (IOException e) {
                if (!processIsDead()) {
                    exception.compareAndSet(null, e);
                    LogV3.logger(ProcessBuilderFactory.class).log(e);
                }
                try {
                    process.exitValue();
                } catch (IllegalThreadStateException e2) {
                    // System.out.println("Process still running. Killing it");
                    process.destroy();
                }
            } finally {
                // System.out.println("Stop Process-Reader-Error");
            }
        }

        /**
         * @throws IOException
         *
         */
        private void readStreamToOutputStream() throws IOException {
            try {
                final byte[] buffer = new byte[1024];
                int len = 0;
                boolean wait = false;
                while (true) {
                    if (processIsDead() && input.available() == 0) {
                        return;
                    }
                    len = input.read(buffer);
                    if (processIsDead() && len == 0) {
                        // according to
                        // https://stackoverflow.com/questions/2319395/what-0-returned-by-inputstream-read-means-how-to-handle-this, this
                        // method MAY return 0 if nothing is read.
                        // so this is a workaround for bad inputstream implementations that might result in endless blocking readers
                        return;
                    }
                    if (len == -1) {
                        break;
                    } else if (len > 0) {
                        wait = false;
                        output.write(buffer, 0, len);
                    } else {
                        try {
                            if (wait == false) {
                                wait = true;
                                // System.out.println("Reader Wait");
                            }
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            throw new IOException(e);
                        }
                    }
                }
            } catch (final IOException e) {
                throw e;
            } finally {
                try {
                    input.close();
                } catch (final Exception e) {
                }
            }
        }

        /**
         * @return
         */
        private boolean processIsDead() {
            return processIsDead;
        }

        /**
         * @throws InterruptedException
         *
         */
        public void onProcessDead() throws InterruptedException {
            if (processIsDead) {
                return;
            }
            processIsDead = true;
            try {
                // wait until the buffer is empty and close the stream afterwards to cancel blocking readers
                while (input.available() > 0) {
                    Thread.sleep(50);
                }
                input.close();
            } catch (IOException e) {
            }
        }
    }

    private static String CONSOLE_CODEPAGE = null;

    public static ProcessOutput runCommand(final java.util.List<String> commands) throws IOException, InterruptedException {
        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static ProcessOutput runCommand(String... commands) throws IOException, InterruptedException {
        return ProcessBuilderFactory.runCommand(ProcessBuilderFactory.create(commands));
    }

    public static ProcessOutput runCommand(ProcessBuilder pb) throws IOException, InterruptedException {
        final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        final ByteArrayOutputStream sdtStream = new ByteArrayOutputStream();
        int exitCode = runCommand(pb, errorStream, sdtStream);
        return new ProcessOutput(exitCode, sdtStream.toByteArray(), errorStream.toByteArray(), getConsoleCodepage());
    }

    public static int runCommand(ProcessBuilder pb, final OutputStream errorStream, final OutputStream sdtStream) throws IOException, InterruptedException {
        return runCommand(pb, errorStream, sdtStream, null);
    }

    /**
     * s
     *
     * @param create
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public static int runCommand(ProcessBuilder pb, final OutputStream errorStream, final OutputStream sdtStream, final ProcessHandler osHandler) throws IOException, InterruptedException {
        // System.out.println("Start Process " + pb.command());
        //
        final Process process = pb.start();
        ProcessStreamReader stdReader = null;
        ProcessStreamReader errorReader = null;
        try {
            final AtomicReference<IOException> exception = new AtomicReference<IOException>();
            if (osHandler == null || !osHandler.setProcess(process)) {
                process.getOutputStream().close();
            }
            stdReader = new ProcessStreamReader("Process-Reader-Std", exception, process, process.getInputStream(), sdtStream);
            errorReader = new ProcessStreamReader("Process-Reader-Error", exception, process, process.getErrorStream(), errorStream);
            int returnCode = -1;
            if (CrossSystem.isWindows()) {
                stdReader.setPriority(Thread.NORM_PRIORITY + 1);
                errorReader.setPriority(Thread.NORM_PRIORITY + 1);
            }
            stdReader.setDaemon(true);
            errorReader.setDaemon(true);
            stdReader.start();
            errorReader.start();
            // System.out.println("Wait for Process");
            returnCode = process.waitFor();
            stdReader.onProcessDead();
            errorReader.onProcessDead();
            // System.out.println("Process returned: " + returnCode);
            while (stdReader.isAlive()) {
                // System.out.println("Wait for Process-Reader-Std");
                stdReader.join(10000);
            }
            while (errorReader.isAlive()) {
                // System.out.println("Wait fo Process-Reader-Error");
                errorReader.join(10000);
            }
            return returnCode;
        } finally {
            try {
                if (errorStream != null) {
                    errorStream.close();
                }
            } catch (Throwable e) {
                LogV3.logger(ProcessBuilderFactory.class).exception("Failed to close the errorStream ", e);
            }
            try {
                if (sdtStream != null) {
                    sdtStream.close();
                }
            } catch (Throwable e) {
                LogV3.logger(ProcessBuilderFactory.class).exception("Failed to close the sdtStream ", e);
            }
            try {
                process.destroy();
            } catch (Throwable e) {
            }
            // make sure the readers end, even in case of an exception
            if (errorReader.isAlive()) {
                errorReader.interrupt();
            }
            if (stdReader.isAlive()) {
                stdReader.interrupt();
            }
            stdReader.onProcessDead();
            errorReader.onProcessDead();
        }
    }

    public static ProcessBuilder create(final java.util.List<String> splitCommandString) {
        return ProcessBuilderFactory.create(splitCommandString.toArray(new String[] {}));
    }

    public static ProcessBuilder create(final String... tiny) {
        return new ProcessBuilder(ProcessBuilderFactory.escape(tiny));
    }

    public static String[] escape(final String[] input) {
        return escape(input, false);
    }

    public static String[] escape(final String[] input, final boolean forceEscape) {
        if (input != null && (CrossSystem.isWindows() || CrossSystem.isOS2() || forceEscape)) {
            /* The windows processbuilder throws exceptions if a arguments starts with ", but does not end with " or vice versa */
            final String[] ret = new String[input.length];
            final String rawC = "\"";
            final String escapedC = "\\\"";
            for (int index = 0; index < ret.length; index++) {
                // only count non escaped quotations
                final String value = input[index];
                if (value == null) {
                    ret[index] = value;
                } else {
                    final int count = new Regex(value, "((?<!\\\\)" + rawC + ")").count();
                    final boolean rawC_Start = value.startsWith(rawC);
                    final boolean rawC_End = value.endsWith(rawC) && !value.endsWith(escapedC);
                    if (count == 0) {
                        // we have none!
                        ret[index] = value;
                    } else if (rawC_Start && rawC_End) {
                        // prefix and postfix are provided
                        if (count % 2 == 0) {
                            ret[index] = value;
                        } else {
                            // we have to accept our fate and trust the input to be valid :)
                            ret[index] = value;
                        }
                    } else if (count % 2 == 0) {
                        // even count
                        ret[index] = value;
                    } else {
                        // WTF: rest must be odd! corrections required?
                        // note: you can't use replace as you will nuke other valid quoted components.
                        if (rawC_Start && !rawC_End) {
                            ret[index] = value + rawC;
                        } else if (!rawC_Start && rawC_End) {
                            ret[index] = rawC + value;
                        } else {
                            // we have to accept our fate and trust the input to be valid :)
                            ret[index] = value;
                        }
                    }
                }
            }
            return ret;
        } else {
            return input;
        }
    }

    /**
     * @return
     * @throws InterruptedException
     */
    public static String getConsoleCodepage() throws InterruptedException {
        if (StringUtils.isEmpty(CONSOLE_CODEPAGE)) {
            switch (CrossSystem.getOSFamily()) {
            case LINUX:
                return System.getProperty("file.encoding");
            case WINDOWS:
                final ProcessBuilder pb = ProcessBuilderFactory.create("cmd", "/c", "chcp");
                final Process process;
                try {
                    process = pb.start();
                    final Thread th = new Thread() {
                        public void run() {
                            try {
                                process.getOutputStream().close();
                                final BufferedReader f = new BufferedReader(new InputStreamReader(process.getInputStream(), "ASCII"));
                                String line;
                                final StringBuilder ret = new StringBuilder();
                                final String sep = System.getProperty("line.separator");
                                while ((line = f.readLine()) != null) {
                                    if (Thread.interrupted()) {
                                        return;
                                    }
                                    if (ret.length() > 0) {
                                        ret.append(sep);
                                    } else if (line.startsWith("\uFEFF")) {
                                        /*
                                         * Workaround for this bug: http://bugs.sun.com/view_bug.do?bug_id=4508058
                                         * http://bugs.sun.com/view_bug.do?bug_id=6378911
                                         */
                                        line = line.substring(1);
                                    }
                                    ret.append(line);
                                }
                                process.destroy();
                                String result = ret.toString();
                                // /
                                result = new Regex(result, ":\\s*(\\d+)").getMatch(0);
                                if (StringUtils.isNotEmpty(result)) {
                                    final String cp = "cp" + result.trim();
                                    // https://msdn.microsoft.com/en-us/library/dd317756%28VS.85%29.aspx
                                    if ("CP65001".equalsIgnoreCase(cp)) {
                                        CONSOLE_CODEPAGE = "UTF-8";
                                    } else {
                                        CONSOLE_CODEPAGE = cp;
                                    }
                                }
                            } catch (Throwable e) {
                                LogV3.log(e);
                            } finally {
                                try {
                                    process.destroy();
                                } catch (Throwable e) {
                                }
                            }
                        };
                    };
                    th.start();
                    try {
                        th.join();
                    } catch (InterruptedException e) {
                        try {
                            process.destroy();
                        } catch (Throwable e1) {
                        }
                        throw e;
                    }
                } catch (IOException e1) {
                    LogV3.log(e1);
                }
                break;
            default:
                break;
            }
            LogV3.info("Console Codepage: " + CONSOLE_CODEPAGE + "(" + Charset.defaultCharset().displayName() + ")");
            if (StringUtils.isEmpty(CONSOLE_CODEPAGE)) {
                CONSOLE_CODEPAGE = Charset.defaultCharset().displayName();
            }
        }
        return CONSOLE_CODEPAGE;
    }
}
