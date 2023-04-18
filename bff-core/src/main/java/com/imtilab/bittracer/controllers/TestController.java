package com.imtilab.bittracer.controllers;

import com.imtilab.bittracer.services.TestService;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/p/test")
public class TestController {
    @Autowired
    TestService testService;

    @GetMapping(value = "/test")
    public String test() {
        String data = testService.test();
        return data;
    }

    @GetMapping(value = "/get")
    public String get() {
        Object data = testService.getData();
        return "Report data " + data;
    }

    @GetMapping(value = "/exec")
    public String execute() {
        testService.execute();
        return "Execution started.";
    }

    @GetMapping(value = "/download")
    public ResponseEntity<Object> downloadFile() throws IOException {
        System.out.println("here in file");
        String filename = "F:/SB/image.jpg";
        File file = new File(filename);
        InputStreamResource resource = new InputStreamResource(new FileInputStream(file));

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition",
                String.format("attachment; filename=\"%s\"", "export.zip"));
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");

        ResponseEntity<Object> responseEntity = ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .contentType(MediaType.parseMediaType("application/txt"))
                .body(resource);

        return responseEntity;
    }

    @GetMapping(value = "/zip", produces = "application/zip")
    public void zipFiles(HttpServletResponse response) throws IOException {

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");

        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        // create a list to add files to be zipped
        ArrayList<File> files = new ArrayList<>(2);
        files.add(new File("F:/SB/image.jpg"));

        // package files
        for (File file : files) {
            //new zip entry and copying inputstream with file to zipOutputStream, after all closing streams
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);

            IOUtils.copy(fileInputStream, zipOutputStream);

            fileInputStream.close();
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
    }

    @GetMapping(value = "/report", produces = "application/zip")
    public void zipFiles2(HttpServletResponse response) throws IOException {

        //setting headers
        response.setStatus(HttpServletResponse.SC_OK);
        response.addHeader("Content-Disposition", "attachment; filename=\"test.zip\"");

        ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream());

        // create a list to add files to be zipped
        ArrayList<File> files = new ArrayList<>(2);
        files.add(new File("F:\\SB\\allTests"));

        // package files
        for (File file : files) {
            //new zip entry and copying inputstream with file to zipOutputStream, after all closing streams
            zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
            FileInputStream fileInputStream = new FileInputStream(file);

            IOUtils.copy(fileInputStream, zipOutputStream);

            fileInputStream.close();
            zipOutputStream.closeEntry();
        }

        zipOutputStream.close();
    }

    /**
     * Working perfectly as expected
     *
     * @return zip file generated from report dir
     */
    @GetMapping(value = "/report2", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> export() {
        // customService.generateZipStream() is a service method that can
        //generate zip file using ZipOutputStream and then return this stream

        return ResponseEntity
                .ok()
                .header("Content-Disposition", "attachment;filename=export.zip")
                .body(outputStream -> {
                    // Use inner implement and set StreamResponseBody to ZipOutputStream
                    try (ZipOutputStream zipOut = new ZipOutputStream(outputStream)) {
                        generateZipStream(zipOut);
                    }
                });
    }

    public void generateZipStream(ZipOutputStream zipOut) throws IOException {
        File dir = new File("F:/SB/allTests");
        zipFile(dir, dir.getName(), zipOut);
        zipOut.closeEntry();
        zipOut.close();
    }

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut) throws IOException {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            File[] children = fileToZip.listFiles();
            for (File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }
}
