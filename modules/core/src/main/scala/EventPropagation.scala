package sjs.diffless

/** propagation mode of an event in the DOM tree */
enum EventPropagation {
	case Propagate
	case Stop
	case StopImmediate
}
