package com.imtilab.bittracer.test.validationv2

import com.jayway.jsonpath.JsonPath
import groovy.util.logging.Slf4j
import com.imtilab.bittracer.antlr.expression.ValidationBaseVisitor
import com.imtilab.bittracer.antlr.expression.ValidationLexer
import com.imtilab.bittracer.antlr.expression.ValidationParser
import com.imtilab.bittracer.model.DataType
import com.imtilab.bittracer.model.MetaData
import com.imtilab.bittracer.model.ValueStorage
import com.imtilab.bittracer.utils.DateUtil

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.regex.Matcher

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasNoJsonPath
import static com.imtilab.bittracer.test.validationv2.matchers.IsCollectionMatchers.isCollection
import static org.hamcrest.MatcherAssert.assertThat

@Slf4j
class ValidationVisitor extends ValidationBaseVisitor<Object> {
    boolean isAsserted = false
    boolean isAssertableStatement = true
    ValueStorage valueStorage
    // assert: assert expression
    @Override
    Object visitAssertFunctionCall(ValidationParser.AssertFunctionCallContext ctx) {
        isAsserted = true
        assert visit(ctx)
    }

    /*
    // list: '[' exprList? ']'
    @Override
    def visitList(ValidationParser.ListContext ctx) {
        List<Object> list = new ArrayList<>()
        if (ctx.exprList() != null) {
            for (ValidationParser.ExpressionContext ex : ctx.exprList().expression()) {
                list.add(this.visit(ex))
            }
        }
        return list
    }*/

    // Size expression      #sizeFunctionCall
    @Override
    def visitSizeFunctionCall(ValidationParser.SizeFunctionCallContext ctx) {
        def value = this.visit(ctx.value())
        if (value instanceof Collection) {
            if (!value.isEmpty() && value[0] instanceof Collection) {
                List allLength = []
                value.each {
                    allLength.add(it.size())
                }
                return allLength
            }
            return value.size()
        }

        throw new ValidationException(ctx)
    }

    //Date value Pattern? value                           #dateFunctionCall
    @Override
    Object visitDateFunctionCall(ValidationParser.DateFunctionCallContext ctx) {
        def date = visit(ctx.value(0))
        String pattern = visit(ctx.value(1))
        if (isAssertableStatement)
            isAsserted = true
        DateUtil.getFormattedDate(date, pattern)
    }

    //DateTime value Pattern? value                        #dateTimeFunctionCall
    @Override
    Object visitDateTimeFunctionCall(ValidationParser.DateTimeFunctionCallContext ctx) {
        def date = visit(ctx.value(0))
        String pattern = visit(ctx.value(1))
        if (isAssertableStatement)
            isAsserted = true
        DateUtil.getFormattedDateTime(date, pattern)
    }

    // DATETIME dateKey PATTERN datePattern timeZone TO PATTERN toDatePattern toTimeZone
    //DateTime value Pattern? value value To Pattern? value value    #dateTimeConversionFunctionCall
    @Override
    Object visitDateTimeConversionFunctionCall(ValidationParser.DateTimeConversionFunctionCallContext ctx) {
        def dateInput = visit(ctx.value(0))
        String pattern = visit(ctx.value(1))
        String timeZone = visit(ctx.value(2))
        String toPattern = visit(ctx.value(3))
        String toTimeZone = visit(ctx.value(4))
        if (dateInput instanceof Collection) {
            return dateInput.stream()
                    .map { date -> ValidationVisitorHelper.convertToTimeZone(date as String, pattern, timeZone, toPattern, toTimeZone) }
                    .collect()
        }
        return ValidationVisitorHelper.convertToTimeZone(dateInput as String, pattern, timeZone, toPattern, toTimeZone)
    }

    //Date value Pattern? value To Pattern? value        #dateConvertionFunctionCall
    @Override
    Object visitDateConvertionFunctionCall(ValidationParser.DateConvertionFunctionCallContext ctx) {
        def dateInput = visit(ctx.value(0))
        String pattern = visit(ctx.value(1))
        String toPattern = visit(ctx.value(2))
        if (dateInput instanceof Collection) {
            return dateInput.stream()
                    .map { date -> DateUtil.getOldToNewFormattedDate(date as String, pattern,toPattern) }
                    .collect()
        }
        return DateUtil.getOldToNewFormattedDate(dateInput,pattern,toPattern)
    }

    // Exist expression      #existFunctionCall
    @Override
    Object visitExistKey(ValidationParser.ExistKeyContext ctx) {
        ctx.jsonKey().each {
            String key = "\$." + it.getText().replaceAll("\\[\\]", "[*]")
            if (key.endsWith("[*]")) {
                assert isCollection(JsonPath.read(valueStorage.getResponseBody(), key))
            } else {
                assertThat(valueStorage.getResponseBody(), hasJsonPath(key))
            }
        }
        isAsserted = true
        true
    }

    // Exist Any expression      #ExistAnyFunctionCall
    @Override
    Object visitExistAnyKey(ValidationParser.ExistAnyKeyContext ctx) {
        ctx.jsonKey().each {
            String key = "\$." + it.getText().replaceAll("\\[\\]", "[*]")
            assertThat(valueStorage.getResponseBody(), hasJsonPath(key))
        }
        isAsserted = true
        true
    }

    // Not Exist expression      #notExistFunctionCall
    @Override
    Object visitNotExistKey(ValidationParser.NotExistKeyContext ctx) {
        ctx.jsonKey().each {
            String key = "\$." + it.getText().replaceAll("\\[\\]", "[*]")
            assertThat(valueStorage.getResponseBody(), hasNoJsonPath(key))
        }
        isAsserted = true
        true
    }

    // Length expression      #lengthFunctionCall
    @Override
    def visitLengthFunctionCall(ValidationParser.LengthFunctionCallContext ctx) {
        def value = this.visit(ctx.value())
        if (value instanceof String) {
            return value.length()
        }

        if (value instanceof Collection) {
            List allLengths = []
            value.each {
                if (it instanceof String)
                    allLengths.add(it.length())
                else {
                    throw new ValidationException(it)
                }
            }
            return allLengths
        }

        throw new ValidationException(ctx)
    }

    // Sort value By jsonKey (','jsonKey)* order=(Ascending| Decending) #sortFunctionCall
    @Override
    Object visitSortFunctionCall(ValidationParser.SortFunctionCallContext ctx) {
        int order = ctx.hasProperty('order') && ctx.order != null ? ctx.order.getType() : ValidationParser.Ascending
        def array = visit(ctx.value())
        def prevArr = array.collect()
        List properties = []
        ctx.jsonKey().each {
            properties.add(it.getText())
        }
        ValidationVisitorHelper.sort(array, properties, order)

        //array
        if (prevArr ==array) isAsserted =true
    }

    // '-' expression                           #unaryMinusExpression
    @Override
    def visitUnaryMinusExpression(ValidationParser.UnaryMinusExpressionContext ctx) {
        def v = this.visit(ctx.expression())
        if (!(v instanceof Number)) {
            throw new ValidationException(ctx)
        }
        return -1 * v
    }

    // '!' expression                           #notExpression
    @Override
    def visitNotExpression(ValidationParser.NotExpressionContext ctx) {
        def v = this.visit(ctx.expression())
        if (!(v instanceof Boolean)) {
            throw new ValidationException(ctx)
        }
        if (isAssertableStatement) {
            assert !v
            return true
        } else {
            return !v
        }
    }
    // expression '^' expression                #powerExpression
    @Override
    def visitPowerExpression(ValidationParser.PowerExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (lhs instanceof Number && rhs instanceof Number) {
            return Math.pow(lhs as double, rhs as double)
        }
        throw new ValidationException(ctx)
    }

    // expression op=( '*' | '/' | '%' ) expression         #multExpression
    @Override
    def visitMultExpression(ValidationParser.MultExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case ValidationLexer.Multiply:
                return multiply(ctx)
            case ValidationLexer.Divide:
                return divide(ctx)
            case ValidationLexer.Modulus:
                return modulus(ctx)
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType())
        }
    }

    // expression op=( '+' | '-' ) expression               #addExpression
    @Override
    def visitAddExpression(ValidationParser.AddExpressionContext ctx) {
        if (ctx.dateTimeOp != null) {
            switch (ctx.op.getType()) {
                case ValidationLexer.Add:
                    return addDate(ctx)
                case ValidationLexer.Subtract:
                    return subtractDate(ctx)
                default:
                    throw new RuntimeException("unknown operator type: " + ctx.op.getType())
            }
        } else {
            switch (ctx.op.getType()) {
                case ValidationLexer.Add:
                    return add(ctx)
                case ValidationLexer.Subtract:
                    return subtract(ctx)
                default:
                    throw new RuntimeException("unknown operator type: " + ctx.op.getType())
            }
        }
    }

    // expression op=( '>=' | '<=' | '>' | '<' ) expression #compExpression
    @Override
    def visitCompExpression(ValidationParser.CompExpressionContext ctx) {
        if (isAssertableStatement)
            isAsserted = true
        def actual = this.visit(ctx.expression(0))
        def expected = this.visit(ctx.expression(1))
        if (actual instanceof Collection) {
            actual.each {
                assertComExpression(it, expected, ctx)
            }
            return true
        } else {
            return assertComExpression(actual, expected, ctx)
        }
    }

    private assertComExpression(def actual, def expected, ValidationParser.CompExpressionContext ctx) {
        switch (ctx.op.getType()) {
            case ValidationLexer.LT:
                return lt(actual, expected, ctx)
            case ValidationLexer.LTEquals:
                return ltEq(actual, expected, ctx)
            case ValidationLexer.GT:
                return gt(actual, expected, ctx)
            case ValidationLexer.GTEquals:
                return gtEq(actual, expected, ctx)
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType())
        }
    }

    // expression op=( '==' | '!=' ) expression             #eqExpression
    @Override
    def visitEqExpression(ValidationParser.EqExpressionContext ctx) {
        if (isAssertableStatement)
            isAsserted = true
        switch (ctx.op.getType()) {
            case ValidationLexer.Equals:
                return eq(ctx)
            case ValidationLexer.NEquals:
                return nEq(ctx)
            default:
                throw new RuntimeException("unknown operator type: " + ctx.op.getType())
        }
    }

    def multiply(ValidationParser.MultExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (lhs == null || rhs == null || lhs instanceof String || rhs instanceof String) {
            throw new ValidationException(ctx)
        }

        if (lhs instanceof Number && rhs instanceof Number) {
            return lhs * rhs
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> x * rhs }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> x * lhs }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def divide(ValidationParser.MultExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (lhs instanceof Number && rhs instanceof Number) {
            return lhs / rhs
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> x / rhs }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> x / lhs }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def modulus(ValidationParser.MultExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (lhs instanceof Number && rhs instanceof Number) {
            return lhs % rhs
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> x % rhs }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> x % lhs }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def addDate(ValidationParser.AddExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        int rhs = this.visit(ctx.expression(1))
        int dateTimeOp = ctx.dateTimeOp.getType()

        if (lhs instanceof Number && rhs instanceof Number) {
            return addWithDate(lhs, rhs, ctx, dateTimeOp)
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> addWithDate(x, rhs, ctx, dateTimeOp) }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> addWithDate(x, rhs, ctx, dateTimeOp) }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def subtractDate(ValidationParser.AddExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        int rhs = this.visit(ctx.expression(1))
        int dateTimeOp = ctx.dateTimeOp.getType()

        if (lhs instanceof Number && rhs instanceof Number) {
            return subtractFromDate(lhs, rhs, ctx, dateTimeOp)
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> subtractFromDate(x, rhs, ctx, dateTimeOp) }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> subtractFromDate(x, rhs, ctx, dateTimeOp) }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def addWithDate(LocalDateTime originalDate, int increaseTo, int dateTimeOp) {
        switch (dateTimeOp) {
            case ValidationParser.Year:
                return originalDate.plusYears(increaseTo)
                break
            case ValidationParser.Month:
                return originalDate.plusMonths(increaseTo)
                break
            case ValidationParser.Week:
                return originalDate.plusWeeks(increaseTo)
                break
            case ValidationParser.Day:
                return originalDate.plusDays(increaseTo)
                break
            case ValidationParser.Hour:
                return originalDate.plusHours(increaseTo)
                break
            case ValidationParser.Minute:
                return originalDate.plusMinutes(increaseTo)
                break
        }
    }

    private def subtractFromDate(LocalDateTime originalDate, int reduceTo, int dateTimeOp) {
        switch (dateTimeOp) {
            case ValidationParser.Year:
                return originalDate.minusYears(reduceTo)
                break
            case ValidationParser.Month:
                return originalDate.minusMonths(reduceTo)
                break
            case ValidationParser.Week:
                return originalDate.minusWeeks(reduceTo)
                break
            case ValidationParser.Day:
                return originalDate.minusDays(reduceTo)
                break
            case ValidationParser.Hour:
                return originalDate.minusHours(reduceTo)
                break
            case ValidationParser.Minute:
                return originalDate.minusMinutes(reduceTo)
                break
        }
    }

    private def add(ValidationParser.AddExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))

        if (lhs instanceof Number && rhs instanceof Number) {
            return lhs + rhs
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> x + rhs }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> x + lhs }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def subtract(ValidationParser.AddExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (lhs instanceof Number && rhs instanceof Number) {
            return lhs - rhs
        } else if (lhs instanceof Collection && !(rhs instanceof Collection)) {
            return lhs.stream().map { x -> x - rhs }.collect()
        } else if (rhs instanceof Collection && !(lhs instanceof Collection)) {
            return rhs.stream().map { x -> x - lhs }.collect()
        }
        throw new ValidationException(ctx)
    }

    private def gtEq(def actual, def expected, ValidationParser.CompExpressionContext ctx) {
        if (actual instanceof Number && expected instanceof Number) {
            if (isAssertableStatement) {
                assert actual >= expected
                return true
            }
            return actual >= expected

        }
        if (actual instanceof String && expected instanceof String) {
            if (isAssertableStatement) {
                assert actual.compareTo(expected) >= 0
                return true
            }
            return actual.compareTo(expected) >= 0

        }
        throw new ValidationException(ctx)
    }

    private def ltEq(def actual, def expected, ValidationParser.CompExpressionContext ctx) {
        if (actual instanceof Number && expected instanceof Number) {
            if (isAssertableStatement) {
                assert actual <= expected
                return true
            }
            return actual <= expected

        }
        if (actual instanceof String && expected instanceof String) {
            if (isAssertableStatement) {
                assert actual.compareTo(expected) > 0
                return true
            }
            return actual.compareTo(expected) > 0

        }
        throw new ValidationException(ctx)
    }

    private def gt(def actual, def expected, ValidationParser.CompExpressionContext ctx) {
        if (actual instanceof Number && expected instanceof Number) {
            if (isAssertableStatement) {
                assert actual > expected
                return true
            }
            return actual > expected

        }
        if (actual instanceof String && expected instanceof String) {
            if (isAssertableStatement) {
                assert actual.compareTo(expected) > 0
                return true
            }
            return actual.compareTo(expected) > 0

        }
        throw new ValidationException(ctx)
    }

    private def lt(def actual, def expected, ValidationParser.CompExpressionContext ctx) {
        if (actual instanceof Number && expected instanceof Number) {
            if (isAssertableStatement) {
                assert actual < expected
                return true
            }
            return actual < expected

        }
        if (actual instanceof String && expected instanceof String) {
            if (isAssertableStatement) {
                assert actual.compareTo(expected) < 0
                return true
            }
            return actual.compareTo(expected) < 0

        }
        throw new ValidationException(ctx)
    }

    private def eq(ValidationParser.EqExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            if (lhs instanceof Collection && !(rhs instanceof Collection)) {
                assert lhs.size() > 0
                lhs.each {
                    assert it == rhs
                }
            } else if (lhs instanceof Collection && rhs instanceof Collection) {
                assert lhs.size() == rhs.size(): "Two list size are not same.\nlhs: ${lhs}\nrhs: ${rhs}"
                for (int i = 0; i < lhs.size(); i++) {
                    assert lhs.getAt(i) == rhs.getAt(i)
                }
            } else {
                assert lhs == rhs
            }
            return true
        }
        return lhs == rhs
    }

    private def nEq(ValidationParser.EqExpressionContext ctx) {
        def actual = this.visit(ctx.expression(0))
        def expected = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            assert !(actual == expected)
            return true
        }
        return !(actual == expected)
    }

    // expression '&&' expression               #andExpression
    @Override
    def visitAndExpression(ValidationParser.AndExpressionContext ctx) {
        isAssertableStatement = false
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        isAssertableStatement = true

        if (!(lhs instanceof Boolean) || !(rhs instanceof Boolean)) {
            throw new ValidationException(ctx)
        }
        if (isAssertableStatement) {
            isAsserted = true
            assert lhs && rhs
            return true
        }
        return lhs && rhs
    }

    // expression '||' expression               #orExpression
    @Override
    def visitOrExpression(ValidationParser.OrExpressionContext ctx) {
        isAssertableStatement = false
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        isAssertableStatement = true

        if (!(lhs instanceof Boolean) || !(rhs instanceof Boolean)) {
            throw new ValidationException(ctx)
        }
        if (isAssertableStatement) {
            isAsserted = true
            assert lhs || rhs
            return true
        }
        return lhs || rhs
    }

    // condition '?' expression ':' expression #ternaryExpression
    @Override
    def visitTernaryExpression(ValidationParser.TernaryExpressionContext ctx) {
        def condition = this.visit(ctx.expression(0))
        if (isAssertableStatement)
            isAsserted = true
        if (condition as Boolean) {
            return this.visit(ctx.expression(1))
        } else {
            return this.visit(ctx.expression(2))
        }
    }

    // expression In expression                 #inExpression
    @Override
    def visitInExpression(ValidationParser.InExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            isAsserted = true
            if (lhs instanceof Collection) {
                lhs.each { leftItem ->
                    assert leftItem in rhs
                }
            } else {
                assert lhs in rhs
            }
        }

        if (lhs instanceof Collection) {
            boolean isIn = true
            lhs.each { leftItem ->
                if (!(leftItem in rhs)) {
                    isIn = false
                }
            }
            return isIn
        } else {
            return lhs in rhs
        }
    }

    // expression In expression                 #notInExpression
    @Override
    Object visitNotInExpression(ValidationParser.NotInExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            isAsserted = true
            if (lhs instanceof Collection) {
                lhs.each { leftItem ->
                    assert !(leftItem in rhs)
                }
            } else {
                assert !(lhs in rhs)
            }
        }

        if (lhs instanceof Collection) {
            boolean isNotIn = true
            lhs.each { leftItem ->
                if (leftItem in rhs) {
                    isNotIn = false
                }
            }
            return isNotIn
        } else {
            return !(lhs in rhs)
        }
    }

    // expression StartsWith expression                     #startWithExpression
    @Override
    Object visitStartWithExpression(ValidationParser.StartWithExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            isAsserted = true
            if (lhs instanceof Collection) {
                lhs.each { leftItem ->
                    assert leftItem.startsWith(rhs)
                }
            } else {
                assert lhs.startsWith(rhs)
            }
            return true
        } else {
            if (lhs instanceof Collection) {
                boolean isStartWith = true
                for (def leftItem : lhs) {
                    if (!leftItem.startsWith(rhs)) {
                        isStartWith = false
                        break
                    }
                }
                return isStartWith
            } else {
                return lhs.startsWith(rhs)
            }
        }
    }

    @Override
    Object visitStartWithAnyExpression(ValidationParser.StartWithAnyExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (rhs instanceof Collection) {
            boolean result = false
            for(def rightItem:rhs){
                if( lhs.startsWith(rightItem)){
                   result = true
                    break
                }
            }
            assert result == true
        } else {
            assert lhs.startsWith(rhs) == true
        }

    }
// expression EndsWith expression                       #endWithExpression
    @Override
    Object visitEndWithExpression(ValidationParser.EndWithExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            isAsserted = true
            if (lhs instanceof Collection) {
                lhs.each { leftItem ->
                    assert leftItem.endsWith(rhs)
                }
            } else {
                assert lhs.endsWith(rhs)
            }
            return true
        } else {
            boolean isEndsWith = true
            if (lhs instanceof Collection) {
                for (def leftItem : lhs) {
                    if (!leftItem.endsWith(rhs)) {
                        isEndsWith = false
                        break
                    }
                }
                return isEndsWith
            } else {
                assert lhs.endsWith(rhs)
            }
        }
    }

    //expression EndWithAny                                 #endWithAnyExpression
    @Override
    Object visitEndWithAnyExpression(ValidationParser.EndWithAnyExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (rhs instanceof Collection) {
            boolean result = false
            for(def rightItem:rhs){
                if( lhs.endsWith(rightItem)){
                    result = true
                    break
                }
            }
            assert result

        } else {
            assert lhs.endsWith(rhs)
        }

    }
// expression Contains expression                       #containsWithExpression
    @Override
    Object visitContainsWithExpression(ValidationParser.ContainsWithExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (isAssertableStatement) {
            isAsserted = true
            if (lhs instanceof Collection) {
                lhs.each { leftItem ->
                    assert leftItem.contains(rhs)
                }
            } else {
                assert lhs.contains(rhs)
            }
            return true
        } else {
            if (lhs instanceof Collection) {
                boolean isContains = true
                for (def leftItem : lhs) {
                    if (!leftItem.contains(rhs)) {
                        isContains = false
                        break
                    }
                }
                return isContains
            } else {
                return lhs.contains(rhs)
            }
        }
    }

    //expression ContainsWithAny            #containsWithAny
    @Override
    Object visitContainsWithAnyExpression(ValidationParser.ContainsWithAnyExpressionContext ctx) {
        def lhs = this.visit(ctx.expression(0))
        def rhs = this.visit(ctx.expression(1))
        if (rhs instanceof Collection) {
            boolean result = false
           for(def rightItem:rhs){
                if( lhs.contains(rightItem)){
                    result = true
                    break
                }
            }
            assert result
        } else {
            assert lhs.contains(rhs) == true
        }
    }
// Number                                   #numberExpression
    @Override
    def visitNumberExpression(ValidationParser.NumberExpressionContext ctx) {
        return Double.valueOf(ctx.getText())
    }

    // Bool                                     #boolExpression
    @Override
    def visitBoolExpression(ValidationParser.BoolExpressionContext ctx) {
        return Boolean.valueOf(ctx.getText())
    }

    // Null                                     #nullExpression
    @Override
    def visitNullExpression(ValidationParser.NullExpressionContext ctx) {
        return null
    }

    // list indexes?                            #listExpression
    @Override
    def visitListExpression(ValidationParser.ListExpressionContext ctx) {
        def val = JsonPath.read(ctx.List().getText(), '$')
        if (ctx.indexes() != null) {
            List<ValidationParser.ExpressionContext> exps = ctx.indexes().expression()
            val = resolveIndexes(val, exps)
        }
        return val
    }

//    // '{}'                                                 #emptyObjectExpression
//    @Override
//    Object visitEmptyObjectExpression(ValidationParser.EmptyObjectExpressionContext ctx) {
//        return [:]
//    }

    @Override
    Object visitJsonValueExpression(ValidationParser.JsonValueExpressionContext ctx) {
        return JsonPath.read(ctx.JsonObj().getText(), '$')
    }

    @Override
    def visitCurrentDateFunctionCall(ValidationParser.CurrentDateFunctionCallContext ctx) {
        switch (ctx.current.getType()) {
            case ValidationParser.CurrentDate:
                return LocalDate.now()
            case ValidationParser.CurrentDateTime:
                return LocalDateTime.now()
        }
    }

    @Override
    def visitJsonKeyExpression(ValidationParser.JsonKeyExpressionContext ctx) {
        if (ctx.jsonKey().getText().equals('[]')) {
            return []
        }
        String key = "\$." + ctx.jsonKey().getText().replaceAll("\\[\\]", "[*]")
        int fromObjectType = ctx.hasProperty('from') && ctx.from != null ? ctx.from.getType() : ValidationParser.RESPONSE
        def value = null
        switch (fromObjectType) {
            case ValidationParser.REQUEST:
                value = JsonPath.read(valueStorage.getRequestBody().toString(), key)
                break
            case ValidationParser.UPSTREAM:
                value = JsonPath.read(valueStorage.getUpstreamBody().toString(), key)
                break
            default:
                value = JsonPath.read(valueStorage.getResponseBody().toString(), key)
                break
        }
        return value
    }

    @Override
    Object visitTypeConvertionExpression(ValidationParser.TypeConvertionExpressionContext ctx) {
        int toType = ctx.type.getType()
        def value = this.visit(ctx.value())
        if (value != null) {
            value = ValidationVisitorHelper.convertTypeAll(value, toType)
        }
        value
    }

    // String indexes?                          #stringExpression
    @Override
    def visitStringExpression(ValidationParser.StringExpressionContext ctx) {
        String text = ctx.getText()
        text = text.substring(1, text.length() - 1).replaceAll("\\\\(.)", "\$1")
        def val = text
        if (ctx.indexes() != null) {
            List<ValidationParser.ExpressionContext> exps = ctx.indexes().expression()
            val = resolveIndexes(val, exps)
        }
        return val
    }

    // '(' expression ')' indexes?              #expressionExpression
    @Override
    Object visitExpressionExpression(ValidationParser.ExpressionExpressionContext ctx) {
        def val = this.visit(ctx.expression());
        if (ctx.indexes() != null) {
            List<ValidationParser.ExpressionContext> exps = ctx.indexes().expression();
            val = resolveIndexes(val, exps);
        }
        return val;
    }

    private def resolveIndexes(def val, List<ValidationParser.ExpressionContext> indexes) {
        for (ValidationParser.ExpressionContext ec : indexes) {
            def idx = this.visit(ec)
            if (!(idx instanceof Number) || (!(val instanceof Collection) && !(val instanceof String))) {
                throw new ValidationException("Problem resolving indexes on " + val + " at " + idx, ec)
            }
            int i = idx.intValue()
            if (val instanceof String) {
                val = val.substring(i, i + 1)
            } else {
                val = val.get(i)
            }
        }
        return val
    }

    //Concat value(','value)*                              #concatExpression
    @Override
    Object visitConcatExpression(ValidationParser.ConcatExpressionContext ctx) {
        def resultValue = ""
        ctx.value().each {
            def visitedValue = visit(it)
            if (visitedValue instanceof Collection && !(resultValue instanceof Collection)) {
                List<String> list = []
                visitedValue.each { vv ->
                    list.add(resultValue + vv)
                }
                resultValue = list
            } else if (resultValue instanceof Collection && !(visitedValue instanceof Collection)) {
                List<String> list = []
                resultValue.each { rv ->
                    list.add(rv + visitedValue)
                }
                resultValue = list
            } else if (resultValue instanceof Collection && visitedValue instanceof Collection) {
                throw new Exception("Multiple list cannot be concat")
            } else {
                resultValue = resultValue + visitedValue
            }
        }
        if (resultValue instanceof String) {
            return resultValue
        } else if (resultValue instanceof Collection) {
            return resultValue.collect { it -> it.toString() }
        }
    }

    //Validate Schema                                      #validateJsonSchema
    @Override
    Object visitValidateJsonSchema(ValidationParser.ValidateJsonSchemaContext ctx) {
        isAsserted = true
        String schema = ctx.JsonObj().getText()
        Set allErrors = JsonSchemaValidator.validate(valueStorage.getResponseBody().toString(), schema)
        List errors = []
        allErrors.each {
            def missingParamMsgArr = it.toString().replaceFirst("\\\$\\.","").split("(: is missing)")
            if (missingParamMsgArr.size() > 1) {
                String missingParam = missingParamMsgArr[0]
                if (isIgnorableRule(missingParam)) {
                    log.info("Assertion ignored: " + missingParam)
                } else {
                    log.info("Assertion failed: " + missingParam)
                    errors.add(it)
                }
            } else {
                errors.add(it)
            }
        }
        assert errors.size() == 0
        return true
    }

    private boolean isIgnorableRule(String rule) {
        boolean isIgnore = false
        for (String ignoreParam : MetaData.ignoreTests) {
            if ((ignoreParam =~ /\[[0-9]+\]/) || !ignoreParam.contains("[")) {
                if (rule.startsWith(ignoreParam)) {
                    isIgnore = true
                    break
                }
            } else {
                ignoreParam = ignoreParam
                        .replaceAll(/\./, "\\\\.").replaceAll(/\[.*?\]/, "\\\\[.*?\\\\]")
                if (rule =~ /$ignoreParam/) {
                    isIgnore = true
                    break
                }
            }
        }
        isIgnore
    }

    @Override
    Boolean visitIfElseExpression(ValidationParser.IfElseExpressionContext ctx) {
        def condition = visit(ctx.condition())
        if (isAssertableStatement) {
            isAsserted = true
            if (condition) {
                assert visit(ctx.exIf)
                return true
            } else if (ctx.exElse) {
                assert visit(ctx.exElse)
                return true
            }
            return true
        } else {
            if (condition) {
                return visit(ctx.exIf)
            } else if (ctx.exElse) {
                return visit(ctx.exElse)
            }
            return true
        }
    }

    @Override
    Object visitValueExpression(ValidationParser.ValueExpressionContext ctx) {
        return visit(ctx.value())
    }

    @Override
    Object visitConditionExpression(ValidationParser.ConditionExpressionContext ctx) {
        isAssertableStatement = false
        def value = visit(ctx.expression())
        isAssertableStatement = true
        return value
    }

    // ceil (expression)
    @Override
    Object visitCeilFunctionCall(ValidationParser.CeilFunctionCallContext ctx) {
        def val = visit(ctx.expression())
        if (val instanceof Collection) {
            return val.stream().map { x -> Math.ceil(x) }.collect()
        }
        return Math.ceil(val)
    }

    // round (expression)
    @Override
    Object visitRoundFunctionCall(ValidationParser.RoundFunctionCallContext ctx) {
        def val = visit(ctx.expression())
        if (val instanceof Collection) {
            return val.stream().map { x -> Math.round(x) }.collect()
        }
        return Math.round(val)
    }

    // floor (expression)
    @Override
    Object visitFloorFunctionCall(ValidationParser.FloorFunctionCallContext ctx) {
        def val = visit(ctx.expression())
        if (val instanceof Collection) {
            return val.stream().map { x -> Math.floor(x) }.collect()
        }
        return Math.floor(val)
    }

    @Override
    Object visitFormatNumberExpression(ValidationParser.FormatNumberExpressionContext ctx) {
        try {
            def val = visit(ctx.value(0))
            String pattern = visit(ctx.value(1))
            DecimalFormat decimalFormat = new DecimalFormat(pattern)
            return decimalFormat.format(val)
        } catch (Exception e) {
            log.error("Make sure number formatter is correct.\n Error Message: ${e.getMessage()}")
        }
    }

    // value Replace (flg=(First | Last | IgnoreCase ))? value By value     #replaceExpression
    @Override
    Object visitReplaceExpression(ValidationParser.ReplaceExpressionContext ctx) {
        int replaceType = (ctx.flg) ? ctx.flg.getType() : -1
        def target = visit(ctx.value(0))
        String searchString = visit(ctx.value(1))
        String replacement = visit(ctx.value(2))
        if (target instanceof Collection) {
            return target.stream()
                    .map { t -> ValidationVisitorHelper.replace(t, searchString, replacement, replaceType) }
                    .collect()
        }
        return ValidationVisitorHelper.replace(target, searchString, replacement, replaceType)
    }

    @Override
    Object visitValidateDatatype(ValidationParser.ValidateDatatypeContext ctx) {
        String key = "\$." + ctx.jsonKey().getText().replaceAll("\\[\\]", "[*]")
        String type = ctx.String().getText()
        int endIndex = type.lastIndexOf('\"')
        type = type.substring(1, endIndex)
        Enum dataType = Enum.valueOf(DataType.class, type.toUpperCase())
        def value = JsonPath.read(valueStorage.getResponseBody().toString(), key)

        if (dataType == DataType.LIST || dataType == DataType.ARRAY) {
            assert value instanceof Collection
        } else if (value instanceof Collection) {
            value.each {
                ValidationVisitorHelper.validateDataType(it, dataType)
            }
        } else {
            ValidationVisitorHelper.validateDataType(value, dataType)
        }
        isAsserted = true
        return true
    }

    // Make sure generic patten symoble "$" replace by "<end>"
    @Override
    Object visitValidatePattern(ValidationParser.ValidatePatternContext ctx) {
        def value = visit(ctx.value())
        String pattern = ctx.String().getText()
                .replaceAll("<end>", Matcher.quoteReplacement("\$"))
        pattern = pattern.substring(1, pattern.length() - 1)
        if (value instanceof Collection) {
            value.each {
                assert it.matches(pattern)
            }
        } else {
            assert value.matches(pattern)
        }
        isAsserted = true
        return true
    }

    @Override
    Object visitMinValueStatement(ValidationParser.MinValueStatementContext ctx) {
        def list = this.visit(ctx.value())
        if(list instanceof Collection){
           def min= list.collect().min()
            return min
        }
        return list
    }

    @Override
    Object visitMaxValueStatement(ValidationParser.MaxValueStatementContext ctx) {
        def list = this.visit(ctx.value())
        if(list instanceof Collection){
            def max= list.collect().max()
            return max
        }
        return list
    }
}
