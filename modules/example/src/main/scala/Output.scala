package sjs.diffless.demo

object Output {
	final case class CreateText(focus:()=>Unit)			extends Output
	final case class Task(id:TaskId, sub:TaskOutput)	extends Output
}

/** DOM node markers when we have to call back into the node */
sealed trait Output
