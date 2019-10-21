package com.ctl.arrow.recursion

import arrow.core.Eval
import arrow.extension

import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.instances.fix.recursive.cata
import arrow.recursion.instances.fix.recursive.recursive
import arrow.recursion.typeclasses.Recursive
import arrow.typeclasses.Functor
import com.ctl.arrow.recursion.intlistpattern.functor.functor

sealed class IntListPattern<out A> : IntListPatternOf<A> {
    companion object
}

object NilPattern : IntListPattern<Nothing>()

data class ConsPattern<out A>(val head: Int, val tail: A) : IntListPattern<A>()

class ForIntListPattern private constructor() {
    companion object
}
typealias IntListPatternOf<A> = arrow.Kind<ForIntListPattern, A>

@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
inline fun <A> IntListPatternOf<A>.fix(): IntListPattern<A> =
        this as IntListPattern<A>


typealias IntFixList = Fix<ForIntListPattern>

@extension
interface IntListPatternFunctor : Functor<ForIntListPattern> {
    override fun <A, B> IntListPatternOf<A>.map(f: (A) -> B): IntListPatternOf<B> {
        val lp = fix()
        return when (lp) {
            NilPattern -> NilPattern
            is ConsPattern -> ConsPattern(lp.head, f(lp.tail))
        }
    }
}

val multiply: Algebra<ForIntListPattern, Eval<Int>> = { l ->
    val list = l.fix()
    when (list) {
        NilPattern -> Eval.now(1)
        is ConsPattern -> list.tail.map { it * list.head }
    }
}

fun multiplication(list: Fix<ForIntListPattern>): Int {
    return Fix.recursive().run {
        IntListPattern.functor().cata(list, multiply)
    }
}

fun multiplicatio2(list: Fix<ForIntListPattern>): Int {
    return IntListPattern.functor().cata(list, multiply)
}