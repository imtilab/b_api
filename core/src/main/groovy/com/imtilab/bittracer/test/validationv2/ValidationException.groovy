package com.imtilab.bittracer.test.validationv2

import org.antlr.v4.runtime.ParserRuleContext

class ValidationException extends RuntimeException {
    ValidationException(ParserRuleContext ctx) {
        this("Illegal expression: " + ctx.getText(), ctx);
    }

    ValidationException(String msg, ParserRuleContext ctx) {
        super(msg + " line:" + ctx.start.getLine());
    }
}