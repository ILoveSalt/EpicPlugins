package project

import kotlin.math.abs
fun main()
{

    val green = "\u001B[32m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[35m"

    println("Планирование бюджета на день")

    println("Введите дневной доход: ")

    val income = readln().toDouble()

    println("Планирование расходов")

    println("Расходы на еду: ")
    val food = readln().toDouble()

    println("Расходы на транспорт: ")
    val transport = readln().toDouble()

    println("Расходы на развлечение")
    val ent = readln().toDouble()

    val totalExp = food + transport + ent
    val balance = income - totalExp

    println("""
        $purple$bold--------------------------------------$reset
        $purple$bold|              Итоги дня             |$reset
        $purple$bold--------------------------------------$reset
        $purple$bold|$green Общая сумма расходов: $cyan $totalExp$purple$bold      |$reset
        $purple$bold--------------------------------------$reset
        
        
    """.trimIndent())

    if (balance >= 0) {

        println("Вы укладываетесь в бюджет. Остаток X")
    }
    else
    {
        println("Вы превысили бюджет!")

    }



}