package sjs.diffless.demo

object Action {
	final case object	Skip								extends Action
	final case object	Boot								extends Action

	final case object	Complete							extends Action
	final case class	Creating(text:String)				extends Action
	final case object	Create								extends Action

	final case class	Task(id:TaskId, sub:TaskAction)		extends Action

	final case class	Filter(state:Option[Boolean])		extends Action
	final case object	Clear								extends Action
}

/** sum type encompassing every action the user can request */
sealed trait Action
