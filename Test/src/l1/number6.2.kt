package l1

fun main()
{
    println("Введите имя: ")

    val friendName: String = readln()

    println("Сколько тебе лет: ")

    val ageScore0: String = readln()
    val ageScore1: Int = ageScore0.toInt()
    println("Привет, $friendName, тебе будет ${ageScore1 + 1} лет в след. году")

}