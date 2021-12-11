package sjs.diffless

import org.scalajs.dom.*

/** pseudo-attributes which have turned out to be useful */
object derived {
	val displayed:Attribute[HTMLElement,Boolean]	=
		Attribute.dynamic[HTMLElement,Boolean] { (node, value) =>
			node.style.display	= if (value) "" else "none"
		}

	val visible:Attribute[HTMLElement,Boolean]	=
		Attribute.dynamic[HTMLElement,Boolean] { (node, value) =>
			node.style.visibility	= if (value) "" else "hidden"
		}

	val classSet:Attribute[HTMLElement,Set[String]]	=
		Attribute.dynamic[HTMLElement,Set[String]] { (node, value) =>
			node.className = value mkString " "
		}
}
