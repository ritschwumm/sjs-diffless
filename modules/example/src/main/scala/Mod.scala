package sjs.diffless.demo

import sjs.diffless.demo.util.*

object Mod {
	def identity[T]:Mod[T,T]				= Mod(func => func)
	def where[T](pred:T=>Boolean):Mod[T,T]	= Mod(func => item => if (pred(item)) func(item) else item)
	def each[T]:Mod[Vector[T],T]			= Mod(func => _ map func)
}

/** a sad excuse for real optics - but sufficient for our purposes */
final case class Mod[S,T](lift:(T=>T)=>(S=>S)) {
	def >=>[U](that:Mod[T,U]):Mod[S,U]	= Mod(this.lift compose that.lift)

	def set(it:T):S=>S	= lift(constant(it))
}
