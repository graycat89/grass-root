package lineserver

import com.google.common.cache.LoadingCache
import io.ktor.application.ApplicationCall
import io.ktor.features.NotFoundException
import io.ktor.response.respondText
import java.io.File
import java.io.FileNotFoundException

class LineService(val diffOn: Boolean = false, val compareList: List<String>, val partitionRoot: String, val partitionSize: Int, val cache: LoadingCache<Long, String>) {
    suspend fun serveLine(call: ApplicationCall) {
        val index = call.parameters["index"]?.toLongOrNull() ?: throw NotFoundException()
        val result = cache[index]

        if (diffOn) {
            call.application.environment.log.info("\nExpected line: ${compareList[index.toInt()]} \n Actual line: ${cache[index]}")
        }

        call.respondText {
            result
        }
    }
}

class CacheService(val partitionRoot: String, val partitionSize: Int) {
    /*
        Convert global index to local index of the partition
     */
    fun getLocalIndex(index: Long) = (index % this.partitionSize).toInt()

    fun loadValue(key: Long): String {
        val start = (key / this.partitionSize) * this.partitionSize + 1
        val end = start + this.partitionSize - 1;
        val partitionName = "output-$start-$end.txt"
        val file = File("$partitionRoot/$partitionName")
        val values = mutableListOf<String>()

        try {
            file.forEachLine(Charsets.UTF_8) { line ->
                values.add(line)
            }
        } catch (ex: FileNotFoundException) {
            throw NotFoundException("Could not find the partion file for index $key")
        }

        return values[getLocalIndex(key)]
    }
}
