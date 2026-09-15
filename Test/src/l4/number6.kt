package l4

import colors.white
import colors.red
import colors.green

fun main ()
{
    println("$white Введите возраст:")
    val age: Int = readln().toInt()

    if(age < 18)
    {
        println("$red Ребёнок")
    }
    else
    {
        println("$green Взрослый")
    }
}