package org.broadinstitute.dsp

import cats.Show
import cats.implicits._
import org.broadinstitute.dsp.HelmJnaClient.GoString
import scala.language.implicitConversions

object implicits {
  implicit val showNameSpace: Show[Namespace] = Show.show[Namespace](_.asString)
  implicit val showKubeToken: Show[KubeToken] = Show.show[KubeToken](_.asString)
  implicit val showKubeApiServer: Show[KubeApiServer] = Show.show[KubeApiServer](_.asString)
  implicit val showRelease: Show[Release] = Show.show[Release](_.asString)
  implicit val showChart: Show[Chart] = Show.show[Chart](_.asString)
  implicit val showValues: Show[Values] = Show.show[Values](_.asString)

  implicit def toGoString[A: Show](a: A): GoString.ByValue = {
    val goString = new HelmJnaClient.GoString.ByValue()
    goString.p = a.show
    goString.n = a.show.length
    goString
  }
}
