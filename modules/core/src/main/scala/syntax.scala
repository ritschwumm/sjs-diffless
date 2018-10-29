package sjs.diffless

import scala.scalajs.js
import org.scalajs.dom.raw._

object syntax extends syntax

trait syntax {
	implicit class TagExt[N<:Node](peer:Tag[N]) {
		def apply[M,A,H](children:Child[N,M,A,H]*):View[M,A,H]	=
				View elementFromChildren (peer, children.toVector)
	}

	implicit class AttrKeyAttrExt[K](peer:K) {
		def ~=[N,M,MM](func:M=>MM)(implicit ev:AttrSetter[K,N,MM]):Attr[N,M]	= Attr dynamic ev.proc adaptModel func
		def :=[N,M,MM](value:MM)(implicit ev:AttrSetter[K,N,MM]):Attr[N,M]		= Attr static  (ev.proc, value)

		/*
		sadly, this leads to ambiguous implicits
		def apply[N,M,A](implicit ev:AttrSetter[K,N,M]):Attr[N,M]	= Attr dynamic ev.proc
		*/
	}

	implicit class AttrKeyEmitExt[K](peer:K) {
		def |=[N,A,E<:Event](handler:(N,E)=>A)(implicit ev:AttrSetter[K,N,js.Function1[E,_]]):Emit[N,A]	= {
			val attach:(N,E=>Unit)=>Unit	= (n,ae) => ev.proc(n, ae)
			Emit action (attach, handler)
		}
	}
}
