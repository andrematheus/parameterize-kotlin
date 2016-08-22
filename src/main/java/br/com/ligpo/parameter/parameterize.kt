package br.com.ligpo.parameter

import kotlin.concurrent.getOrSet

internal class ThreadLocalParameter<T>(val bottom: T) : StackableParameter<T> {
    val bindingList: ThreadLocal<MutableList<T>> = ThreadLocal()

    val bindings: MutableList<T>
        get() = bindingList.getOrSet { mutableListOf() }

    override fun get() = bindings.lastOrNull() ?: bottom

    override fun bind(t: T) {
        bindings.add(t)
    }

    override fun pop() {
        bindings.removeAt(bindings.lastIndex)
    }
}

internal class AssignOnceParameter<T> : AssignOnce<T> {
    var t: T? = null

    override fun get() = t ?: throw IllegalStateException("assign once parameter must be bound")

    override fun bind(t: T) {
        if (this.t == null) {
            this.t = t
        } else {
            throw IllegalStateException("assign once parameter must be bound only once")
        }
    }

    override val isUnbound: Boolean = t == null
}