package com.imtilab.bittracer.utils

import com.imtilab.bittracer.antlr.expression.ExpressionBaseVisitor
import com.imtilab.bittracer.antlr.expression.ExpressionLexer
import com.imtilab.bittracer.antlr.expression.ExpressionParser
import com.imtilab.bittracer.model.ValueStorage
import net.sf.json.JSONObject
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CodePointCharStream
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree

class ExpressionEvaluation extends ExpressionBaseVisitor<Object> {
    def data
    boolean isNotVisit = false
    ValueStorage valueStorage

    static def evaluate(def data, String expression, ValueStorage storage) {
        CodePointCharStream input = CharStreams.fromString(expression)
        ExpressionLexer lexer = new ExpressionLexer(input)
        CommonTokenStream tokenStream = new CommonTokenStream(lexer)
        ExpressionParser parser = new ExpressionParser(tokenStream)
        ParseTree tree = parser.expr()
        ExpressionEvaluation visitor = new ExpressionEvaluation()
        visitor.setData(data)
        visitor.setValueStorage(storage)
        visitor.visit(tree)
    }

    @Override
    Object visitNull(ExpressionParser.NullContext ctx) {
        null
    }

    @Override
    Object visitDataType(ExpressionParser.DataTypeContext ctx) {
        ctx.DATATYPE().getText()
    }

    @Override
    Object visitArrayDataType(ExpressionParser.ArrayDataTypeContext ctx) {
        ctx.ARRAY_DATATYPE().getText()
    }

    @Override
    Object visitBooleanValue(ExpressionParser.BooleanValueContext ctx) {
        Boolean.parseBoolean(ctx.BOOLEAN_VALUE().getText())
    }

    @Override
    Object visitStringValue(ExpressionParser.StringValueContext ctx) {
        CommonUtils.convertToPrimitive(ctx.STRINGVALUE().getText())
    }

    @Override
    def visitMulDiv(ExpressionParser.MulDivContext ctx) {
        int operator = ctx.operator.getType()
        def left = visit(ctx.expr(0))
        def right = visit(ctx.expr(1))
        if (left instanceof Collection) {
            ExpressionUtil.performOperationOnArray(left, right, operator)
        } else {
            ExpressionUtil.performOperationOnPrimitive(left, right, operator)
        }
    }

    @Override
    def visitAddSub(ExpressionParser.AddSubContext ctx) {
        Integer operator = ctx.operator.getType()
        def left = visit(ctx.expr(0))
        def right = visit(ctx.expr(1))
        if (left instanceof Collection) {
            ExpressionUtil.performOperationOnArray(left, right, operator)
        } else {
            ExpressionUtil.performOperationOnPrimitive(left, right, operator)
        }
    }

    @Override
    Object visitEmptyObject(ExpressionParser.EmptyObjectContext ctx) {
        JSONObject.fromObject(ctx.EMPTY_OBJECT().getText())
    }

    @Override
    def visitKey(ExpressionParser.KeyContext ctx) {
        def value
        if (isNotVisit) {
            value = ctx.KEY().getText()
        } else {
            String key = ctx.KEY().getText()
            value = ExpressionUtil.evalValueAndFilterEmpty(data, key)
        }
        value
    }

    @Override
    def visitNumber(ExpressionParser.NumberContext ctx) {
        String operand = ctx.NUMBER_OR_PERCENTAGE().getText()
        def sign = ctx.hasProperty('sign') ? ctx.sign : null
        if (sign && sign.getType() == ExpressionParser.MINUS) {
            operand = '-'+operand
        }
        if (operand.endsWith('%')) {
            def (String percentage) = operand.split('%')
            CommonUtils.convertToPrimitive(percentage) / 100
        } else {
            CommonUtils.convertToPrimitive(operand)
        }
    }

    @Override
    def visitFunction(ExpressionParser.FunctionContext ctx) {
        def values = []
        String function = ctx.FUNCTION().getText()
        if (function == "EVALFROM" || function == "FILTER_EMPTY" || function == "FILTER_BY_PROPERTY") {
            isNotVisit = true
        }
        ctx.expr().each {
            values.add(visit(it))
        }
        isNotVisit = false
        ExpressionUtil.evalFunction(valueStorage, function, values)
    }

    @Override
    def visitParenthesis(ExpressionParser.ParenthesisContext ctx) {
        visit(ctx.expr())
    }

    @Override
    Object visitList(ExpressionParser.ListContext ctx) {
        Eval.me(ctx.LIST().getText())
    }

    @Override
    Object visitCloneSeparator(ExpressionParser.CloneSeparatorContext ctx) {
        ctx.BETWEEN_NUMBER().getText()
    }
}
