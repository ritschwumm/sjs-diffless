package sjs.diffless

import org.scalajs.dom.raw._

object Emit {
	def emitBuilder[N,M,A,E<:Event](setter:(N,E=>Unit)=>Unit):EmitBuilder[N,E]	= new EmitBuilder[N,E](setter)
	final class EmitBuilder[N,E<:Event](setter:(N,E=>Unit) => Unit) {
		def |=[A](handler:(N,E)=>A):Emit[N,A]	= action(setter, handler)
	}

	def action[N,A,E<:Event](setter:(N,E=>Unit) => Unit, handler:(N,E) => A):Emit[N,A]	=
			Emit { (target, dispatch) =>
				setter(
					target,
					(ev:E) => {
						val action	= handler(target, ev)
						val EventFlow(defaultAction, propagation)	= dispatch(action)
						defaultAction match {
							case EventDefaultAction.Permit		=>
							case EventDefaultAction.Prevent		=> ev.preventDefault()
						}
						propagation match {
							case EventPropagation.Propagate		=>
							case EventPropagation.Stop			=> ev.stopPropagation()
							case EventPropagation.StopImmediate	=> ev.stopImmediatePropagation()
						}
					}
				)
			}
}

final case class Emit[-N,+A](setup:(N,A=>EventFlow) =>Unit) extends Child[N,Any,A,Nothing] {
	// TODO do we need this?
	def mapAction[AA](func:A=>AA):Emit[N,AA]	=
			Emit { (target, dispatch) =>
				setup(target, func andThen dispatch)
			}
}
