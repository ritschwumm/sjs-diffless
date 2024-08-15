package sjs.diffless

import org.scalajs.dom.*

object View {
	private type Setup[M,A,H]	= (M, A=>EventFlow) => Updater[M,H]

	/** this is used to support the tag dsl */
	def elementFromChildren[N<:Node,M,A,H](tag:Tag[N], children:Vector[Child[N,M,A,H]]):View[M,A,H] = {
		val attributes:Vector[Attribute[N,M]]	= children.collect { case x:Attribute[N,M]	=> x }
		val emits:Vector[Emit[N,A]]				= children.collect { case x:Emit[N,A]		=> x }
		val inners:Vector[View[M,A,H]]			= children.collect { case x:View[M,A,H]		=> x }
		val attachments:Vector[Attachment[N,H]]	= children.collect { case x:Attachment[N,H]		=> x }
		element(
			tag			= tag,
			attributes	= attributes,
			emits		= emits,
			inner		= sequence(inners),
			attachments	= attachments
		)
	}

	/**
	 * build a View for a single html tag
	 *
	 * takes
	 * - which tag to build
	 * - the tag's attributes
	 * - which events to emit
	 * - the child elements of the tag
	 * - how handles are to be attached to the tag
	 */
	def element[N<:Node,M,A,H](
		tag:Tag[N],
		attributes:Vector[Attribute[N,M]],
		emits:Vector[Emit[N,A]],
		inner:View[M,A,H],
		attachments:Vector[Attachment[N,H]]
	):View[M,A,H] =
		{
			// NOTE maybe converting attrs to an array would make sense here

			val requiresUpdates	= inner.requiresUpdates || attributes.exists(_.requiresUpdates)
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= false,
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						private val node	= tag.create()

						val selfHandles:Vector[H]	= attachments.map(_.create(node))

						private val attributeUpdates:Vector[M=>Unit]	=
							attributes.flatMap { attr =>
								val base	= attr.setup(node, initial)
								// NOTE this check is important, calling the updater on a static attribute will fail at runtime
								if (attr.requiresUpdates)	Some(base)
								else						None
							}

						// attach emitters, they don't get updated
						emits.foreach { _.setup(node, dispatch) }

						private val innerUpdater	= inner.setup(initial, dispatch)

						// append child nodes
						innerUpdater.active.foreach(node.appendChild)

						private var old:M	= initial

						def update(value:M):Vector[Node]	=
							{
								if (requiresUpdates) {
									if (value != old) {
										old	= value

										// update attributes
										attributeUpdates.foreach(_.apply(value))

										if (inner.requiresUpdates) {
											// update content
											val innerExpired	= innerUpdater.update(value)

											if (inner.instableNodes) {
												// remove vanished child nodes
												innerExpired.foreach(node.removeChild)

												// insert nodes in order, skip over existing ones
												val todo	= innerUpdater.active
												var ptr		= node.firstChild
												todo.foreach { childNode =>
													if (childNode eq ptr) {
														ptr	= ptr.nextSibling
													}
													else {
														node.insertBefore(childNode, ptr)
													}
												}
											}

											// NOTE selfHandles is constant here, but innerUpdater.handles might change even
											// if inner.instableNodes is false because handles are accumulated transitively
											handles	= selfHandles ++ innerUpdater.handles
										}
									}
								}

								// we always do the same node, so nothing to remove here
								Vector.empty
							}

						val active:Vector[Node]		= Vector(node)
						var handles:Vector[H]		= selfHandles ++ innerUpdater.handles
					}
				}
			)
		}

	/** a view which never puts any nodes into its parent */
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

	/** convenience function for #sequence */
	def vararg[M,A,H](children:View[M,A,H]*):View[M,A,H]	=
		sequence(children.toVector)

	/** concatenates a fixed sequence of views into a larger one */
	def sequence[M,A,H](children:Vector[View[M,A,H]]):View[M,A,H]	=
		if		(children.isEmpty)		empty
		else if	(children.size == 1)	children.head
		else {
			val requiresUpdates	= children.exists(_.requiresUpdates)
			val instableNodes	= children.exists(_.instableNodes)
			View(
				requiresUpdates	= requiresUpdates,
				instableNodes	= instableNodes,
				setup	= (initial, dispatch) => {
					new Updater[M,H] {
						private val childUpdaters:Vector[Updater[M,H]]	= children.map(_.setup(initial, dispatch))

						var active:Vector[Node]	= childUpdaters.flatMap(_.active)
						var handles:Vector[H]	= childUpdaters.flatMap(_.handles)

						private var old	= initial

						def update(value:M):Vector[Node]	=
								if (requiresUpdates && value != old) {
									old	= value

									val expired	= childUpdaters.flatMap(_.update(value))

									if (instableNodes) {
										active	= childUpdaters.flatMap(_.active)
									}

									handles	= childUpdaters.flatMap(_.handles)
									expired
								}
								else {
									Vector.empty
								}
					}
				}
			)
		}

	/**
	 * lifts a view for a single item into a view of a vector of such items
	 * for stateful views like input tags, use #keyed instead
	 */
	def vector[M,A,H](item:View[M,A,H]):View[Vector[M],A,H]	=
		View(
			requiresUpdates	= true,
			instableNodes	= true,
			setup	= (initial, dispatch) => {
				new Updater[Vector[M],H] {
					private var childUpdaters:Vector[Updater[M,H]]	= initial.map { it => item.setup(it, dispatch) }

					var active:Vector[Node]	= childUpdaters.flatMap(_.active)
					var handles:Vector[H]	= childUpdaters.flatMap(_.handles)

					private var old	= initial

					// TODO opt use optimization data
					def update(value:Vector[M]):Vector[Node]	=
							if (value != old) {
								old	= value

								val oldSize	= childUpdaters.size
								val newSize	= value.size
								if (newSize > oldSize) {
									// add appeared children
									val expired	= childUpdaters.zip(value).flatMap { (u,v) => u.update(v) }
									val fresh	= oldSize.until(newSize).toVector.map { idx => item.setup(value(idx), dispatch) }
									childUpdaters	++= fresh
									active	= childUpdaters.flatMap(_.active)
									handles	= childUpdaters.flatMap(_.handles)
									expired
								}
								else if (newSize < oldSize) {
									// remove vanished children
									val (keep, remove)	= childUpdaters.splitAt(newSize)
									childUpdaters	= keep
									val expired	= childUpdaters.zip(value).flatMap { (u,v) => u.update(v) }
									active	= childUpdaters.flatMap(_.active)
									handles	= childUpdaters.flatMap(_.handles)
									expired	++ remove.flatMap(_.active)
								}
								else {
									// no size change
									val expired	= childUpdaters.zip(value).flatMap { (u,v) => u.update(v) }
									active	= childUpdaters.flatMap(_.active)
									handles	= childUpdaters.flatMap(_.handles)
									expired
								}
							}
							else {
								Vector.empty
							}
				}
			}
		)

	/** convenience function for keyed where the key can be derived from the item */
	def keyedBy[M,A,H](key:M=>ViewKey, itemView:View[M,A,H]):View[Vector[M],A,H]	=
		keyed(itemView).adaptModel { items =>
			items.map { item =>
				key(item) -> item
			}
		}

	/**
	 * lifts a view for a single item into a view of a vector of those items
	 * individual items are identified by a #ViewKey which allows
	 * keeping the state of a stateful view like e.g. an input tag
	 */
	def keyed[M,A,H](item:View[M,A,H]):View[Vector[(ViewKey,M)],A,H]	=
		View(
			requiresUpdates	= true,
			instableNodes	= true,
			setup	= (initial, dispatch) => {
				new Updater[Vector[(ViewKey,M)],H] {
					private var childOuts:Vector[(ViewKey,Updater[M,H])]	= initial.map { (k,v) => k -> (item.setup(v, dispatch)) }

					var active:Vector[Node]	= childOuts.flatMap { (_, updater) => updater.active }
					var handles:Vector[H]	= childOuts.flatMap { (_, updater) => updater.handles }

					private var old:Vector[(ViewKey,M)]	= initial

					// TODO opt use optimization data
					def update(value:Vector[(ViewKey,M)]):Vector[Node]	=
							if (value != old) {
								old	= value

								val newKeys:Set[ViewKey]	= value.map{ (key, _) => key }.toSet

								val (keep, remove)	= childOuts.partition { (key, _) => newKeys.contains(key) }
								var expired:Vector[Node]	= remove.flatMap { (_, updater) => updater.active }

								val keepUpdaters:Map[ViewKey,Updater[M,H]]	= keep.toMap
								childOuts	=
										value.map { (key, part) =>
											keepUpdaters.get(key) match {
												case Some(old)	=>
													expired	++= old.update(part)
													key -> old
												case None =>
													val ng	= item.setup(part, dispatch)
													key -> ng
											}
										}

								active	= childOuts.flatMap { (_, updater) => updater.active }
								handles	= childOuts.flatMap { (_, updater) => updater.handles }

								expired
							}
							else {
								Vector.empty
							}
				}
			}
		)

	/** lifts a view for an item to a view which optionally display this item */
	def optional[M,A,H](item:View[M,A,H]):View[Option[M],A,H]	=
		// BETTER optimize?
		vector[M,A,H](item).adaptModel(_.toVector)

	/** lifts two views into a view which displays either one or the other */
	def either[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[Either[M1,M2],A,H]	=
		// BETTER optimize?
		vararg(
			vector(item1).adaptModel(_.swap.toOption.toVector),
			vector(item2).adaptModel(_.toOption.toVector)
		)

	/** lifts two views into a view which displays both views, one after another */
	def pair[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[(M1,M2),A,H]	=
		// BETTER optimize?
		vararg(
			item1.adaptModel(_._1),
			item2.adaptModel(_._2)
		)

	/** a primitive view displaying some dynamically changing text */
	def text[A,H]:View[String,A,H]	=
		View(
			requiresUpdates	= true,
			instableNodes	= false,
			setup	= (initial, dispatch) => {
				new Updater[String,H] {
					private val node	= document.createTextNode(initial)
					def update(value:String):Vector[Node]	= {
						node.data = value
						Vector.empty
					}
					val active:Vector[Node]	= Vector(node)
					val handles:Vector[H]	= Vector.empty
				}
			}
		)

	/**
	 * a primitive view displaying some static text
	 * behaves like text.contraMap(constant(value))
	 */
	def literal[M,A,H](value:String):View[M,A,H]	=
		View(
			requiresUpdates	= false,
			instableNodes	= false,
			setup	= (initial, dispatch) => {
				new Updater[M,H] {
					private val node	= document.createTextNode(value)
					def update(value:M):Vector[Node]	= Vector.empty
					val active:Vector[Node]				= Vector(node)
					val handles:Vector[H]				= Vector.empty
				}
			}
		)
}

/** defines a list of DOM nodes by how to put them into a parent element */
final case class View[-M,+A,+H](
	/*
	!requiresUpdates	implies	!instableNodes
	instableNodes		implies	requiresUpdates
	*/

	// when this is false, there's no need to call update at all because the view is completely static
	requiresUpdates:Boolean,

	// when this is false, the View's Updater always has the same active Nodes - but no necessarily the same handles, because those are gathered transitively
	instableNodes:Boolean,

	setup:(M, A=>EventFlow) => Updater[M,H]
)
extends Child[Any,M,A,H] { self =>
	def apply[MM](func:MM=>M):View[MM,A,H]	= adaptModel(func)

	def adapt[MM,AA,HH](model:MM=>M, action:A=>AA, handle:H=>HH):View[MM,AA,HH]	=
		withSetup[MM,AA,HH] { (initial, dispatch) =>
			setup(model(initial), action `andThen` dispatch).adapt(model, handle)
		}

	// TODO auto-cache?
	//adapt(identity, identity, identity)
	def adaptModel[MM](model:MM=>M):View[MM,A,H]	=
		withSetup[MM,A,H] { (initial, dispatch) =>
			setup(model(initial), dispatch).adaptModel(model)
		}

	def adaptAction[AA](func:A=>AA):View[M,AA,H]	=
		//adapt(identity, action, identity)
		withSetup[M,AA,H] { (initial, dispatch) =>
			setup(initial, func `andThen` dispatch)
		}

	def adaptHandle[HH](handle:H=>HH):View[M,A,HH]	=
		//adapt(identity, identity, handle)
		withSetup[M,A,HH] { (initial, dispatch) =>
			setup(initial, dispatch).adaptHandle(handle)
		}

	def adaptModelAndAction[MM,AA](model:MM=>M, action:A=>AA):View[MM,AA,H]	=
		//adapt(model, action, identity)
		withSetup[MM,AA,H] { (initial, dispatch) =>
			setup(model(initial), action `andThen` dispatch).adaptModel(model)
		}

	def adaptActionAndHandle[AA,HH](action:A=>AA, handle:H=>HH):View[M,AA,HH]	=
		//adapt(identity, action, handle)
		withSetup[M,AA,HH] { (initial, dispatch) =>
			setup(initial, action `andThen` dispatch).adaptHandle(handle)
		}

	/** removes all attached handles */
	def dropHandle:View[M,A,Nothing]	=
		withSetup[M,A,Nothing] { (initial, dispatch) =>
			setup(initial, dispatch).dropHandle
		}

	/*
	def modifyHandles[HH](func:Vector[H]=>Vector[HH]):View[M,A,HH]	=
		withSetup[M,A,HH] { (initial, dispatch) =>
			self.setup(initial, dispatch).modifyHandles(func)
		}
	*/

	/**
	 * the counterpart to View.keyed acting on actions and handles instead of the model
	 * - a context part which can be used to modify actions and handles
	 * - a model part which is passed on to the original view
	 */
	def contextual[C,AA,HH](actionFunc:(C,A)=>AA, handleFunc:(C,H)=>HH):View[(C,M),AA,HH]	=
		View(
			requiresUpdates	= true,
			instableNodes	= instableNodes,
			setup	= (initial, dispatch) => {
				new Updater[(C,M),HH] {
					private var context			= initial._1

					private val innerDispatch	= (action:A) => dispatch(actionFunc(context, action))
					private val innerUpdater	= self.setup(initial._2, innerDispatch)

					def update(value:(C,M)):Vector[Node]	= {
						context	= value._1
						innerUpdater.update(value._2)
					}
					def active:Vector[Node]	= innerUpdater.active
					def handles:Vector[HH]	= innerUpdater.handles.map { handle => handleFunc(context, handle) }
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
					private val innerUpdater	= self.setup(initial, innerDispatch)

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

	/** adds model caching unless this view is static anyway */
	def caching:View[M,A,H]	=
		if (requiresUpdates) {
			withSetup[M,A,H] {
				(initial, dispatch) => {
					setup(initial, dispatch).caching(initial)
				}
			}
		}
		else this

	/** ignores its model completely */
	def static(value:M):View[Any,A,H]	=
		View(
			requiresUpdates	= false,
			instableNodes	= false,
			setup	= (initial, dispatch) => {
				setup(value, dispatch).static
			}
		)

	/** ignores after-init changes when the alive-predicate is false */
	def lively[MM](alive:MM=>Boolean, func:MM=>M):View[MM,A,H]	=
		View(
			requiresUpdates	= requiresUpdates,
			instableNodes	= instableNodes,
			setup	= (initial, dispatch) => {
				setup(func(initial), dispatch).lively(alive, func)
			}
		)

	//------------------------------------------------------------------------------

	/**
	 * installs this view into some parent node and takes over management of all child nodes of it.
	 * requires an initial value for the model and a way to dispatch with events.
	 * returns the attached handles of the view after setup and an update function
	 * to be called on model changes which returns a new set of attached handles
	 * when it's called.
	 */
	def attach(node:Node, initial:M, dispatch:A=>EventFlow):(Vector[H], M=>Vector[H])	= {
		while (node.firstChild != null) {
			node.removeChild(node.firstChild)
		}

		val innerUpdater	= setup(initial, dispatch)

		innerUpdater.active.foreach(node.appendChild)

		var old	= initial

		val update:M=>Vector[H]	=
			value	=> {
				if (requiresUpdates) {
					if (value != old) {
						old	= value

						// update content
						val innerExpired	= innerUpdater.update(value)

						if (instableNodes) {
							// remove vanished child nodes
							innerExpired.foreach(node.removeChild)

							// insert nodes in order, skip over existing ones
							val todo	= innerUpdater.active
							var ptr		= node.firstChild
							todo.foreach { childNode =>
								if (childNode eq ptr) {
									ptr	= ptr.nextSibling
								}
								else {
									node.insertBefore(childNode, ptr)
								}
							}
						}
					}
				}
				innerUpdater.handles
			}

		innerUpdater.handles -> update
	}

	@inline private def withSetup[MM, AA, HH](setup:View.Setup[MM,AA,HH]):View[MM,AA,HH]	=
		View(
			requiresUpdates	= requiresUpdates,
			instableNodes	= instableNodes,
			setup			= setup
		)
}
