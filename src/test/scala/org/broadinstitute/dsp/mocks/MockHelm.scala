package org.broadinstitute.dsp.mocks

import cats.data.Kleisli
import cats.effect.IO
import org.broadinstitute.dsp.{AuthContext, ChartName, ChartVersion, HelmAlgebra, Release, Values}

class MockHelm extends HelmAlgebra[IO] {
  override def installChart(release: Release,
                            chartName: ChartName,
                            chartVersion: ChartVersion,
                            values: Values,
                            createNamespace: Boolean = false): Kleisli[IO, AuthContext, Unit] =
    Kleisli.pure(())

  override def listHelm(): Kleisli[IO, AuthContext, Unit] = Kleisli.pure(())

  override def pullChart(chartName: ChartName,
                         chartVersion: ChartVersion,
                         destDir: String): Kleisli[IO, AuthContext, Unit] =
    Kleisli.pure(())

  override def uninstall(release: Release, keepHistory: Boolean): Kleisli[IO, AuthContext, Unit] = Kleisli.pure(())

  override def upgradeChart(release: Release,
                            chartName: ChartName,
                            chartVersion: ChartVersion,
                            values: Values): Kleisli[IO, AuthContext, Unit] =
<<<<<<< Updated upstream
    Kleisli.pure(())

  override def pullChart(chartName: ChartName, chartVersion: ChartVersion): Kleisli[IO, AuthContext, Unit] =
=======
>>>>>>> Stashed changes
    Kleisli.pure(())
}

object MockHelm extends MockHelm
