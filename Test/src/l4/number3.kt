package l4

import colors.green
import colors.bold
import colors.red


fun main ()
{
    println("$green$bold Введите число: ")
    val number: Int = readln().toInt()

    if (number % 2 == 0)
    {
        println("$green$bold Число четное")

    }
    else
    {
        println("$red$bold Число не четное")

    }


}