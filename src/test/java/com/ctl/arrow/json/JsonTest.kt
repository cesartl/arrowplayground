package com.ctl.arrow.json

import arrow.core.Eval
import arrow.core.Tuple2
import arrow.instances.list.foldable.foldMap
import arrow.instances.monoid
import arrow.instances.sequence.foldable.foldMap
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.instances.fix.recursive.recursive
import arrow.recursion.typeclasses.Recursive
import arrow.typeclasses.Monoid
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayInputStream
import java.io.InputStream

internal class JsonTest {

    val aoc2015d12 = this.javaClass.classLoader.getResourceAsStream("2015Day12.txt")

    @Test
    fun addNum() {
        val json = Json.jArray(listOf(Json.jNum(1.0), Json.jNum(2.0)))
        val sum = Json.addNum(json)
        assertEquals(3.0, sum)
    }

    fun aocPart1(json: FixJson): Double {
        return Json.addNum(json)
    }

    fun aocPart2(json: FixJson): Double {
        Fix.recursive().run {
            return JsonPatternFunctor.cata(json, part2Algebra).a
        }
    }

    val containsPropertyAlgebra: Algebra<ForJsonPattern, Eval<Boolean>> = { l ->
        val json = l.fix()
        when (json) {
            is JsonPattern.JObject -> {
                if (json.value.containsKey("red")) {
                    Eval.now(true)
                } else {
                    Eval.now(false)
                }
            }
            else -> Eval.now(false)
        }
    }


    fun <A, B> monoidTuple(MA: Monoid<A>, MB: Monoid<B>): Monoid<Tuple2<A, B>> =
            object : Monoid<Tuple2<A, B>> {

                override fun Tuple2<A, B>.combine(y: Tuple2<A, B>): Tuple2<A, B> {
                    val (xa, xb) = this
                    val (ya, yb) = y
                    return Tuple2(MA.run { xa.combine(ya) }, MB.run { xb.combine(yb) })
                }

                override fun empty(): Tuple2<A, B> = Tuple2(MA.empty(), MB.empty())
            }

    val boolM: Monoid<Boolean> = object : Monoid<Boolean> {
        override fun empty(): Boolean = false

        override fun Boolean.combine(b: Boolean): Boolean = this || b
    }

    val part2Algebra: Algebra<ForJsonPattern, Eval<Tuple2<Double, Boolean>>> = { l ->
        val json = l.fix()
        when (json) {
            JsonPattern.JNull -> Eval.now(Tuple2(0.0, false))
            is JsonPattern.JBool -> Eval.now(Tuple2(0.0, false))
            is JsonPattern.JNum -> Eval.now(Tuple2(json.value, false))
            is JsonPattern.JString -> Eval.now(Tuple2(0.0, json.value == "red"))
            is JsonPattern.JArray -> json.value.foldMap(evalMonoid(monoidTuple(Double.monoid(), boolM))) { it.map { Tuple2(it.a, false) } }
            is JsonPattern.JObject -> {
                if (json.value.values.any { it.value().b }) {
                    Eval.now(Tuple2(0.0, false))
                } else {
                    json.value.values.asSequence().foldMap(evalMonoid(monoidTuple(Double.monoid(), boolM))) { it }
                }
            }
        }
    }

    @Test
    internal fun parse() {
        val json1 = """{"a":2,"b":4}"""

        val json = Json.parse(ByteArrayInputStream(json1.toByteArray()))
        println()
    }

    @Test
    internal fun aoc() {
        val json = Json.parse(aoc2015d12)

        val total1 = aocPart1(json)
        println(total1)

        val total2 = aocPart2(json)
        println(total2)
    }
}