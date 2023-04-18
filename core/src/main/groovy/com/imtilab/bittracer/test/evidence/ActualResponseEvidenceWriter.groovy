package com.imtilab.bittracer.test.evidence

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.EvidenceContext
import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8

@Slf4j
class ActualResponseEvidenceWriter implements EvidenceWriter {
    @Override
    void write(EvidenceContext context) {
        try {
            def headerStringBuilder = new StringBuilder()
            (headerStringBuilder).with {
                append(Constants.RESPONSE_HEADERS)
                context.getApiResponse().responseHeaders.each { value -> append(value).append("\n") }
            }

            def statusStringBuilder = new StringBuilder()
            (statusStringBuilder).with {
                append(Constants.RESPONSE_STATUS)
                append(context.getApiResponse().getStatus())
                append("\n")
            }

            def actualResponseBuilder = new StringBuilder()
            (actualResponseBuilder).with {
                append(Constants.RESPONSE_BODY)
                append(context.getApiResponse().getResponseBody())
                append("\n")
            }

            String fileName = Constants.ACTUAL_RESPONSE_EVIDENCE_FILE + context.getTimestamp() + Constants.TXT_FILE
            File actualResponseFile = new File(context.archivePath + File.separator + fileName)

            FileUtils.touch(actualResponseFile)
            FileUtils.writeStringToFile(actualResponseFile, headerStringBuilder.toString(), UTF_8, !Constants.APPEND_ON_FILE_WRITE)
            FileUtils.writeStringToFile(actualResponseFile, statusStringBuilder.toString(), UTF_8, Constants.APPEND_ON_FILE_WRITE)
            FileUtils.writeStringToFile(actualResponseFile, actualResponseBuilder.toString(), UTF_8, Constants.APPEND_ON_FILE_WRITE)
        } catch (IOException e) {
            log.debug("opps! unable to write actual response evidence", e)
        }
    }
}
