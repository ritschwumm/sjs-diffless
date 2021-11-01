package sjs.diffless.demo

object Action {
	case object	Skip									extends Action
	case object	Boot									extends Action

	case object	Complete								extends Action
	final case class	Creating(text:String)			extends Action
	case object	Create									extends Action

	final case class	Task(id:TaskId, sub:TaskAction)	extends Action

	final case class	Filter(state:Option[Boolean])	extends Action
	case object	Clear									extends Action
}

/** sum type encompassing every action the user can request */
sealed trait Action
