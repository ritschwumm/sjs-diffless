package sjs.diffless

import org.scalajs.dom.raw._

object derived extends derived

trait derived {
	//------------------------------------------------------------------------------
	//## derived

	val display:Attr[HTMLElement,Boolean]	=
			Attr dynamic[HTMLElement,Boolean]		{ (node, value) => node.style.display		= if (value) "" else "none" }

	val visible:Attr[HTMLElement,Boolean]	=
			Attr dynamic[HTMLElement,Boolean]		{ (node, value) => node.style.visibility	= if (value) "" else "hidden" }

	val classSet:Attr[HTMLElement,Set[String]]	=
			Attr dynamic[HTMLElement,Set[String]]	{ (node, value) => node.className			= value mkString " " }

	/*
	val classSet:Attr[HTMLElement,Set[String]]	=
			Attr dynamic[HTMLElement,String] (_.className = _) contraMapModel (_ mkString " ")
	*/
}
