package sjs.diffless.demo

/** DOM node markers when we have to call back into the node */
enum Handle {
	case CreateText(focus:()=>Unit)
	case Task(id:TaskId, sub:TaskHandle)
}
