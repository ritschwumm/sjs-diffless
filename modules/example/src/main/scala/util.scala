package sjs.diffless.demo

import scala.scalajs.js.timers.RawTimers

/** some utility functions */
object util {
	def constant[S,T](it:T):S=>T	= _ => it
	def dependent[T,X](get:T=>X, mod:X=>T=>T):T=>T	= it => mod(get(it))(it)

	//------------------------------------------------------------------------------

	def runLater(action:()=>Unit):Unit	= {
		RawTimers.setTimeout(action, 1)
	}

	def runNow(action:()=>Unit):Unit	= {
		action()
	}

	//------------------------------------------------------------------------------

	/*
	implicit class AnyExt[T](peer:T) {
		def into[U](func:T=>U):U	= func(peer)

		def matchOption[U](pf:PartialFunction[T,U]):Option[U]	=
			if (pf isDefinedAt peer)	Some(pf(peer))
			else						None
	}
	*/
}
