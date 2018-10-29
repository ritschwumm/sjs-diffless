package sjs.diffless

object Attr {
	def dynamic[N,M](setter:(N,M)=>Unit):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, initial)
				var old	= initial
				Some(
					value => if (value != old) {
						old	= value
						setter(target, value)
					}
				)
			}

	def dynamicUncached[N,M](setter:(N,M)=>Unit):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, initial)
				Some(
					value => setter(target, value)
				)
			}

	def static[N,M,V](setter:(N,V)=>Unit, value:V):Attr[N,M]	=
			Attr { (target, initial) =>
				setter(target, value)
				None
			}
}

final case class Attr[-N,-M](setup:(N,M) => Option[(M => Unit)]) extends Child[N,M,Nothing,Nothing] {
	def ~=[MM](func:MM=>M):Attr[N,MM]	= adaptModelCached(func)
	def :=[MM](value:M):Attr[N,MM]		= static(value)

	def adaptModel[MM](func:MM=>M):Attr[N,MM]	=
			Attr { (target, initial) =>
				val baseOpt:Option[M=>Unit]	= setup(target, func(initial))
				baseOpt map func.andThen
			}

	def adaptModelCached[MM](func:MM=>M):Attr[N,MM]	=
			Attr { (target, initial) =>
				val baseOpt:Option[M=>Unit]	= setup(target, func(initial))
				baseOpt map { base =>
					var old	= initial
					value	=> {
						if (value != old) {
							old	= value
							base(func(value))
						}
					}
				}
			}

	def caching:Attr[N,M]	=
			Attr { (target, initial) =>
				val baseOpt:Option[M=>Unit]	= setup(target, initial)
				baseOpt map { base =>
					var old		= initial
					value => {
						if (value != old) {
							old	= value
							base(value)
						}
					}
				}
			}

	/** sets a value once and ignores further updates */
	def static[MM](value:M):Attr[N,MM]	=
			Attr { (target, initial) =>
				setup(target, value)
				None
			}
}
