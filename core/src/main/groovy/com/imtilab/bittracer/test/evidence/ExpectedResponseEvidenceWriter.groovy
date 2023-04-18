package com.imtilab.bittracer.test.evidence

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.EvidenceContext
import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8

@Slf4j
class ExpectedResponseEvidenceWriter implements EvidenceWriter {
    @Override
    void write(EvidenceContext context) {
        try {
            String fileName = Constants.EXPECTED_RESPONSE_EVIDENCE_FILE + context.getTimestamp() + Constants.TXT_FILE
            File expectedResponseFile = new File(context.archivePath + File.separator + fileName)
            FileUtils.touch(expectedResponseFile)
            FileUtils.writeStringToFile(expectedResponseFile, context.getExpectedResponse().toString(), UTF_8, !Constants.APPEND_ON_FILE_WRITE)
        } catch (IOException e) {
            log.debug("opps! unable to write expected response evidence", e)
        }
    }
}
