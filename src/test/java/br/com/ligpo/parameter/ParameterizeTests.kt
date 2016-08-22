package br.com.ligpo.parameter

import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.PrintStream
import java.util.*
import kotlin.concurrent.thread
import kotlin.reflect.KProperty
import kotlin.test.assertEquals

class Resettable<T>(val block: () -> T) {
    var ref: T? = null

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (ref == null) {
            ref = block()
        }
        val r = ref
        if (r != null) {
            return r
        } else {
            throw IllegalStateException("Should not be null here.")
        }
    }

    fun reset() {
        ref = null
    }
}

class ParameterizeTests {
    interface IOInterface {
        val emptyString: Parameter<String>
        val input: DynamicParameter<InputStream>
        val output: DynamicParameter<PrintStream>
        val error: AssignOnce<PrintStream>
    }

    private val ior = Resettable {
        object: IOInterface {
            override val emptyString = Parameter.dynamic("")
            override val input = Parameter.dynamic(System.`in`)
            override val output = Parameter.dynamic(System.out)
            override val error = Parameter.once<PrintStream>()
        }
    }

    val IO: IOInterface by ior

    @BeforeMethod
    fun setUp() {
        ior.reset()
    }

    val LOG_MSG = "Outra coisa"

    fun log(msg: String = LOG_MSG) {
        IO.output.get().print(msg)
    }

    @Test
    fun parameterizingSystemOut() {
        val sw = ByteArrayOutputStream()
        val out = PrintStream(sw)
        parameterize(IO.output with out) {
            val o = IO.output.get()
            o.print("Test")
        }
        assertEquals("Test", sw.toString())
    }

    @Test
    fun parameterizingManyParameters() {
        val sout = ByteArrayOutputStream()
        val out = PrintStream(sout)
        val sin = "input".byteInputStream(Charsets.UTF_8)
        parameterize(IO.output with out, IO.input with sin) {
            val o = IO.output.get()
            val input = IO.input.get()
            val s = Scanner(input)
            val read = s.nextLine()
            o.print(read.toUpperCase())
        }
        assertEquals("INPUT", sout.toString())
    }

    @Test
    fun nestedParameterization() {
        val outs = (0..3).map { ByteArrayOutputStream() }
        val outp = ThreadLocalParameter(PrintStream(outs[0]))
        val meth = { s: String ->
            val out = outp.get()
            out.print(s)
        }
        parameterize(outp with PrintStream(outs[1])) {
            meth("1")
            parameterize(outp with PrintStream(outs[2])) {
                meth("2")
            }
            meth("3")
        }
        assertEquals("", outs[0].toString())
        assertEquals("13", outs[1].toString())
        assertEquals("2", outs[2].toString())
    }

    @Test
    fun delegatePropertyInObject() {
        val withDelegatedProperty = object {
            val log by IO.output
            fun doIt() {
                log.print("Hi")
            }
        }
        val bot = ByteArrayOutputStream()
        parameterize(IO.output with PrintStream(bot)) {
            withDelegatedProperty.doIt()
        }
        assertEquals("Hi", bot.toString())
    }

    @Test
    fun asssignOnceShouldBeAssignable() {
        val errs = ByteArrayOutputStream()
        parameterize(IO.error with PrintStream(errs)) {
            IO.error.get().print("Hi")
        }
        assertEquals("Hi", errs.toString())
    }

    @Test(expectedExceptions = arrayOf(IllegalStateException::class))
    fun assignOnceShouldBeAssignableOnce() {
        val errs = ByteArrayOutputStream()
        val errs2 = ByteArrayOutputStream()
        parameterize(IO.error with PrintStream(errs)) {
            parameterize(IO.error with PrintStream(errs2)) {
                IO.error.get().print("Hi")
            }
        }
    }

    @Test
    fun parameterizationWorksAcrossFunctionCalls() {
        val out = ByteArrayOutputStream()
        parameterize(IO.output with PrintStream(out)) {
            log()
        }
        assertEquals(LOG_MSG, out.toString())
    }

    @Test
    fun parametersWorkInMultipleThreads() {
        val outs = (0..3).map { it to ByteArrayOutputStream() }
        val threads = outs.mapIndexed { i, o ->
            thread {
                IO.output.bind(PrintStream(o.second))
                log(LOG_MSG + i)
            }
        }
        threads.forEach { it.join() }
        outs.forEach {
            assertEquals(LOG_MSG + it.first, it.second.toString())
        }
    }
}