package com.imtilab.bittracer.services;

import java.io.File;

public class ExecThread implements Runnable{

    @Override
    public void run() {
        System.out.println("run start-----------------------");
       // try {
//            System.out.println("sleep started");
//            Thread.sleep(10000);
//            System.out.println("sleep end");
//            data = "test data";
            String PROJECT_PATH = "F: "+ File.separator+"cd F:/SB/bittracer_api_client";
//            List<String> commands = new ArrayList<>();
//            commands.add("CMD");
//            commands.add("/C");
//            commands.add("cd");
//            commands.add(PROJECT_PATH);
//            commands.add(" && gradle clean testReport -PAPINameList=TestBrandList");
//            System.out.println(commands);
//            ProcessBuilder pb = new ProcessBuilder(commands);
//            try {
//                Process p = pb.start();
//                int j = p.waitFor();
//                int exitValue = p.exitValue();
//                System.out.println("Finished with code: " + j);
//                System.out.println("Finished with exitValue: " + exitValue);
//            } catch (Exception e) {
//                System.out.println("exception: " + e);
//            }
            //String PROJECT_PATH = "F:\\workspace\\QA_Automation_Tool\\ecsg.qa.it";
//           new Thread(()->{
            try {
                String className = "TestBrandList";
                String cmdCommand = PROJECT_PATH + " && gradlew build testReport -PAPINameList=" + className;
                System.out.println("cmdCommand "+cmdCommand);
                CmdExecuter.runProcess(cmdCommand);

                System.out.println("Test execution started");
            } catch (Exception e) {
                System.out.println("exception occurred : " + e);
            }
//           }).start();
//            String className = "TestBrandList";
//            String cmdCommand = "cd " + PROJECT_PATH + " && gradlew build testReport -PAPINameList=" + className;
//            System.out.println(cmdCommand);
//            CmdExecuter.runProcess(cmdCommand);
//        } catch (Exception e) {
//            System.err.println(e);
//        }
        System.out.println("--------------end of the run method-------------");
    }
}
