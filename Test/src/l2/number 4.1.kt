package l2

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

fun main()
{
    val a: Double = 10.5
    val b: Double = 20.3

    println("Максимум: ${max(a, b)}")
    println("Минимум:${min(a, b)}")
    println("Округление вверх: ${ceil(10.1)}")
    println("Округление вниз: ${floor(10.9)}")

}