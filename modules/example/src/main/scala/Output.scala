package sjs.diffless.demo

object Output {
	final case class CreateText(focus:()=>Unit)			extends Output
	final case class Task(id:TaskId, sub:TaskOutput)	extends Output
}

sealed trait Output
