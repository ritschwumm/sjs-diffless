package sjs.diffless

object EventDefaultAction {
	object Permit	extends EventDefaultAction
	object Prevent	extends EventDefaultAction
}
sealed trait EventDefaultAction
