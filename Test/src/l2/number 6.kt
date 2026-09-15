package l2

import kotlin.math.sqrt

fun main()
{
    val celsius: Double = 25.0
    val fahrenheit: Double = celsius * 9 / 5 + 32
    val a: Double = 3.0
    val b: Double = 4.0
    val c = sqrt(a * a + b * b)


    println("$celsius ℃ = $fahrenheit ℉")
    println("Гипотенуза: $c")



}