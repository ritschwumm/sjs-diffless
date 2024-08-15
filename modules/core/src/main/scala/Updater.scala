package sjs.diffless

import org.scalajs.dom.*

/** updates zero or more DOM nodes when the model changes */
abstract class Updater[-M,+H] { self =>
	/** returns expired nodes */
	def update(value:M):Vector[Node]
	/** current child nodes, might change in #update */
	def active:Vector[Node]
	/** current handles, might change in #update */
	def handles:Vector[H]

	def adapt[MM,HH](modelFunc:MM=>M, handleFunc:H=>HH):Updater[MM,HH]	=
		new Updater[MM,HH] {
			def update(value:MM):Vector[Node]	= self.update(modelFunc(value))
			def active:Vector[Node]				= self.active
			def handles:Vector[HH]				= self.handles.map(handleFunc)
		}

	// TODO auto-cache?
	def adaptModel[MM](func:MM=>M):Updater[MM,H]	=
		new Updater[MM,H] {
			def update(value:MM):Vector[Node]	= self.update(func(value))
			def active:Vector[Node]				= self.active
			def handles:Vector[H]				= self.handles
		}

	def adaptHandle[HH](func:H=>HH):Updater[M,HH]	=
		new Updater[M,HH] {
			def update(value:M):Vector[Node]	= self.update(value)
			def active:Vector[Node]				= self.active
			def handles:Vector[HH]				= self.handles.map(func)
		}

	def dropHandle:Updater[M,Nothing]	=
		new Updater[M,Nothing] {
			def update(value:M):Vector[Node]	= self.update(value)
			def active:Vector[Node]				= self.active
			def handles:Vector[Nothing]			= Vector.empty
		}

	/*
	def modifyHandles[HH](func:Vector[H]=>Vector[HH]):Updater[M,HH]	=
		new Updater[M,HH] {
			def update(value:M):Vector[Node]	= self.update(value)
			def active:Vector[Node]				= self.active
			def handle:Vector[HH]				= func(self.handle)
		}
	*/

	/*
	def contextualHandle[MM<:M,HH](initial:MM, func:(MM,H)=>HH):Updater[MM,HH]	=
		new Updater[MM,HH] {
			var model:MM	= initial
			def update(value:MM):Vector[Node]	= {
				model	= value
				self.update(value)
			}
			def active:Vector[Node]				= self.active
			def handles:Vector[HH]				= self.handles.map { it => func(model, it) }
		}
	*/

	def caching(initial:M):Updater[M,H]	=
		new Updater[M,H] {
			var old		= initial
			var active	= self.active
			var handles	= self.handles
			def update(value:M):Vector[Node]	=
				if (value != old) {
					old		= value
					val expired	= self.update(value)
					active	= self.active
					handles	= self.handles
					expired
				}
				else {
					Vector.empty
				}
		}

	def static:Updater[Any,H]	=
		new Updater[Any,H] {
			def update(value:Any):Vector[Node]	= Vector.empty
			val active:Vector[Node]				= self.active
			val handles:Vector[H]				= self.handles
		}

	def lively[MM](alive:MM=>Boolean, func:MM=>M):Updater[MM,H]	=
		new Updater[MM,H] {
			def update(value:MM):Vector[Node]	=
					if (alive(value))	self.update(func(value))
					else				Vector.empty
			def active:Vector[Node]	= self.active
			def handles:Vector[H]	= self.handles
		}
}
