package org.broadinstitute.dsp.mocks

import cats.data.Kleisli
import cats.effect.IO
import org.broadinstitute.dsp.{AuthContext, Chart, HelmAlgebra, Release, Values}

class MockHelm extends HelmAlgebra[IO] {
  override def installChart(release: Release, chart: Chart, values: Values): Kleisli[IO, AuthContext, Unit] =
    Kleisli.pure(())

  override def listHelm(): Kleisli[IO, AuthContext, Unit] = Kleisli.pure(())

  override def uninstall(release: Release): Kleisli[IO, AuthContext, Unit] = Kleisli.pure(())
}

object MockHelm extends MockHelm
