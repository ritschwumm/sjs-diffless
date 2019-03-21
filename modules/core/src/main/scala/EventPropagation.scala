package sjs.diffless

object EventPropagation {
	object Propagate		extends EventPropagation
	object Stop				extends EventPropagation
	object StopImmediate	extends EventPropagation
}

/** propagation mode of an event in the DOM tree */
sealed trait EventPropagation
