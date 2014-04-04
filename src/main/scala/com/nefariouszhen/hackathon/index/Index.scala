package com.nefariouszhen.hackathon.index

import com.google.inject.Inject
import com.nefariouszhen.hackathon.data.{MetricsContextResult, DatadogClient}
import com.yammer.dropwizard.lifecycle.Managed
import scala.collection.mutable
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import java.io.{PrintWriter, FileOutputStream, BufferedOutputStream, File}
import com.fasterxml.jackson.module.scala.DefaultScalaModule

class Index @Inject() (client: DatadogClient) extends Managed {
  val objectMapper = new ObjectMapper with ScalaObjectMapper
  objectMapper.registerModule(DefaultScalaModule)

  val keys = mutable.HashSet[String]()

  def start(): Unit = {
    val fileCache = new File("data/cache.json")
    if (!fileCache.exists()) {
      fileCache.createNewFile()
    }

    val outputStream = new PrintWriter(new BufferedOutputStream(new FileOutputStream(fileCache, true)))
    try {
      client.login()

      // Load Cached Data
      val contextIterator = new Iterator[MetricsContextResult] {
        val jacksonIterator = objectMapper.reader[MetricsContextResult].readValues(fileCache)

        def hasNext: Boolean = jacksonIterator.hasNext

        def next(): MetricsContextResult = jacksonIterator.next()
      }

      contextIterator.foreach(indexContext)
      println("Have reloaded " + keys.size + " contexts from cache.")

      // Fetch all new metric names
      val metricIterator = client.listMetrics().metrics.iterator.filter(k => !keys.contains(k))

      // For these metrics, fetch tags
      while (metricIterator.hasNext) {
        try {
          val context = client.listContextForMetrics(metricIterator.take(25).toList)
          indexContext(context)
          outputStream.println(objectMapper.writeValueAsString(context))
          outputStream.flush()
        } catch {
          case t: Throwable =>
            // TODO, make the datadog client restartable properly.
            println("Failed to fetch metrics. Be sure to restart.")
            Thread.sleep(2500)
            client.login()
        }
      }
    } finally {
      outputStream.close()
    }

    println("Completed Indexing!!")
  }

  def stop(): Unit = {}

  private[this] def indexContext(context: MetricsContextResult) {
    for ((key, matches) <- context.contextMap) {
      keys += key
    }
  }
}
