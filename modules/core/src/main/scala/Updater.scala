package sjs.diffless

import org.scalajs.dom.raw._

abstract class Updater[-M,+H] { self =>
	/** returns expired nodes */
	def update(value:M):Vector[Node]
	def active:Vector[Node]
	def handle:Vector[H]

	def adapt[MM,HH](modelFunc:MM=>M, handleFunc:H=>HH):Updater[MM,HH]	=
			new Updater[MM,HH] {
				def update(value:MM):Vector[Node]	= self update modelFunc(value)
				def active:Vector[Node]				= self.active
				def handle:Vector[HH]				= self.handle map handleFunc
			}

	// TODO auto-cache?
	def adaptModel[MM](func:MM=>M):Updater[MM,H]	=
			new Updater[MM,H] {
				def update(value:MM):Vector[Node]	= self update func(value)
				def active:Vector[Node]				= self.active
				def handle:Vector[H]				= self.handle
			}

	/*
	def contextualHandle[MM<:M,HH](initial:MM, func:(MM,H)=>HH):Updater[MM,HH]	=
			new Updater[MM,HH] {
				var model:MM	= initial
				def update(value:MM):Vector[Node]	= {
					model	= value
					self update value
				}
				def active:Vector[Node]				= self.active
				def handle:Vector[HH]				= self.handle map { it => func(model, it) }
			}
	*/

	def adaptHandle[HH](func:H=>HH):Updater[M,HH]	=
			new Updater[M,HH] {
				def update(value:M):Vector[Node]	= self update value
				def active:Vector[Node]				= self.active
				def handle:Vector[HH]				= self.handle map func
			}

	def dropHandle:Updater[M,Nothing]	=
			new Updater[M,Nothing] {
				def update(value:M):Vector[Node]	= self update value
				def active:Vector[Node]				= self.active
				def handle:Vector[Nothing]			= Vector.empty
			}

	/*
	def modifyHandles[HH](func:Vector[H]=>Vector[HH]):Updater[M,HH]	=
			new Updater[M,HH] {
				def update(value:M):Vector[Node]	= self update value
				def active:Vector[Node]				= self.active
				def handle:Vector[HH]				= func(self.handle)
			}
	*/

	def caching(initial:M):Updater[M,H]	=
			new Updater[M,H] {
				var old		= initial
				var active	= self.active
				var handle	= self.handle
				def update(value:M):Vector[Node]	=
						if (value != old) {
							old		= value
							val expired	= self update value
							active	= self.active
							handle	= self.handle
							expired
						}
						else {
							Vector.empty
						}
			}
}
