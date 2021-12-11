package sjs.diffless

import scala.scalajs.js
import org.scalajs.dom._

/** syntax extensions to have nice and clean DSL */
object syntax {
	extension[N<:Node](peer:Tag[N]) {
		/**
		 * allows using a Tag as a factory of the corresponding html element when provided
		 * with attribute values, child elements, event emitters and attachments.
		 */
		def apply[M,A,H](children:Child[N,M,A,H]*):View[M,A,H]	=
			View.elementFromChildren(peer, children.toVector)
	}

	extension[K](peer:K) {
		/** make the value of this attribute of the surrounding html element dynamically change based on the model */
		def ~=[N,M,MM](func:M=>MM)(using ev:AttributeAccess[K,N,MM]):Attribute[N,M]	= Attribute	.dynamic	(ev.setter)	adaptModel func
		/** set the value of this attribute of the surrounding html element to a fixed value */
		def :=[N,M,MM](value:MM)(using ev:AttributeAccess[K,N,MM]):Attribute[N,M]	= Attribute	.static  (ev.setter, value)

		/*
		sadly, this leads to ambiguous implicits
		def apply[N,M,A](using ev:AttributeAccess[K,N,M]):Attribute[N,M]	= Attribute dynamic ev.proc
		*/
	}

	extension[K](peer:K) {
		/** attach an event listener to the surrounding html element */
		def |=[N,A,E<:Event](handler:(N,E)=>A)(using ev:AttributeAccess[K,N,js.Function1[E,?]]):Emit[N,A]	= {
			val attach:(N,E=>Unit)=>Unit	= (n,ae) => ev.setter(n, ae)
			Emit.action(attach, handler)
		}
	}
}
