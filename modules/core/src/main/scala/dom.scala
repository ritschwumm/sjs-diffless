package sjs.diffless

import scala.scalajs.js
import org.scalajs.dom.raw._

object dom {
	lazy val window:Window			= js.Dynamic.global.asInstanceOf[Window]
	lazy val document:HTMLDocument	= window.document
}
