package com.imtilab.bittracer.test.validationv2

import com.imtilab.bittracer.antlr.expression.ValidationLexer
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.LexerNoViableAltException

class ValidationSimpleLexer extends ValidationLexer{
    ValidationSimpleLexer(CharStream input) {
        super(input)
    }

    @Override
    void recover(LexerNoViableAltException e) {
        throw new RuntimeException(e)
    }
}
