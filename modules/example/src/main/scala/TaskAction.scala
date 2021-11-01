package sjs.diffless.demo

object TaskAction {
	case object	Skip							extends TaskAction
	case object	Toggle							extends TaskAction
	case object	Remove							extends TaskAction
	case object	Edit							extends TaskAction
	final case class	Change(preview:String)	extends TaskAction
	case object	Commit							extends TaskAction
	case object	Rollback						extends TaskAction
}

sealed trait TaskAction
