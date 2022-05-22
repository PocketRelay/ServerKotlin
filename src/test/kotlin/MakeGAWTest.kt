import com.jacobtread.kme.utils.unixTimeSeconds
import com.jacobtread.xml.PrintOptions
import com.jacobtread.xml.XmlVersion
import com.jacobtread.xml.xml

fun main() {

    val time = unixTimeSeconds().toString()
    val sessionToken = "iwajd90adjh12e7y8123n1219nbd2u8wad1-213123"
    val playerId = 1.toString()
    val playerEmail = "test@test.com"

    @Suppress("SpellCheckingInspection")
    val value = xml("fulllogin", "UTF-8", XmlVersion.V10) {
        element("canageup", "0")
        element("legaldochost")
        element("needslegaldoc", "0")
        element("pclogintoken", sessionToken)
        element("privacypolicyuri")
        "sessioninfo" {
            element("blazeuserid", playerId)
            element("isfirstlogin", "0")
            element("sessionkey", playerId)
            element("lastlogindatetime", time)
            element("email", playerEmail)
        }
    }
    val printOptions = PrintOptions(
        singleLineTextElements = true
    )
    println(value.toString(printOptions))


}