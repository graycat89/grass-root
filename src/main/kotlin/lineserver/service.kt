package lineserver

import com.google.common.cache.LoadingCache
import io.ktor.application.ApplicationCall
import io.ktor.features.NotFoundException
import io.ktor.response.respondText
import java.io.File
import java.io.FileNotFoundException

@io.ktor.util.KtorExperimentalAPI
class LineService(val diffOn: Boolean = false, val compareList: List<String>, val partitionRoot: String, val partitionSize: Int, val cache: LoadingCache<Long, String?>) {
    suspend fun serveLine(call: ApplicationCall) {
        val index = call.parameters["index"]?.toLongOrNull() ?: throw NotFoundException("Probably index of out bound")
        val ret = cache[index] ?: throw NotFoundException("Probably index of out bound")
        if (diffOn) {
            call.application.environment.log.info("\nExpected line: ${compareList[index.toInt()]} \n Actual line: $ret")
        }

        call.respondText {
            ret
        }
    }
}

@io.ktor.util.KtorExperimentalAPI
class CacheService(val partitionRoot: String, val partitionSize: Int) {
    /*
        Convert global index to local index of the partition
     */
    fun getLocalIndex(index: Long) = (index % this.partitionSize).toInt()

    fun loadValue(key: Long): String? {
        val start = (key / this.partitionSize) * this.partitionSize + 1
        val end = start + this.partitionSize - 1;
        val partitionName = "output-$start-$end.txt"
        val file = File("$partitionRoot/$partitionName")
        val values = mutableListOf<String>()

        try {
            // we can also return as soon as the requested line is found
            // since the partition is not expect to be large
            // it't ok to keep reading til the end of the partition and then return by localIndex
            file.forEachLine(Charsets.UTF_8) { line ->
                values.add(line)
            }
        } catch (ex: FileNotFoundException) {
            return null
        }

        return values[getLocalIndex(key)]
    }
}
