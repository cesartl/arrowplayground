package com.ctl.arrow.recursion

import arrow.Kind
import arrow.core.Eval
import arrow.core.fix
import arrow.extension
import arrow.higherkind
import arrow.instances.eval.monad.monad
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.data.FixOf
import arrow.recursion.data.Mu
import arrow.recursion.data.fix
import arrow.recursion.instances.fix.recursive.cata
import arrow.recursion.instances.fix.recursive.recursive
import arrow.recursion.typeclasses.*
import arrow.typeclasses.Functor


@higherkind
sealed class ExprPattern<out A> : ExprPatternOf<A> {
    class Int(val value: kotlin.Int) : ExprPattern<Nothing>()
    class Neg<out A>(val expr: A) : ExprPattern<A>()
    class Plus<out A>(val expr1: A, val expr2: A) : ExprPattern<A>()
    companion object
}

object ExprPatternFunctorInstance : Functor<ForExprPattern> {

    override fun <A, B> ExprPatternOf<A>.map(f: (A) -> B) = run {
        val fix = fix()
        when (fix) {
            is ExprPattern.Int -> fix
            is ExprPattern.Neg -> ExprPattern.Neg(f(fix.expr))
            is ExprPattern.Plus -> ExprPattern.Plus(f(fix.expr1), f(fix.expr2))
        }
    }
}

typealias Expr = FixOf<ForExprPattern>

fun int(i: Int): Fix<ForExprPattern> = Fix(ExprPattern.Int(i))
fun neg(e: Expr) = Fix(ExprPattern.Neg(Eval.now(e)))
fun plus(a: Expr, b: Expr) = Fix(ExprPattern.Plus(Eval.now(a), Eval.now(b)))

fun evalExprAlgebra() = Algebra<ForExprPattern, Eval<Int>> {
    val fix = it.fix()
    when (fix) {
        is ExprPattern.Int -> Eval.now(fix.value)
        is ExprPattern.Neg -> fix.expr.map { -it }
        is ExprPattern.Plus -> Eval.monad().binding { fix.expr1.bind() + fix.expr2.bind() }.fix()
    }
}



object Expression {

    fun evaluate(exp: Expr): Int {
        return Fix.recursive().run {
            ExprPatternFunctorInstance.cata(exp, evalExprAlgebra())
        }
    }

    fun evaluate2(exp: Expr): Int {
        return ExprPatternFunctorInstance.cata(exp, evalExprAlgebra())
    }
}