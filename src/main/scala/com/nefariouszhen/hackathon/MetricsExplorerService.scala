package com.nefariouszhen.hackathon

import com.bazaarvoice.dropwizard.redirect.{RedirectBundle, HttpsRedirect, UriRedirect}
import com.bazaarvoice.dropwizard.assets.{AssetsConfiguration, AssetsBundleConfiguration, ConfiguredAssetsBundle}
import com.bazaarvoice.dropwizard.webjars.WebJarBundle
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.inject.Guice
import com.nefariouszhen.hackathon.index.IndexModule
import com.yammer.dropwizard.config.{Environment, Bootstrap, Configuration}
import com.yammer.dropwizard.ScalaService
import com.yammer.dropwizard.bundles.ScalaBundle
import com.nefariouszhen.hackathon.data.{DatadogCredentials, DataModule}
import com.nefariouszhen.hackathon.util.DropwizardPublicModule

class MetricsExplorerConfiguration extends Configuration with AssetsBundleConfiguration {
  @JsonProperty
  var datadog = DatadogCredentials("user", "pass")

  @JsonProperty
  var assets = new AssetsConfiguration()

  def getAssetsConfiguration: AssetsConfiguration = assets
}

object MetricsExplorerService extends ScalaService[MetricsExplorerConfiguration] {
  def initialize(bootstrap: Bootstrap[MetricsExplorerConfiguration]) {
    bootstrap.setName("MetricsExplorer")
    bootstrap.addBundle(new ScalaBundle)
    bootstrap.addBundle(new WebJarBundle)
    bootstrap.addBundle(new ConfiguredAssetsBundle("/app/", "/app/"))

    import scala.collection.JavaConversions._
    bootstrap.addBundle(new RedirectBundle(
      new HttpsRedirect(),
      new UriRedirect(Map(
        "/" -> "/app/",
        "/index.htm" -> "/app/",
        "/index.html" -> "/app/",
        "/app" -> "/app/"
      ))
    ))
  }

  def run(configuration: MetricsExplorerConfiguration, environment: Environment) {
    val modules = List(
      new IndexModule(),
      new DataModule(configuration.datadog),
      new DropwizardPublicModule {
        def doConfigure(): Unit = {
          bind[MetricsExplorerConfiguration].toInstance(configuration)
          bind[Environment].toInstance(environment)
        }
        def install(env: Environment): Unit = {}
      }
    )

    Guice.createInjector(modules: _*)

    modules.foreach(_.install(environment))
  }
}
