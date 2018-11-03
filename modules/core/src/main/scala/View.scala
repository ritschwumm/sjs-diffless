package sjs.diffless

import org.scalajs.dom.raw._

import sjs.diffless.dom._

object View {
	def elementFromChildren[N<:Node,M,A,H](tag:Tag[N], children:Vector[Child[N,M,A,H]]):View[M,A,H] = {
		val attributes:Vector[Attribute[N,M]]	= children collect { case x:Attribute[N,M]	=> x }
		val emits:Vector[Emit[N,A]]				= children collect { case x:Emit[N,A]		=> x }
		val inners:Vector[View[M,A,H]]			= children collect { case x:View[M,A,H]		=> x }
		val grafts:Vector[Graft[N,H]]			= children collect { case x:Graft[N,H]		=> x }
		element(
			tag			= tag,
			attributes	= attributes,
			emits		= emits,
			inner		= sequence(inners),
			grafts		= grafts
		)
	}

	// NOTE maybe converting attrs to an array would make sense here
	def element[N<:Node,M,A,H](tag:Tag[N], attributes:Vector[Attribute[N,M]], emits:Vector[Emit[N,A]], inner:View[M,A,H], grafts:Vector[Graft[N,H]]):View[M,A,H] =
		{
			val requiresUpdates	= inner.requiresUpdates || (attributes exists (_.requiresUpdates))
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						private val node	= tag.create()

						val selfHandles:Vector[H]	= grafts map (_ create node)

						// BETTER filter out static attributes, no need to update them
						private val attributeUpdates:Vector[M=>Unit]	=
								attributes flatMap { attr =>
									val base	= attr setup (node, initial)
									// NOTE this check is important, calling the updater on a static attribute will fail at runtime
									if (attr.requiresUpdates)	Some(base)
									else						None
								}

						// attach emitters, they don't get updated
						emits foreach { _ setup (node, dispatch) }

						private val innerUpdater	= inner setup (initial, dispatch)

						// append child nodes
						innerUpdater.active foreach node.appendChild

						private var old:M	= initial

						def update(value:M):Vector[Node]	=
							{
								if (requiresUpdates) {
									if (value != old) {
										old	= value

										// update attributes
										attributeUpdates foreach (_ apply value)

										if (inner.requiresUpdates) {
											// update content
											val innerExpired	= innerUpdater update value

											if (inner.instableNodes) {
												// remove vanished child nodes
												innerExpired foreach node.removeChild

												// insert nodes in order, skip over existing ones
												val todo	= innerUpdater.active
												var ptr		= node.firstChild
												todo foreach { childNode =>
													if (childNode eq ptr) {
														ptr	= ptr.nextSibling
													}
													else {
														node insertBefore (childNode, ptr)
													}
												}
											}
										}

										// TODO opt calculating inner handles is not necessary when !inner.requiresUpdate
										handles	= selfHandles ++ innerUpdater.handles
									}
								}

								// we always do the same node, to nothing to remove here
								Vector.empty
							}

						val active:Vector[Node]		= Vector(node)
						var handles:Vector[H]		= selfHandles ++ innerUpdater.handles
					}
				}
			)
		}

	def empty[M,A,H]:View[M,A,H]	=
			View(
				requiresUpdates	= false,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						def update(value:M):Vector[Node]	= Vector.empty
						val active:Vector[Node]				= Vector.empty
						val handles:Vector[H]				= Vector.empty
					}
				}
			)

	def vararg[M,A,H](children:View[M,A,H]*):View[M,A,H]	=
			sequence(children.toVector)

	def sequence[M,A,H](children:Vector[View[M,A,H]]):View[M,A,H]	=
				 if (children.isEmpty)		empty
			else if (children.size == 1)	children.head
			else View(
				requiresUpdates	= children exists (_.requiresUpdates),
				instableNodes	= children exists (_.instableNodes),
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						private val childUpdaters:Vector[Updater[M,H]]	= children map (_ setup (initial, dispatch))

						var active:Vector[Node]	= childUpdaters flatMap (_.active)
						var handles:Vector[H]	= childUpdaters flatMap (_.handles)

						private var old	= initial

						// TODO opt use optimization data
						def update(value:M):Vector[Node]	=
								if (value != old) {
									old	= value

									val expired	= childUpdaters flatMap (_ update value)
									active	= childUpdaters flatMap (_.active)
									handles	= childUpdaters flatMap (_.handles)
									expired
								}
								else {
									Vector.empty
								}
					}
				}
			)

	def vector[M,A,H](item:View[M,A,H]):View[Vector[M],A,H]	=
			View(
				requiresUpdates	= true,
				instableNodes	= true,
				setup	= (initial, dispatch) => {
					new Updater[Vector[M],H] {
						private var childUpdaters:Vector[Updater[M,H]]	= initial map { it => item setup (it, dispatch) }

						var active:Vector[Node]	= childUpdaters flatMap (_.active)
						var handles:Vector[H]	= childUpdaters flatMap (_.handles)

						private var old	= initial

						// TODO opt use optimization data
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
										handles	= childUpdaters flatMap (_.handles)
										expired
									}
									else if (newSize < oldSize) {
										// remove vanished children
										val (keep, remove)	= childUpdaters splitAt newSize
										childUpdaters	= keep
										val expired	= (childUpdaters zip value) flatMap { case (u,v) => u update v }
										active	= childUpdaters flatMap (_.active)
										handles	= childUpdaters flatMap (_.handles)
										expired	++ (remove flatMap (_.active))
									}
									else {
										// no size change
										val expired	= (childUpdaters zip value) flatMap { case (u,v) => u update v }
										active	= childUpdaters flatMap (_.active)
										handles	= childUpdaters flatMap (_.handles)
										expired
									}
								}
								else {
									Vector.empty
								}
					}
				}
			)

	def keyedBy[M,A,H](key:M=>ViewKey, itemView:View[M,A,H]):View[Vector[M],A,H]	=
			keyed(itemView) adaptModel { items =>
				items map { item =>
					key(item) -> item
				}
			}

	def keyed[M,A,H](item:View[M,A,H]):View[Vector[(ViewKey,M)],A,H]	=
			View(
				requiresUpdates	= true,
				instableNodes	= true,
				setup	= (initial, dispatch) => {
					new Updater[Vector[(ViewKey,M)],H] {
						private var childOuts:Vector[(ViewKey,Updater[M,H])]	= initial map { case (k,v) => k -> (item setup (v, dispatch)) }

						var active:Vector[Node]	= childOuts flatMap { case (_, updater) => updater.active }
						var handles:Vector[H]	= childOuts flatMap { case (_, updater) => updater.handles }

						private var old:Vector[(ViewKey,M)]	= initial

						// TODO opt use optimization data
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
									handles	= childOuts flatMap { case (_, updater) => updater.handles }

									expired
								}
								else {
									Vector.empty
								}
					}
				}
			)

	// BETTER optimize?
	def optional[M,A,H](item:View[M,A,H]):View[Option[M],A,H]	=
			vector[M,A,H](item) adaptModel (_.toVector)

	// BETTER optimize?
	def either[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[Either[M1,M2],A,H]	=
			vararg(
				optional(item1) adaptModel (_.swap.toOption),
				optional(item2) adaptModel (_.toOption)
			)

	// BETTER optimize?
	def pair[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[(M1,M2),A,H]	=
			vararg(
				item1 adaptModel (_._1),
				item2 adaptModel (_._2)
			)

	def text[A,H]:View[String,A,H]	=
			View(
				requiresUpdates	= true,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					new Updater[String,H] {
						private val node	= document createTextNode initial
						def update(value:String):Vector[Node]	= {
							node.data = value
							Vector.empty
						}
						val active:Vector[Node]	= Vector(node)
						val handles:Vector[H]	= Vector.empty
					}
				}
			)

	/** behaves like text.contraMap(constant(value)) */
	def literal[M,A,H](value:String):View[M,A,H]	=
			View(
				requiresUpdates	= false,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						private val node	= document createTextNode value
						def update(value:M):Vector[Node]	= Vector.empty
						val active:Vector[Node]				= Vector(node)
						val handles:Vector[H]				= Vector.empty
					}
				}
			)
}

final case class View[-M,+A,+H](
	// !requiresUpdates implies !instableNodes
	// instableNodes implies requiresUpdates
	requiresUpdates:Boolean,
	instableNodes:Boolean,
	setup:(M, A=>EventFlow) => Updater[M,H]
)
extends Child[Any,M,A,H] { self =>
	def apply[MM](func:MM=>M):View[MM,A,H]	= adaptModel(func)

	def adapt[MM,AA,HH](model:MM=>M, action:A=>AA, handle:H=>HH):View[MM,AA,HH]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					setup(model(initial), action andThen dispatch) adapt (model, handle)
				}
			)

	def adaptModelAndAction[MM,AA](model:MM=>M, action:A=>AA):View[MM,AA,H]	=
			adapt(model, action, identity)

	// TODO auto-cache?
	def adaptModel[MM](func:MM=>M):View[MM,A,H]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					setup(func(initial), dispatch) adaptModel func
				}
			)

	def contextual[C,AA,HH](actionFunc:(C,A)=>AA, handleFunc:(C,H)=>HH):View[(C,M),AA,HH]	=
			View(
				requiresUpdates	= true,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					new Updater[(C,M),HH] {
						private var context			= initial._1

						private val innerDispatch	= (action:A) => dispatch(actionFunc(context, action))
						private val innerUpdater	= self setup (initial._2, innerDispatch)

						def update(value:(C,M)):Vector[Node]	= {
							context	= value._1
							innerUpdater update value._2
						}
						def active:Vector[Node]	= innerUpdater.active
						def handles:Vector[HH]	= innerUpdater.handles map { handle => handleFunc(context, handle) }
					}
				}
			)

	/*
	def contextualAction[MM<:M,AA](func:(MM,A)=>AA):View[MM,AA,H]	=
			View(
				requiresUpdates	= true,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
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
			)

	def contextualHandle[MM<:M,HH](func:(MM,H)=>HH):View[MM,A,HH]	=
			View(
				requiresUpdates	= true,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					self setup (initial, dispatch) contextHandle (initial, func)
				}
			)
	*/

	def adaptActionAndHandle[AA,HH](action:A=>AA, handle:H=>HH):View[M,AA,HH]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					setup(initial, action andThen dispatch) adaptHandle handle
				}
			)

	def adaptAction[AA](func:A=>AA):View[M,AA,H]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					self setup (initial, func andThen dispatch)
				}
			)

	def adaptHandle[HH](func:H=>HH):View[M,A,HH]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					self setup (initial, dispatch) adaptHandle func
				}
			)

	def dropHandle:View[M,A,Nothing]	=
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					self.setup(initial, dispatch).dropHandle
				}
			)

	/*
	def modifyHandles[HH](func:Vector[H]=>Vector[HH]):View[M,A,HH]	=
			View(
				requiresUpdates	= requiresUpdates,
				setup	= (initial, dispatch) => {
					self setup (initial, dispatch) modifyHandles func
				}
			)
	*/

	def caching:View[M,A,H]	=
			if (requiresUpdates) {
				View(
					requiresUpdates	= requiresUpdates,
					instableNodes	= instableNodes,
					setup	= (initial, dispatch) => {
						setup(initial, dispatch).caching(initial)
					}
				)
			}
			else this

	def static(value:M):View[Any,A,H]	=
			View(
				requiresUpdates	= false,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					setup(value, dispatch).static
				}
			)

	def attach(node:Node, initial:M, dispatch:A=>EventFlow):(Vector[H], M=>Vector[H])	= {
		while (node.firstChild != null) {
			node removeChild node.firstChild
		}

		val innerUpdater	= setup(initial, dispatch)

		innerUpdater.active foreach node.appendChild

		var old	= initial

		val update:M=>Vector[H]	=
				value	=> {
					if (requiresUpdates) {
						if (value != old) {
							old	= value

							// update content
							val innerExpired	= innerUpdater update value

							if (instableNodes) {
								// remove vanished child nodes
								innerExpired foreach node.removeChild

								// insert nodes in order, skip over existing ones
								val todo	= innerUpdater.active
								var ptr		= node.firstChild
								todo foreach { childNode =>
									if (childNode eq ptr) {
										ptr	= ptr.nextSibling
									}
									else {
										node insertBefore (childNode, ptr)
									}
								}
							}
						}
					}
					innerUpdater.handles
				}

		innerUpdater.handles -> update
	}
}
