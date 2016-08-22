# Parameterize

This project bring racket-link parameters to Kotlin.

## Usage

Parameters can be rebound inside a `parameterize` block.


```kotlin
import br.com.ligpo.parameter.parameterize
import br.com.ligpo.parameter.Parameter

val p: Parameter<String> = Parameter.dynamic("a")
var x: String = ""

parameterize(p with "b") {
    x = p.get()
}

println(x) // b
println(p.get()) // a
```

