package l2

fun main()
{
    val number: Int = 7
    val remainder: Int = number % 2

    println("статок от деления 7 на 2: $remainder")

    val seconds: Int = 65
    val minutes: Int = seconds / 60
    val remainingSeconds: Int = seconds % 60

    println("$seconds секунд - это $minutes мин. и $remainingSeconds сек.")
}