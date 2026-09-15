package l4

fun main()
{
    println("Введите кол-во монет: ")

    val money: Int = readln().toInt()

    println("Введите цену: ")

    val price: Int = readln().toInt()

    if (money >= price)
    {
        println("Покупка совершена")

    }
    else
    {
        println("Недостаточно монет!")
    }

}