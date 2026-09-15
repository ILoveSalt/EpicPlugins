package l5

fun main()
{
    println("Выбери день недели: ")
    val day: Int = readln().toInt()

    when (day){

        1 -> println("Понедельник")
        2 -> println("Вторник")
        3 -> println("Среда")

    }


}