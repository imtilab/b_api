package com.imtilab.bittracer.utils.curlparser


import com.imtilab.bittracer.antlr.expression.CurlBaseVisitor
import com.imtilab.bittracer.antlr.expression.CurlLexer
import com.imtilab.bittracer.antlr.expression.CurlParser
import com.imtilab.bittracer.model.ApiRequest
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CodePointCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.apache.commons.lang3.StringUtils

class CurlVisitor extends CurlBaseVisitor<ApiRequest> {

    static ApiRequest parseCurl(String curlCommand){
        CodePointCharStream input = CharStreams.fromString(curlCommand)
        CurlLexer lexer = new CurlLexer(input)
        CommonTokenStream tokenStream = new CommonTokenStream(lexer)
        CurlParser parser = new CurlParser(tokenStream)
        ParseTree tree = parser.curl()
        CurlVisitor visitor = new CurlVisitor()
        visitor.visit(tree)
    }

    @Override
    ApiRequest visitCURLCOMMAND(CurlParser.CURLCOMMANDContext ctx) {
        String method = ctx.method.getText()
        String url = quoteRemover(ctx.URL().getText())
        Map headers = [:]
        headers = extractDataAsMap(ctx.HEADER(), false)
        String data = ctx.DATA() == null ? null : quoteRemover(ctx.DATA().getText())
        def formData = ctx.FORM() == null ? [:] : extractDataAsMap(ctx.FORM(), true)
        ApiRequest apiRequest = ApiRequest.builder()
                .methodName(method)
                .url(url).queryParameters(formData != null && !formData.isEmpty() ? formData : [:])
                .requestHeaders(headers)
                .requestBody(data)
                .build()
        apiRequest
    }

    private String quoteRemover(String value){
        int startIndex = value.indexOf('\'')
        int endIndex = value.lastIndexOf('\'')
        value.substring(startIndex+1,endIndex)
    }

    private String quoteRemoverForFormData(String value) {
        int startIndex = value.indexOf('\"')
        int endIndex = value.lastIndexOf('\"')
        if (startIndex >= 0 && endIndex >= 0) return value.substring(startIndex + 1, endIndex)
        return value
    }

    def extractDataAsMap(List<TerminalNode> nodes, boolean isFormData) {
        Map map = [:]
        String key, value
        nodes.each {
            header ->
                String h = quoteRemover(header.getText())
                if (h.trim().endsWith(";")) {
                    h = h.trim()
                    key = h.substring(0, h.size() - 1)
                    value = StringUtils.EMPTY
                } else {
                    if (!isFormData) (key, value) = h.split(":", 2)
                    else {
                        (key, value) = h.split("=", 2)
                        value = quoteRemoverForFormData(value)
                    }
                }
                map.put(key, value)
        }
        map
    }
}
