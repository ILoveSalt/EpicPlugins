package l5

import colors.red
import colors.yellow
import colors.green

fun main()
{

    println("Введите цвет светофора")
    val color: String = readln().toString()

     when (color){

         "red" -> println("$red Стоп")
         "yellow" -> println("$yellow Приготовься")
         "green" -> println("$green Едь")


     }

}