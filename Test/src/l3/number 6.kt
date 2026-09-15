package l3

fun main()
{
    val green = "\u001B[32m"
    val reset = "\u001B[0m"
    val cyan = "\u001B[36m"
    val bold = "\u001B[1m"
    val purple = "\u001B[35m"

    println("Введите трёхзначное число: ")

    val score: Int = readln().toInt()

    val a = score / 100
    val b = (score / 10) % 10
    val c = score % 10

    println("""
        $purple$bold--------------------------------------$reset
        $purple$bold|          Перестановка цифр         |$reset
        $purple$bold--------------------------------------$reset
        $purple$bold|$green $a$b$c $purple$bold                               |$reset
        $purple$bold|$green $a$c$b $purple$bold                               |$reset
        $purple$bold|$green $b$a$c $purple$bold                               |$reset
        $purple$bold|$green $b$c$a $purple$bold                               |$reset
        $purple$bold|$green $c$a$b $purple$bold                               |$reset
        $purple$bold|$green $c$b$a $purple$bold                               |$reset
        $purple$bold--------------------------------------$reset
        
        
    """.trimIndent())

}