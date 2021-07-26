package org.broadinstitute.dsp

import cats.effect.IO
import cats.effect.std.Semaphore
import cats.effect.unsafe.implicits.global
import org.scalatest.matchers.should.Matchers
import org.typelevel.log4cats.StructuredLogger
import org.typelevel.log4cats.slf4j.Slf4jLogger

trait HelmScalaSdkTestSuite extends Matchers {
  implicit val logger: StructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  val semaphore = Semaphore[IO](10).unsafeRunSync()
}
