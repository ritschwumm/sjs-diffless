package sjs.diffless.demo

object TaskAction {
	final case object	Skip					extends TaskAction
	final case object	Toggle					extends TaskAction
	final case object	Remove					extends TaskAction
	final case object	Edit					extends TaskAction
	final case class	Change(preview:String)	extends TaskAction
	final case object	Commit					extends TaskAction
	final case object	Rollback				extends TaskAction
}

sealed trait TaskAction
