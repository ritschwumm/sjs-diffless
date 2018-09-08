package sjs.diffless

object Attr {
	def dynamic[N,M](setter:(N,M)=>Unit):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, initial)
				var old	= initial
				value => if (value != old) {
					old	= value
					setter(target, value)
				}
			}

	def dynamicUncached[N,M](setter:(N,M)=>Unit):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, initial)
				value => setter(target, value)
			}

	// TODO could just take Any and ignore the model
	def static[N,M,V](setter:(N,V)=>Unit, value:V):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, value)
				// TODO optimize - should return something ignorable by the caller
				(value) => ()
			}
}

final case class Attr[-N,-M](setup:(N,M) => (M => Unit)) extends Child[N,M,Nothing,Nothing] {
	def ~=[MM](func:MM=>M):Attr[N,MM]	= adaptModel(func)
	// TODO optimize
	def :=[MM](value:M):Attr[N,MM]		= adaptModel(_ => value)

	// TODO auto-cache?
	def adaptModel[MM](func:MM=>M):Attr[N,MM]	=
			Attr { (target, initial) =>
				func andThen setup(target, func(initial))
			}

	def caching:Attr[N,M]	=
			Attr { (target, initial) =>
				val base	= setup(target, initial)
				var old		= initial
				value => {
					if (value != old) {
						old	= value
						base(value)
					}
				}
			}

	/*
	// sets a value once and ignores further updates
	def constant(value:M):Attr[N,M]	=
			Attr { (target, initial) =>
				setup(target, initial)
				(value) => ()
			}
	*/
}
