import com.jacobtread.kme.blaze.Command
import com.jacobtread.kme.blaze.Component

fun main() {
    makeComponents()
}

fun makeComponents() {
    val out = StringBuilder()
    val commands = Component.values().toList()
    commands.forEach {
        out.append("  const val ")
            .append(it.name)
            .append(" = 0x")
            .append(
                it.value
                    .toString(16)
                    .uppercase()
            )
            .append('\n')
    }

    out.append("  val COMMAND_LOOKUP = mapOf(\n")
    commands.forEach {
        out.append("    ")
            .append(
                it.name.toString()
            )
            .append(" to \"")
            .append(it.name)
            .append("\",\n")
    }
    out.append("  )")
    println(out.toString())
}

fun makeCommands() {
    val out = StringBuilder()
    val commands = Command.values().toList()
    val grouped = commands
        .groupBy { it.component }
    grouped.forEach { mapEntry ->
        out.append("  //region ")
            .append(mapEntry.key.name)
            .append("\n\n")
        mapEntry.value.forEach {
            out.append("  const val ")
                .append(it.name)
                .append(" = 0x")
                .append(
                    it.value
                        .toString(16)
                        .uppercase()
                )
                .append('\n')
        }
        out.append("\n  //endregion\n\n")
    }

    out.append("  val COMMAND_LOOKUP = mapOf(\n")
    commands.forEach {
        out.append("    0x")
            .append(
                ((it.component.value shl 16) + it.value)
                    .toString(16)
                    .uppercase()
            )
            .append(" to \"")
            .append(it.name)
            .append("\",\n")
    }
    out.append("  )")
    println(out.toString())
}