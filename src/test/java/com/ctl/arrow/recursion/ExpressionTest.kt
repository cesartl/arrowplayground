package com.ctl.arrow.recursion

import arrow.recursion.data.Fix
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

internal class ExpressionTest {

    @Test
    fun evaluate() {
        val expr: Fix<ForExprPattern> = plus(plus(int(1), int(2)), neg(plus(int(3), int(4))))
        println(Expression.evaluate(expr))
        println(Expression.evaluate2(expr))
    }
}