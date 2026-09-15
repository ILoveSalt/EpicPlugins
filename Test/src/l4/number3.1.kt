package l4

import colors.green
import colors.white

fun main()
{
    println("$white Введите время дня: ")

    val hour: Int = readln().toInt()

    if(hour >= 12)
    {
        println("$green Доброе утро!")
    }
    else
    {
        println("$green Добрый день")

    }


}