Global / onChangedBuildSource := ReloadOnSourceChanges

inThisBuild(Seq(
	organization	:= "de.djini",
	version			:= "0.45.0",

	scalaVersion	:= "3.3.0",
	scalacOptions	++= Seq(
		"-feature",
		"-deprecation",
		"-unchecked",
		"-Wunused:all",
		"-Xfatal-warnings",
		"-Ykind-projector:underscores",
	),

	versionScheme	:= Some("early-semver")
))

lazy val noTestSettings	=
	Seq(
		test		:= {},
		testQuick	:= {}
	)

lazy val `sjs-diffless` =
	(project in file("."))
	.aggregate(
		`sjs-diffless-core`,
		`sjs-diffless-example`
	)
	.settings(
		publishArtifact := false
		//publish		:= {},
		//publishLocal	:= {}
	)

//------------------------------------------------------------------------------

lazy val `sjs-diffless-core`	=
	(project in file("modules/core"))
	.enablePlugins(
		ScalaJSPlugin
	)
	.dependsOn()
	.settings(
		noTestSettings,
		libraryDependencies ++= Seq(
			"org.scala-js"	%%%	"scalajs-dom"	% "2.6.0"	% "compile"
		)
	)

//------------------------------------------------------------------------------

lazy val appSource	= SettingKey[File]	("appSource")
lazy val appSjs		= SettingKey[File]	("appSjs")
lazy val appTarget	= SettingKey[File]	("appTarget")
lazy val appBuild	= TaskKey[File]		("appBuild")

val jsCompiler	=
	Def.taskDyn {
		Compile / fastOptJS
		/*
		if (development.value)	Compile / fastOptJS
		else					Compile / fullOptJS
		*/
	}

lazy val `sjs-diffless-example`	=
	(project in file("modules/example"))
	.enablePlugins(
		ScalaJSPlugin
	)
	.dependsOn(
		`sjs-diffless-core`
	)
	.settings(
		noTestSettings,

		appSource	:= (Compile / sourceDirectory).value / "webapp",
		appSjs		:= crossTarget.value / "sjs",
		appTarget	:= crossTarget.value / "webapp",
		appBuild	:= {
			// fetch JS files
			val (js, jsMap)	= {
				val attrd	= jsCompiler.value
				val src		= attrd.data
				val map		= attrd get scalaJSSourceMap getOrElse (sys error s"missing map file for $src")
				// file names have been fixed with artifactPath and scalaJSLinkerConfig below
				(src, map)
			}
			val jsToCopy	=
				Vector(
					js		-> (appTarget.value / js.getName),
					jsMap	-> (appTarget.value / jsMap.getName)
				)

			// fetch static assets
			val staticToCopy	=
				appSource.value.allPaths ** -DirectoryFilter pair Path.rebase(appSource.value, appTarget.value)

			// copy together
			streams.value.log info s"building app in ${appTarget.value}"
			IO delete appTarget.value
			appTarget.value mkdirs ()
			IO copy (staticToCopy ++ jsToCopy)

			// BETTER return path mappings
			appTarget.value
		},

		// automatically start on import
		scalaJSUseMainModuleInitializer := true,

		// NOTE somehow this was cleaner
		//relativeSourceMaps	:= true,
		scalaJSLinkerConfig		:= scalaJSLinkerConfig.value withRelativizeSourceMapBase Some((appSjs.value / "index.js").toURI),

		// final name to ensure the .map reference doesn't have to be patched later
		Compile / fastOptJS	/ artifactPath	:= appSjs.value / "index.js",
		Compile / fullOptJS	/ artifactPath	:= appSjs.value / "index.js",

		watchSources	:= watchSources.value :+ Watched.WatchSource(appSource.value)
	)

//------------------------------------------------------------------------------

TaskKey[File]("bundle")	:=
	(`sjs-diffless-example` / appBuild).value

TaskKey[Unit]("demo")	:= {
	import java.awt.Desktop
	import java.net.URI
	val log		= streams.value.log
	val file	= (`sjs-diffless-example` / appBuild).value / "index.html"
	if (Desktop.isDesktopSupported) {
		log info s"opening $file"
		Desktop.getDesktop browse file.toURI
	}
	else {
		log info s"Desktop is not supported"
	}
}
