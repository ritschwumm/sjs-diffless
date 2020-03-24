package sjs.diffless.demo

object Exported {
	final case class CreateText(focus:()=>Unit)			extends Exported
	final case class Task(id:TaskId, sub:TaskExported)	extends Exported
}

/** DOM node markers when we have to call back into the node */
sealed trait Exported
