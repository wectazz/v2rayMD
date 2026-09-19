import com.v2ray.md.fmt.CustomFmt
import com.v2ray.md.util.JsonUtil
import java.io.File

fun main() {
    val content = File("D:/v2rayMD/auto1.txt").readText()
    val config = CustomFmt.parse(content)
    println("Config remarks: ${config.remarks}")
    println("Config server: ${config.server}")
    println("Config port: ${config.serverPort}")
}
