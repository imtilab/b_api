package com.imtilab.bittracer.test.evidence

import groovy.util.logging.Slf4j
import com.imtilab.bittracer.constant.Constants
import com.imtilab.bittracer.model.EvidenceContext
import org.apache.commons.io.FileUtils

import static java.nio.charset.StandardCharsets.UTF_8

@Slf4j
class RequestEvidenceWriter implements EvidenceWriter {
    @Override
    void write(EvidenceContext context) {
        try {
            def urlStringBuilder = new StringBuilder()
            (urlStringBuilder).with {
                append(Constants.REQUEST_URL)
                append(context.getApiRequest().url)
                append("\n")
            }

            def headerStringBuilder = new StringBuilder()
            (headerStringBuilder).with {
                append(Constants.REQUEST_HEADERS)
                context.getApiRequest().requestHeaders.each { value -> append(value).append("\n") }
            }

            def queryParameterBuilder = new StringBuilder()
            (queryParameterBuilder).with {
                append(Constants.QUERY_PARAMETERS)
                context.getApiRequest().queryParameters.each { value -> append(value).append("\n") }
            }

            def actualRequestBuilder = new StringBuilder()
            (actualRequestBuilder).with {
                append(Constants.REQUEST_BODY)
                append(context.getApiRequest().requestBody)
                append("\n")
            }

            String fileName = Constants.REQUEST_EVIDENCE_FILE + context.getTimestamp() + Constants.TXT_FILE
            File requestFile = new File(context.archivePath + File.separator + fileName)

            FileUtils.touch(requestFile)
            FileUtils.writeStringToFile(requestFile, urlStringBuilder.toString(), UTF_8, !Constants.APPEND_ON_FILE_WRITE)
            FileUtils.writeStringToFile(requestFile, headerStringBuilder.toString(), UTF_8, Constants.APPEND_ON_FILE_WRITE)
            FileUtils.writeStringToFile(requestFile, queryParameterBuilder.toString(), UTF_8, Constants.APPEND_ON_FILE_WRITE)
            FileUtils.writeStringToFile(requestFile, actualRequestBuilder.toString(), UTF_8, Constants.APPEND_ON_FILE_WRITE)
        } catch (IOException e) {
            log.debug("opps! unable to write request evidence", e)
        }
    }
}
