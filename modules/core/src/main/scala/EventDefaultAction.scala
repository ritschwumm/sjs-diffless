package sjs.diffless

object EventDefaultAction {
	object Permit	extends EventDefaultAction
	object Prevent	extends EventDefaultAction
}

/** how to deal with the default action of a DOM event */
sealed trait EventDefaultAction
