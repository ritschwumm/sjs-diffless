package sjs.diffless.demo

object Handle {
	final case class CreateText(focus:()=>Unit)			extends Handle
	final case class Task(id:TaskId, sub:TaskHandle)	extends Handle
}

/** DOM node markers when we have to call back into the node */
sealed trait Handle
