package com.nefariouszhen.hackathon.index

import com.google.inject.Inject
import com.nefariouszhen.hackathon.data.DatadogClient
import com.yammer.dropwizard.lifecycle.Managed
import scala.collection.mutable

class Index @Inject() (client: DatadogClient) extends Managed {
  val keys = mutable.HashSet[String]()

  def start(): Unit = {
    client.login()
    client.listMetrics().metrics.foreach(metric => keys += metric)
    for ((metric, matches) <- client.listContextForMetricsPost(keys.take(20)).contextMap) {
      matches.map(_.mkString(", ")).foreach(println)
    }
  }

  def stop(): Unit = {}
}
