package l3

import kotlin.math.*
fun main()
{
    println("Введите первое значение: ")

    val a = readln().toDouble()

    println("Введите второе значение: ")

    val b = readln().toDouble()

    val f = 3 * (a+b).pow(3)+275 * b.pow(2)-127 * a - 41

    println("Результат формулы F($a, $b) = $f")




}