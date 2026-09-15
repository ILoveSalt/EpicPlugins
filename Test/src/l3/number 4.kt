package l3

import kotlin.Long

fun main()
{

    val green = "\u001B[32m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[35m"

    println("Введите любое кол-во минут: ")

    val a: Long = readln().trim().toLong()

    val hours: Long = a / 60
    val minuts: Long = (a % 60)

    println("""
        $purple$bold---------------------------------------$reset
        $purple$bold|    Пересчет временного интервала    |$reset
        $purple$bold---------------------------------------$reset
        $purple$bold| $green$a мин - это  $bold$hours$reset $green час  $bold$minuts$reset $green минут.$green $purple$bold   |$reset
        $purple$bold---------------------------------------$reset
        
        
    """.trimIndent())


}