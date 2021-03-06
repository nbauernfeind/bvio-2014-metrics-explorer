package com.nefariouszhen.hackathon.data

import com.sun.jersey.api.client.{ClientResponse, ClientRequest, Client}
import com.google.inject.{Singleton, Provides, Inject}
import com.yammer.dropwizard.config.Environment
import java.util
import java.util.concurrent.Executors
import com.yammer.dropwizard.client.{JerseyClientBuilder, JerseyClientConfiguration}
import com.yammer.dropwizard.util.Duration
import com.nefariouszhen.hackathon.util.DropwizardPrivateModule
import javax.ws.rs.core.MediaType
import com.sun.jersey.api.client.filter.ClientFilter
import com.sun.jersey.core.util.MultivaluedMapImpl
import com.fasterxml.jackson.annotation.JsonProperty

case class DatadogCredentials(username: String, password: String)

case class ListMetricsResult(metrics: Array[String])
case class MetricsContextResult(timing: Double,
                                @JsonProperty("contexts_by_metric") contextMap: Map[String, Array[Array[String]]])

class DatadogClient @Inject() (creds: DatadogCredentials, client: Client, clientFilter: CookieClientFilter) {
  def login(): Unit = {
    clientFilter.clearCookies()

    // TODO, detect the difference between a login (via 302) and an invalid login.
    try {
      val formData = new MultivaluedMapImpl()
      formData.add("username", creds.username)
      formData.add("password", creds.password)
      client.resource("https://app.datadoghq.com/account/login")
        .queryParams(formData)
        .post()
    } catch {
      case _: Throwable =>
    }
  }

  def listMetrics(): ListMetricsResult = {
    client.resource("https://app.datadoghq.com/metric/list")
      .queryParam("window", "86400")
      .accept(MediaType.APPLICATION_JSON)
      .get(classOf[ListMetricsResult])
  }

  def listContextForMetrics(metrics: Iterable[String]): MetricsContextResult = {
    var builder = client.resource("https://app.datadoghq.com/metric/contexts_for_metrics")

    val formData = new MultivaluedMapImpl()
    for (metric <- metrics) {
      formData.add("metrics[]", metric)
    }

    builder.queryParam("window", "86400")
      .queryParams(formData)
      .accept(MediaType.APPLICATION_JSON)
      .post(classOf[MetricsContextResult])
  }
}

class DataModule(creds: DatadogCredentials) extends DropwizardPrivateModule {
  def doConfigure(): Unit = {
    bind[DatadogClient].asEagerSingleton()
    bind[DatadogCredentials].toInstance(creds)
    bind[ClientFilter].to[CookieClientFilter]
    bind[CookieClientFilter].asEagerSingleton()

    expose[DatadogClient]
  }

  def install(env: Environment): Unit = {

  }

  @Provides
  @Singleton
  def createJerseyClient(env: Environment, clientFilter: ClientFilter): Client = {
    val jerseyPool = Executors.newCachedThreadPool()
    val jerseyCfg = new JerseyClientConfiguration
    jerseyCfg.setTimeout(Duration.seconds(60))

    val client = new JerseyClientBuilder()
      .using(jerseyCfg)
      .using(jerseyPool, env.getObjectMapperFactory.build())
      .build()

    client.addFilter(clientFilter)

    client.setFollowRedirects(true)

    client
  }
}

class CookieClientFilter extends ClientFilter {
  private val cookies = new util.ArrayList[AnyRef]()

  def clearCookies(): Unit = cookies.clear()

  @Override
  def handle(request: ClientRequest): ClientResponse = {
    if (cookies != null) {
      request.getHeaders.put("Cookie", cookies)
    }

    val response = getNext.handle(request)
    if (response.getCookies != null) {
      cookies.addAll(response.getCookies)
    }

    response
  }
}
