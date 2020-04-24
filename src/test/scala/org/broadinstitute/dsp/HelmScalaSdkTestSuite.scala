package org.broadinstitute.dsp

import cats.effect.concurrent.Semaphore
import cats.effect.{Blocker, ContextShift, IO, Timer}
import io.chrisdavenport.log4cats.StructuredLogger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import org.scalatest.matchers.should.Matchers

import scala.concurrent.ExecutionContext.global

trait HelmScalaSdkTestSuite extends Matchers {
  implicit val timer: Timer[IO] = IO.timer(global)
  implicit val cs: ContextShift[IO] = IO.contextShift(global)
  implicit val logger: StructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val blocker = Blocker.liftExecutionContext(global)
  val semaphore = Semaphore[IO](10).unsafeRunSync()
}
