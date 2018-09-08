package sjs.diffless

import sjs.diffless.dom._

final case class Tag[N](name:String) {
	def create():N	=  document.createElement(name).asInstanceOf[N]
}
