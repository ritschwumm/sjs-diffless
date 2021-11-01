package sjs.diffless

object attachments extends attachments

/** syntax for attaching handles */
trait attachments {
	def attach[N,H](create:N=>H):Attachment[N,H]	= Attachment(create)
}
