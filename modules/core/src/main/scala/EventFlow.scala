package sjs.diffless

object EventFlow {
	val permit	=
			EventFlow(
				EventDefaultAction.Permit,
				EventPropagation.Propagate
			)
}

final case class EventFlow(
	defaultAction:EventDefaultAction,
	propagation:EventPropagation
) {
	def preventDefault:EventFlow			= copy(defaultAction	= EventDefaultAction.Prevent)
	def stopPropagation:EventFlow			= copy(propagation		= EventPropagation.Stop)
	def stopImmediatePropagation:EventFlow	= copy(propagation		= EventPropagation.StopImmediate)
}
