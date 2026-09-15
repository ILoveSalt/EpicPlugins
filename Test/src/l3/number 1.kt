package l3

import kotlin.math.pow
fun main()
{
    println("Введите длину ребра: ")
    val a: String = readln()

    val V = a.toDouble().pow(3).toInt()
    val S = (6 * a.toDouble().pow(2).toInt())

    println("Обьем куба: $V")
    println("Площадь полной поверхности: $S")




}