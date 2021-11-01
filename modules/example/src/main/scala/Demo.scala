package sjs.diffless.demo

import org.scalajs.dom._

import sjs.diffless._

/** starts up our little demo application */
object Demo {
	def main(args:Array[String]):Unit	= {
		document.addEventListener("DOMContentLoaded", (_:Event) =>
			App.start(
				container	= document.body,
				initial		= Model.initial,
				view		= Views.mainView,
				controller	= Controller.execute,
				boot		= Action.Boot
			)
		)
	}
}
