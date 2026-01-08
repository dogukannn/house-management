import org.mindrot.jbcrypt.BCrypt

fun main(args: Array<String>) {
    val password = if (args.isNotEmpty()) args[0] else "password"
    val hash = BCrypt.hashpw(password, BCrypt.gensalt(12))
    println(hash)
}
