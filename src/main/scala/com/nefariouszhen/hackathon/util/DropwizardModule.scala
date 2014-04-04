package com.nefariouszhen.hackathon.util

import com.google.inject._
import net.codingwell.scalaguice.{ScalaPrivateModule, ScalaModule, InternalModule}
import com.yammer.dropwizard.config.Environment

trait DropwizardModule[B <: Binder] extends Module {
  self: InternalModule[B] =>

  private[this] var injectorProvider: Provider[Injector] = null

  final def configure() {
    binderAccess.requireExplicitBindings()
    injectorProvider = getProvider[Injector]
    doConfigure()
  }

  protected[this] def injector = injectorProvider.get()

  def doConfigure()
  def install(env: Environment)
}

abstract class DropwizardPublicModule extends ScalaModule with DropwizardModule[Binder]

abstract class DropwizardPrivateModule extends ScalaPrivateModule with DropwizardModule[PrivateBinder]
