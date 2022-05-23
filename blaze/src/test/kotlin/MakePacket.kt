import com.jacobtread.kme.blaze.Commands
import com.jacobtread.kme.blaze.Components
import com.jacobtread.kme.blaze.group
import com.jacobtread.kme.blaze.unique

fun main(args: Array<String>) {
    val v = unique(
        Components.AUTHENTICATION,
        Commands.UPDATE_ACCOUNT
    ) {
        +group {
            text("Hello", "Hello")
        }
    }
    println(v)
}