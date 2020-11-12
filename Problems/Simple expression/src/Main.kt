import java.math.BigInteger
import java.util.*

fun main() {
    var l = mutableListOf<BigInteger>()
    val input = Scanner(System.`in`)
    for(i in 0..3){
        l.add(BigInteger(input.nextLine()))
    }
val a = -l[0]
    val ab = a*l[1]
    val abcd = ab + l[2] - l[3]
    println(abcd)
}
