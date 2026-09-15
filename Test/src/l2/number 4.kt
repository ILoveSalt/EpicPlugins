package l2

import java.lang.Math.pow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun main()
{
    val base: Double = 2.0
    val exponent: Double = 3.0

    println("2 в степени 3: ${pow(base, exponent)}")
    println("Корень из 16: ${sqrt(16.0)}")

}

fun example()
{
    val a: Double = 10.5
    val b: Double = 20.3

    println("Максимум: ${max(a, b)}")
    println("Минимум:${min(a, b)}")
    println("Округление вверх: ${ceil(10.1)}")
    println("Округление вниз: ${floor(10.9)}")

}