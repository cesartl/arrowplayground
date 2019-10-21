package com.ctl.arrow.json

import arrow.core.Eval
import arrow.higherkind
import arrow.instances.eval.applicative.map2
import arrow.instances.list.foldable.foldMap
import arrow.instances.monoid
import arrow.instances.sequence.foldable.foldMap
import arrow.recursion.Algebra
import arrow.recursion.data.Fix
import arrow.recursion.instances.fix.recursive.recursive
import arrow.typeclasses.Functor
import arrow.typeclasses.Monoid
import com.ctl.arrow.json.JsonPattern.*
import com.squareup.moshi.JsonReader
import okio.Okio
import okio.Source
import java.io.InputStream


@higherkind
sealed class JsonPattern<out A> : JsonPatternOf<A> {
    object JNull : JsonPattern<Nothing>()
    data class JBool(val value: Boolean) : JsonPattern<Nothing>()
    data class JNum(val value: Double) : JsonPattern<Nothing>()
    data class JString(val value: String) : JsonPattern<Nothing>()
    data class JArray<A>(val value: List<A>) : JsonPattern<A>()
    data class JObject<A>(val value: Map<String, A>) : JsonPattern<A>()
}

object JsonPatternFunctor : Functor<ForJsonPattern> {
    override fun <A, B> JsonPatternOf<A>.map(f: (A) -> B): JsonPatternOf<B> {
        val json = fix()
        return when (json) {
            JNull -> JNull
            is JBool -> JBool(json.value)
            is JNum -> JNum(json.value)
            is JString -> JString(json.value)
            is JArray -> JArray(json.value.map { f(it) })
            is JObject -> JObject(json.value.mapValues { f(it.value) })
        }
    }

}

fun <A> evalMonoid(m: Monoid<A>): Monoid<Eval<A>> =
        object : Monoid<Eval<A>> {
            override fun empty(): Eval<A> = Eval.now(m.empty())

            override fun Eval<A>.combine(b: Eval<A>): Eval<A> {
                return this.map2(b) { (x, y) ->
                    m.run {
                        x.combine(y)
                    }
                }
            }

        }


val countNumAlgebra: Algebra<ForJsonPattern, Eval<Double>> = { l ->
    val json = l.fix()
    when (json) {
        JNull -> Eval.now(0.0)
        is JBool -> Eval.now(0.0)
        is JNum -> Eval.now(json.value)
        is JString -> Eval.now(0.0)
        is JArray -> json.value.foldMap(evalMonoid(Double.monoid())) { it }
        is JObject -> json.value.values.asSequence().foldMap(evalMonoid(Double.monoid())) { it }
    }
}

typealias FixJson = Fix<ForJsonPattern>

object Json {

    fun jNum(value: Double): FixJson = Fix(JsonPattern.JNum(value))
    fun jBool(value: Boolean): FixJson = Fix(JsonPattern.JBool(value))
    fun jString(value: String): FixJson = Fix(JsonPattern.JString(value))
    fun jNull(): FixJson = Fix(JsonPattern.JNull)
    fun jArray(values: List<FixJson>) = Fix(JsonPattern.JArray(values.map { Eval.now(it) }))
    fun jObject(values: Map<String, FixJson>): FixJson = Fix(JsonPattern.JObject(values.mapValues { Eval.now(it.value) }))
    fun addNum(json: FixJson): Double = Fix.recursive().run {
        return JsonPatternFunctor.cata(json, countNumAlgebra)
    }


    fun parse(input: InputStream): FixJson {
        val reader = JsonReader.of(Okio.buffer(Okio.source(input)))
        return handle(reader)
    }

    fun parse(input: Source): FixJson {
        val reader = JsonReader.of(Okio.buffer(input))
        return handle(reader)
    }

    private fun handle(reader: JsonReader): FixJson {
        return when (reader.peek()) {
            JsonReader.Token.BEGIN_ARRAY -> handleArray(reader)
            JsonReader.Token.BEGIN_OBJECT -> handleObject(reader)
            JsonReader.Token.STRING -> jString(reader.nextString())
            JsonReader.Token.NUMBER -> jNum(reader.nextDouble())
            JsonReader.Token.BOOLEAN -> jBool(reader.nextBoolean())
            JsonReader.Token.NULL -> {
                reader.nextNull<Any>()
                jNull()
            }
            else -> throw IllegalArgumentException(reader.peek().toString())
        }
    }

    private fun handleArray(reader: JsonReader): FixJson {
        reader.beginArray()
        val values = mutableListOf<FixJson>()
        while (reader.hasNext()) {
            values.add(handle(reader))
        }
        reader.endArray()
        return jArray(values)
    }

    private fun handleObject(reader: JsonReader): FixJson {
        reader.beginObject()
        val values = mutableMapOf<String, FixJson>()
        while (reader.hasNext()) {
            val name = reader.nextName()
            val value = handle(reader)
            values[name] = value
        }
        reader.endObject()
        return jObject(values)
    }

}
