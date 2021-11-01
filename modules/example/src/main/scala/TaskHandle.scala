package sjs.diffless.demo

object TaskHandle {
	final case class Editor(focus:()=>Unit)	extends TaskHandle
}

/** DOM node markers belonging to a specific Task */
sealed trait TaskHandle
