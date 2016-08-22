package br.com.ligpo.parameter

import kotlin.reflect.KProperty

interface Parameter<T> {
    fun get(): T
    fun bind(t: T): Unit

    companion object Factory {
        fun <T> dynamic(bottom: T): DynamicParameter<T> = ThreadLocalParameter(bottom)
        fun <T> once(): AssignOnce<T> = AssignOnceParameter()
    }
}

interface AssignOnce<T> : DynamicParameter<T> {
    val isUnbound: Boolean
}

interface DynamicParameter<T> : Parameter<T> {

}

interface StackableParameter<T> : DynamicParameter<T> {
    fun pop(): Unit
}

operator fun <T> Parameter<T>.getValue(thisRef: Any, property: KProperty<*>): T {
    return get()
}

class ParameterPair<T> internal constructor(val parameter: DynamicParameter<T>, val value: T) where T : Any

fun <T : Any> ParameterPair<T>.parameterBind() {
    parameter.bind(value)
}

fun <T : Any> ParameterPair<T>.unbind() {
    if (parameter is StackableParameter) {
        parameter.pop()
    }
}

infix fun <T : Any, P : AssignOnce<T>> P.with(t: T): ParameterPair<T> {
    if (this.isUnbound) {
        return ParameterPair(this, t)
    } else {
        throw IllegalStateException("Trying to bind a bound assign once parameter")
    }
}

infix fun <T : Any, P : DynamicParameter<T>> P.with(t: T): ParameterPair<T> {
    return ParameterPair(this, t)
}

inline fun <T : Any, U> parameterize(binding: ParameterPair<T>, andDo: () -> U): U {
    binding.parameterBind()
    try {
        return andDo()
    } finally {
        binding.unbind()
    }
}

inline fun <U> parameterize(vararg bindings: ParameterPair<*>, andDo: () -> U): U {
    bindings.forEach { p -> p.parameterBind() }
    try {
        return andDo()
    } finally {
        bindings.forEach { p -> p.unbind() }
    }
}