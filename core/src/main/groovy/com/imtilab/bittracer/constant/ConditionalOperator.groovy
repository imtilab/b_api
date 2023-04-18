package com.imtilab.bittracer.constant

/**
 * For Conditional OR, AND
 */
enum ConditionalOperator {
    LOGICAL_OR("\\|\\|"),
    OR("||"),
    LOGICAL_AND("&&"),
    AND("&&")

    String operator

    ConditionalOperator(String operator) {
        this.operator = operator
    }

    String get() {
        this.operator = operator
    }
}
