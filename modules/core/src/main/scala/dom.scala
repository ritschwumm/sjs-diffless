package sjs.diffless

import scala.scalajs.js
import org.scalajs.dom.raw._

/** dom helpers */
object dom {
	lazy val window:Window			= js.Dynamic.global.window.asInstanceOf[Window]
	lazy val document:HTMLDocument	= window.document
}
