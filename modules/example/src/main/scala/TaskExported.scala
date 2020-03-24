package sjs.diffless.demo

object TaskExported {
	final case class Editor(focus:()=>Unit)	extends TaskExported
}

/** DOM node markers belonging to a specific Task */
sealed trait TaskExported
