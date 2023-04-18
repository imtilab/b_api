package com.imtilab.bittracer.services;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CmdExecuter {
    public static Process process = null;

    public static void runProcess(String command) throws Exception {
        System.out.println(" process start ");
        process = Runtime.getRuntime().exec("cmd /c " + command);
        InputStream isr = process.getInputStream();
        System.out.println(" process " + process.getErrorStream());

        String line = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(isr));
        while ((line = in.readLine()) != null) {
            System.out.println(" " + line);
        }

        // process.getOutputStream().close();
        System.out.println(" exitValue() " + process.exitValue());

        int exitCode = process.waitFor();
        assert exitCode == 0;
    }
}
