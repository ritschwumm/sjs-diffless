package sjs.diffless.demo

import org.scalajs.dom.raw._

import sjs.diffless._
import sjs.diffless.dom._

object Demo {
	def main(args:Array[String]) {
		document addEventListener ("DOMContentLoaded", (_:Event) =>
			App start (
				container	= document.body,
				initial		= Model.initial,
				view		= Views.mainView,
				controller	= Controller.handle,
				boot		= Action.Boot
			)
		)
	}
}
