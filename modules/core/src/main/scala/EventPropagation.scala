package sjs.diffless

object EventPropagation {
	object Propagate		extends EventPropagation
	object Stop				extends EventPropagation
	object StopImmediate	extends EventPropagation
}
sealed trait EventPropagation
