package sjs.diffless

import org.scalajs.dom.raw._

import sjs.diffless.dom._

object View {
	def elementFromChildren[N<:Node,M,A,H](tag:Tag[N], children:Vector[Child[N,M,A,H]]):View[M,A,H] = {
		val attrs:Vector[Attr[N,M]]		= children collect { case x:Attr[N,M]	=> x }
		val emits:Vector[Emit[N,A]]		= children collect { case x:Emit[N,A]	=> x }
		val inners:Vector[View[M,A,H]]	= children collect { case x:View[M,A,H]	=> x }
		val handles:Vector[Handle[N,H]]	= children collect { case x:Handle[N,H]	=> x }
		element(
			tag		= tag,
			attrs	= attrs,
			emits	= emits,
			inner	= sequence(inners),
			handles	= handles
		)
	}

	// NOTE maybe converting attrs to an array would make sense here
	def element[N<:Node,M,A,H](tag:Tag[N], attrs:Vector[Attr[N,M]], emits:Vector[Emit[N,A]], inner:View[M,A,H], handles:Vector[Handle[N,H]]):View[M,A,H] =
			View { (initial, dispatch)	=>
				new Updater[M,H] {
					private val node	= tag.create()

					var selfHandle:Vector[H]	= handles map (_ create node)

					// BETTER filter out static attributes, no need to update them
					private val attrUpdates:Vector[M=>Unit]	=
							attrs map { attr =>
								attr setup (node, initial)
							}

					// attach emitters, they don't get updated
					emits foreach { _ setup (node, dispatch) }

					private val innerUpdater	= inner setup (initial, dispatch)

					// append child nodes
					innerUpdater.active foreach node.appendChild

					private var old:M	= initial

					def update(value:M):Vector[Node]	=
							if (value != old) {
								old	= value

								selfHandle	= handles map (_ create node)

								// update attributes
								attrUpdates foreach (_ apply value)

								// update content
								val innerExpired	= innerUpdater update value

								// remove vanished child nodes
								innerExpired foreach node.removeChild

								// insert nodes in order, skip over existing ones
								val todo	= innerUpdater.active
								var ptr		= node.firstChild
								todo foreach { childNode =>
									if (childNode == ptr) {
										ptr	= ptr.nextSibling
									}
									else {
										node insertBefore (childNode, ptr)
									}
								}

								handle	= selfHandle ++ innerUpdater.handle
								Vector.empty
							}
							else {
								Vector.empty
							}

					val active:Vector[Node]		= Vector(node)
					var handle:Vector[H]		= selfHandle ++ innerUpdater.handle
				}
			}

	def empty[M,A,H]:View[M,A,H]	=
			View { (initial, dispatch)	=>
				new Updater[M,H] {
					def update(value:M):Vector[Node]	= Vector.empty
					val active:Vector[Node]				= Vector.empty
					val handle:Vector[H]				= Vector.empty
				}
			}

	def vararg[M,A,H](children:View[M,A,H]*):View[M,A,H]	=
			sequence(children.toVector)

	def sequence[M,A,H](children:Vector[View[M,A,H]]):View[M,A,H]	=
				 if (children.isEmpty)		empty
			else if (children.size == 1)	children.head
			else View { (initial, dispatch) =>
				new Updater[M,H] {
					private val childUpdaters:Vector[Updater[M,H]]	= children map (_ setup (initial, dispatch))

					var active:Vector[Node]	= childUpdaters flatMap (_.active)
					var handle:Vector[H]	= childUpdaters flatMap (_.handle)

					private var old	= initial

					def update(value:M):Vector[Node]	= {
							if (value != old) {
								old	= value

								val expired	= childUpdaters flatMap (_ update value)
								active	= childUpdaters flatMap (_.active)
								handle	= childUpdaters flatMap (_.handle)
								expired
							}
							else {
								Vector.empty
							}
					}
				}
			}

	def vector[M,A,H](item:View[M,A,H]):View[Vector[M],A,H]	=
			View { (initial, dispatch) =>
				new Updater[Vector[M],H] {
					private var childUpdaters:Vector[Updater[M,H]]	= initial map { it => item setup (it, dispatch) }

					var active:Vector[Node]	= childUpdaters flatMap (_.active)
					var handle:Vector[H]	= childUpdaters flatMap (_.handle)

					private var old	= initial

					def update(value:Vector[M]):Vector[Node]	=
							if (value != old) {
								old	= value

								val oldSize	= childUpdaters.size
								val newSize	= value.size
								if (newSize > oldSize) {
									// add appeared children
									val expired	= (childUpdaters zip value) flatMap { case (u,v) => u update v }
									val fresh	= (oldSize until newSize).toVector map { idx => item setup (value(idx), dispatch) }
									childUpdaters	++= fresh
									active	= childUpdaters flatMap (_.active)
									handle	= childUpdaters flatMap (_.handle)
									expired
								}
								else if (newSize < oldSize) {
									// remove vanished children
									val (keep, remove)	= childUpdaters splitAt newSize
									childUpdaters	= keep
									val expired	= (childUpdaters zip value) flatMap { case (u,v) => u update v }
									active	= childUpdaters flatMap (_.active)
									handle	= childUpdaters flatMap (_.handle)
									expired	++ (remove flatMap (_.active))
								}
								else {
									// no size change
									val expired	= (childUpdaters zip value) flatMap { case (u,v) => u update v }
									active	= childUpdaters flatMap (_.active)
									handle	= childUpdaters flatMap (_.handle)
									expired
								}
							}
							else {
								Vector.empty
							}
				}
			}

	def keyedBy[M,A,H](key:M=>ViewKey, itemView:View[M,A,H]):View[Vector[M],A,H]	=
			keyed(itemView) adaptModel { items =>
				items map { item =>
					key(item) -> item
				}
			}

	def keyed[M,A,H](item:View[M,A,H]):View[Vector[(ViewKey,M)],A,H]	=
			View { (initial, dispatch) =>
				new Updater[Vector[(ViewKey,M)],H] {
					private var childOuts:Vector[(ViewKey,Updater[M,H])]	= initial map { case (k,v) => k -> (item setup (v, dispatch)) }

					var active:Vector[Node]	= childOuts flatMap { case (_, updater) => updater.active }
					var handle:Vector[H]	= childOuts flatMap { case (_, updater) => updater.handle }

					private var old:Vector[(ViewKey,M)]	= initial

					def update(value:Vector[(ViewKey,M)]):Vector[Node]	=
							if (value != old) {
								old	= value

								val newKeys:Set[ViewKey]	= value.map{ case (key, _) => key }.toSet

								val (keep, remove)	= childOuts partition { case (key, _) => newKeys contains key }
								var expired:Vector[Node]	= remove flatMap { case (_, updater) => updater.active }

								val keepUpdaters:Map[ViewKey,Updater[M,H]]	= keep.toMap
								childOuts	=
										value map { case (key, part) =>
											keepUpdaters get key match {
												case Some(old)	=>
													expired	++= old update part
													key -> old
												case None =>
													val ng	= item setup (part, dispatch)
													key -> ng
											}
										}

								active	= childOuts flatMap { case (_, updater) => updater.active }
								handle	= childOuts flatMap { case (_, updater) => updater.handle }

								expired
							}
							else {
								Vector.empty
							}
				}
			}

	// BETTER optimize?
	def optional[M,A,H](item:View[M,A,H]):View[Option[M],A,H]	=
			vector[M,A,H](item) adaptModel (_.toVector)

	// BETTER optimize?
	def either[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[Either[M1,M2],A,H]	=
			sequence(Vector(
				optional(item1) adaptModel (_.swap.toOption),
				optional(item2) adaptModel (_.toOption)
			))

	// BETTER optimize?
	def pair[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[(M1,M2),A,H]	=
			sequence(Vector(
				item1 adaptModel (_._1),
				item2 adaptModel (_._2)
			))

	def text[A,H]:View[String,A,H]	=
			View { (initial, dispatch)	=>
				new Updater[String,H] {
					private val node	= document createTextNode initial
					def update(value:String):Vector[Node]	= {
						node.data = value
						Vector.empty
					}
					val active:Vector[Node]	= Vector(node)
					val handle:Vector[H]	= Vector.empty
				}
			}

	/** behaves like text.contraMap(constant(value)) */
	def literal[M,A,H](value:String):View[M,A,H]	=
			View { (initial, dispatch)	=>
				new Updater[M,H] {
					private val node	= document createTextNode value
					def update(value:M):Vector[Node]	= Vector.empty
					val active:Vector[Node]				= Vector(node)
					val handle:Vector[H]				= Vector.empty
				}
			}
}

// NOTE could return null/None for static nodes so we don't have to update them
final case class View[-M,+A,+H](setup:(M, A=>EventFlow) => Updater[M,H]) extends Child[Any,M,A,H] { self =>
	def apply[MM](func:MM=>M):View[MM,A,H]	= adaptModel(func)

	def adapt[MM,AA,HH](model:MM=>M, action:A=>AA, handle:H=>HH):View[MM,AA,HH]	=
			View { (initial, dispatch) =>
				setup(model(initial), action andThen dispatch) adapt (model, handle)
			}

	def adaptModelAndAction[MM,AA](model:MM=>M, action:A=>AA):View[MM,AA,H]	=
			adapt(model, action, identity)

	@deprecated("use adaptModel", "0.3.0")
	def contraMapModel[MM](func:MM=>M):View[MM,A,H]	= adaptModel(func)

	// TODO auto-cache?
	def adaptModel[MM](func:MM=>M):View[MM,A,H]	=
			View { (initial, dispatch) =>
				setup(func(initial), dispatch) adaptModel func
			}

	def contextual[C,AA,HH](actionFunc:(C,A)=>AA, handleFunc:(C,H)=>HH):View[(C,M),AA,HH]	=
			View { (initial, dispatch) =>
				new Updater[(C,M),HH] {
					private var context			= initial._1

					private val innerDispatch	= (action:A) => dispatch(actionFunc(context, action))
					private val innerUpdater	= self setup (initial._2, innerDispatch)

					def update(value:(C,M)):Vector[Node]	= {
						context	= value._1
						innerUpdater update value._2
					}
					def active:Vector[Node]	= innerUpdater.active
					def handle:Vector[HH]	= innerUpdater.handle map { handle => handleFunc(context, handle) }
				}
			}

	/*
	def contextAction[MM<:M,AA](func:(MM,A)=>AA):View[MM,AA,H]	=
			View { (initial, dispatch) =>
				new Updater[MM,H] {
					private var model			= initial
					private val innerDispatch	= (action:A) => dispatch(func(model, action))
					private val innerUpdater	= self setup (initial, innerDispatch)

					def update(value:MM):Vector[Node]	= {
						model	= value
						innerUpdater update model
					}
					def active:Vector[Node]	= innerUpdater.active
					def handle:Vector[H]	= innerUpdater.handle
				}
			}

	def contextHandle[MM<:M,HH](func:(MM,H)=>HH):View[MM,A,HH]	=
			View { (initial, dispatch) =>
				self setup (initial, dispatch) contextHandle (initial, func)
			}
	*/

	@deprecated("use adaptActionAndHandle", "0.3.0")
	def mapOutput[AA,HH](action:A=>AA, handle:H=>HH):View[M,AA,HH]	= adaptActionAndHandle(action, handle)

	def adaptActionAndHandle[AA,HH](action:A=>AA, handle:H=>HH):View[M,AA,HH]	=
			View { (initial, dispatch) =>
				setup(initial, action andThen dispatch) adaptHandle handle
			}

	@deprecated("use adaptAction", "0.3.0")
	def mapAction[AA](func:A=>AA):View[M,AA,H]	= adaptAction(func)

	def adaptAction[AA](func:A=>AA):View[M,AA,H]	=
			View { (initial, dispatch) =>
				self setup (initial, func andThen dispatch)
			}

	@deprecated("use adaptHandle", "0.3.0")
	def mapHandle[HH](func:H=>HH):View[M,A,HH]	= adaptHandle(func)

	def adaptHandle[HH](func:H=>HH):View[M,A,HH]	=
			View { (initial, dispatch) =>
				self setup (initial, dispatch) adaptHandle func
			}

	def dropHandle:View[M,A,Nothing]	=
			View { (initial, dispatch) =>
				self.setup(initial, dispatch).dropHandle
			}

	/*
	def modifyHandles[HH](func:Vector[H]=>Vector[HH]):View[M,A,HH]	=
			View { (initial, dispatch) =>
				self setup (initial, dispatch) modifyHandles func
			}
	*/

	def caching:View[M,A,H]	=
			View { (initial, dispatch) =>
				setup(initial, dispatch).caching(initial)
			}

	def attach(node:Node, initial:M, dispatch:A=>EventFlow):(Vector[H], M=>Vector[H])	= {
		while (node.firstChild != null) {
			node removeChild node.firstChild
		}

		val innerUpdater	= setup(initial, dispatch)

		innerUpdater.active foreach node.appendChild

		var old	= initial

		val update:M=>Vector[H]	=
				value	=> {
					if (value != old) {
						old	= value

						val innerExpired	= innerUpdater update value

						innerExpired foreach node.removeChild

						val todo	= innerUpdater.active
						var ptr		= node.firstChild
						todo foreach { childNode =>
							if (childNode == ptr) {
								ptr	= ptr.nextSibling
							}
							else {
								node insertBefore (childNode, ptr)
							}
						}
					}
					innerUpdater.handle
				}

		innerUpdater.handle -> update
	}
}
