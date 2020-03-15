package sjs.diffless

object EventFlow {
	val permit	=
		EventFlow(
			EventDefaultAction.Permit,
			EventPropagation.Propagate
		)
}

/** what to do after an event has been dealt with */
final case class EventFlow(
	defaultAction:EventDefaultAction,
	propagation:EventPropagation
) {
	def preventDefault:EventFlow			= copy(defaultAction	= EventDefaultAction.Prevent)
	def stopPropagation:EventFlow			= copy(propagation		= EventPropagation.Stop)
	def stopImmediatePropagation:EventFlow	= copy(propagation		= EventPropagation.StopImmediate)
}
