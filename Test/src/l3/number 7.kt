package l3

fun main()
{

    val green = "\u001B[32m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[35m"

    println("Введите четырехзначное число: ")

    val score: Int = readln().toInt()

    val a = score / 1000
    val b = (score / 100) % 10
    val c = (score / 10) % 10
    val d = score % 10

    println("""
        $purple$bold--------------------------------------$reset
        $purple$bold|          Перестановка цифр         |$reset
        $purple$bold--------------------------------------$reset
        $purple$bold|$green Цифра в позиции тысяч равна: $cyan$a$purple$bold     |$reset
        $purple$bold|$green Цифра в позиции сотен равна: $cyan$b$purple$bold     |$reset
        $purple$bold|$green Цифра в позиции десятков равна: $cyan$c$purple$bold  |$reset
        $purple$bold|$green Цифра в позиции единиц равна: $cyan$d$purple$bold    |$reset
        $purple$bold--------------------------------------$reset
        
        
    """.trimIndent())



}