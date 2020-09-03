package org.broadinstitute.dsp

import cats.Show
import cats.implicits._
import org.broadinstitute.dsp.HelmJnaClient.GoString
import scala.language.implicitConversions

object implicits {
  implicit val showNameSpace: Show[Namespace] = Show.show[Namespace](_.asString)
  implicit val showKubeToken: Show[KubeToken] = Show.show[KubeToken](_.asString)
  implicit val showKubeApiServer: Show[KubeApiServer] = Show.show[KubeApiServer](_.asString)
  implicit val showCaCertFile: Show[CaCertFile] = Show.show[CaCertFile](_.path.normalize().toString)
  implicit val showRelease: Show[Release] = Show.show[Release](_.asString)
  implicit val showChartName: Show[ChartName] = Show.show[ChartName](_.asString)
  implicit val showChart: Show[ChartVersion] = Show.show[ChartVersion](_.asString)
  implicit val showValues: Show[Values] = Show.show[Values](_.asString)

  implicit def toGoString[A: Show](a: A): GoString.ByValue = {
    val goString = new HelmJnaClient.GoString.ByValue()
    goString.p = a.show
    goString.n = a.show.length
    goString
  }

  implicit def toGoBool(b: Boolean): Byte =
    if (b) 1 else 0
}
