package sjs.diffless

import scala.scalajs.js
import org.scalajs.dom.raw._

/** dom helpers */
@deprecated("use org.scalajs.dom", "0.30.0")
object dom {
	@deprecated("use org.scalajs.dom.window", "0.30.0")
	lazy val window:Window			= js.Dynamic.global.window.asInstanceOf[Window]
	@deprecated("use org.scalajs.dom.document", "0.30.0")
	lazy val document:HTMLDocument	= window.document
}
