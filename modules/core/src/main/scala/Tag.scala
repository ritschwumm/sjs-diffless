package sjs.diffless

import sjs.diffless.dom._

/** represents an HTML tag and know the DOM type representing this tag */
final case class Tag[N](name:String) {
	def create():N	=  document.createElement(name).asInstanceOf[N]
}
