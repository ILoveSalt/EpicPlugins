package l3

fun main()
{
    println("Введите трехзначное число: ")

    val score: Int = readln().toInt()

    val a = score / 100
    val b = (score / 10) % 10
    val c = score % 10
    val summa = a+b+c
    val proisvedenie = a*b*c

    println("Сумма цифр = $summa")
    println("Произведение цифр = $proisvedenie")


}