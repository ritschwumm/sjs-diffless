package sjs.diffless

import org.scalajs.dom.raw._

object App {
	/** a simple way to start up an application */
	def start[M,A,H](container:HTMLElement, initial:M, view:View[M,A,H], controller:(M, A, Vector[H])=>(M, EventFlow), boot:A):EventFlow	= {
		var model	= initial
		var handles	= Vector.empty[H]

		lazy val (handles1, update)	= view.attach(container, model, dispatch)

		def dispatch(action:A):EventFlow	= {
			val (model1, flow)	= controller(model, action, handles)
			model	= model1
			handles	= update(model)
			flow
		}

		handles	= handles1

		dispatch(boot)
	}
}
