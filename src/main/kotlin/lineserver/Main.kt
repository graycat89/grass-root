package lineserver

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.NotFoundException
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import java.io.File
import java.util.concurrent.TimeUnit

fun Application.mainModule(args: Array<String>) {
    val filename = args.first()
    val size = System.getenv("PARTITION_SIZE").toInt()
    val diffOn = System.getenv("DIFF_ON").toBoolean()
    val partitionRoot = System.getProperty("PARTITION_ROOT", "/Users/mli/work/line-server/tmp")
    val cacheService = CacheService(partitionRoot, size)
    val cache: LoadingCache<Long, String> = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .build(object : CacheLoader<Long, String>() {
                override fun load(key: Long) = cacheService.loadValue(key)
            })

    // load the origin file into map for comparison
    val compareList = mutableListOf<String>()
    if (diffOn) {
        File(filename).forEachLine { line ->
            compareList.add(line)
        }
    }

    val service = LineService(diffOn, compareList, partitionRoot, size, cache)

    environment.log.info("filename: $filename\npartition size=$size\ndiffOn=$diffOn")

    install(DefaultHeaders)
    install(StatusPages) {
        exception<NotFoundException> { cause ->
            cause.printStackTrace()
            call.respond(HttpStatusCode.NotFound, "Line content not found. Probably caused by index out of bound")
        }
        exception<Throwable> { cause ->
            cause.printStackTrace()
            call.respond(HttpStatusCode.InternalServerError, "Unexpected Error")
        }
    }

    routing {
        route("/lines") {
            get("/{index}") {
                service.serveLine(call)
            }
        }
    }
}

fun main(args: Array<String>) {
    if (args.size == 0) {
        error("missing commandLine arg")
    }

    embeddedServer(Netty, port = 5005) {
        mainModule(args)
    }.start(wait = true)
}