package sjs.diffless

object Attribute {
	// behaves like dynamicUncached(setter).caching
	def dynamic[N,M](setter:(N,M)=>Unit):Attribute[N,M]	=
			Attribute(
				requiresUpdates	= true,
				setup	= (target, initial) => {
					setter(target, initial)
					var old	= initial
					value => if (value != old) {
						old	= value
						setter(target, value)
					}
				}
			)

	def dynamicUncached[N,M](setter:(N,M)=>Unit):Attribute[N,M]	=
			Attribute(
				requiresUpdates	= true,
				setup	= (target, initial) => {
					setter(target, initial)
					value => setter(target, value)
				}
			)

	def static[N,M,V](setter:(N,V)=>Unit, value:V):Attribute[N,M]	=
			Attribute(
				requiresUpdates	= false,
				setup	= (target, initial) => {
					setter(target, value)
					ignoring
				}
			)

	val ignoring:Any=>Unit	= _ => sys error "unexpectedly tried to update an attribute"
}

/** tells an element to set one of its attribute values and keep it up-to-date */
final case class Attribute[-N,-M](
	requiresUpdates:Boolean,
	// if requiresUpdates is false, the function returned here will not be called
	// it should return Attribute.ignoring in this case
	setup:(N,M) => (M => Unit)
)
extends Child[N,M,Nothing,Nothing] {
	def ~=[MM](func:MM=>M):Attribute[N,MM]	= adaptModelCached(func)
	def :=[MM](value:M):Attribute[N,MM]		= static(value)

	def adaptModel[MM](func:MM=>M):Attribute[N,MM]	=
			Attribute(
				requiresUpdates	= requiresUpdates,
				setup	= (target, initial) => {
					val base	= setup(target, func(initial))
					if (requiresUpdates)	func andThen base
					else					Attribute.ignoring
				}
			)

	def adaptModelCached[MM](func:MM=>M):Attribute[N,MM]	=
			Attribute(
				requiresUpdates	= requiresUpdates,
				setup	= (target, initial) => {
					val base:M=>Unit	= setup(target, func(initial))
					if (requiresUpdates) {
						var old	= initial
						value	=> {
							if (value != old) {
								old	= value
								base(func(value))
							}
						}
					}
					else Attribute.ignoring
				}
			)

	def caching:Attribute[N,M]	=
			if (requiresUpdates) {
				Attribute(
					requiresUpdates	= requiresUpdates,
					setup	= (target, initial) => {
						val base:M=>Unit	= setup(target, initial)
						if (requiresUpdates) {
							var old		= initial
							value => {
								if (value != old) {
									old	= value
									base(value)
								}
							}
						}
						else Attribute.ignoring
					}
				)
			}
			else this

	/** sets a value once and ignores further updates */
	def static[MM](value:M):Attribute[N,MM]	=
			Attribute(
				requiresUpdates	= false,
				setup	= (target, initial) => {
					setup(target, value)
					Attribute.ignoring
				}
			)
}
