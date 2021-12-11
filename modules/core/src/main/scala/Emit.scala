package sjs.diffless

import org.scalajs.dom.*

object Emit {
	def emitBuilder[N,M,A,E<:Event](attach:(N,E=>Unit)=>Unit):EmitBuilder[N,E]	= new EmitBuilder[N,E](attach)
	final class EmitBuilder[N,E<:Event](attach:(N,E=>Unit) => Unit) {
		def |=[A](handler:(N,E)=>A):Emit[N,A]	= action(attach, handler)
	}

	def action[N,A,E<:Event](attach:(N,E=>Unit) => Unit, handler:(N,E) => A):Emit[N,A]	=
		Emit { (target, dispatch) =>
			attach(
				target,
				(ev:E) => {
					val action		= handler(target, ev)
					val eventFlow	= dispatch(action)
					eventFlow applyTo ev
				}
			)
		}
}

/** tells an element to dispatch certain events */
final case class Emit[-N,+A](setup:(N,A=>EventFlow) => Unit) extends Child[N,Any,A,Nothing] {
	// TODO do we need this?
	def mapAction[AA](func:A=>AA):Emit[N,AA]	=
		Emit { (target, dispatch) =>
			setup(target, func andThen dispatch)
		}
}
