package l4

import colors.red
import colors.green
import colors.white
import colors.bold

fun main(){

    println("$white Сколько вам лет?")
    val age: Int = readln().toInt()

    println("$white Вы в костюме? ($green$bold True$white /$red$bold False $white)")
    val hasSuit: Boolean = readln().toBoolean()



    if (age >= 18 && hasSuit)
    {
        println("$green Добро пожаловать!")
    }
    else if (age < 18)
    {
        println("$red Вход только с 18 лет!")
    }
    else
    {
        println("$red Нужен костюм!")
    }

}