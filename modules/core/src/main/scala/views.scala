package sjs.diffless

object views extends views

/** syntax provider for views */
trait views {
	def empty[M,A,H]:View[M,A,H]															= View.empty
	def vararg[M,A,H](children:View[M,A,H]*):View[M,A,H]									= View.vararg	(children:_*)
	def sequence[M,A,H](children:Vector[View[M,A,H]]):View[M,A,H]							= View.sequence	(children)

	def vector[M,A,H](item:View[M,A,H]):View[Vector[M],A,H]									= View.vector	(item)
	def keyedBy[M,A,H](key:M=>ViewKey, item:View[M,A,H]):View[Vector[M],A,H]				= View.keyedBy	(key, item)
	def keyed[M,A,H](item:View[M,A,H]):View[Vector[(ViewKey,M)],A,H]						= View.keyed	(item)
	def optional[M,A,H](item:View[M,A,H]):View[Option[M],A,H]								= View.optional	(item)

	def either[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[Either[M1,M2],A,H]	= View.either	(item1, item2)
	def pair[M1,M2,A,H](item1:View[M1,A,H], item2:View[M2,A,H]):View[(M1,M2),A,H]			= View.pair		(item1, item2)

	def text[A,H]:View[String,A,H]															= View.text
	def literal[M,A,H](value:String):View[M,A,H]											= View.literal	(value)
}
