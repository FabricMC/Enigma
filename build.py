
import os
import sys

# settings
PathSsjb = "../ssjb"
Author = "Cuchaz"
Version = "0.11b"

DirBin = "bin"
DirLib = "lib"
DirBuild = "build"
PathLocalMavenRepo = "../maven"


# import ssjb
sys.path.insert(0, PathSsjb)
import ssjb
import ssjb.ivy


ArtifactStandalone = ssjb.ivy.Dep("cuchaz:enigma:%s" % Version)
ArtifactLib = ssjb.ivy.Dep("cuchaz:enigma-lib:%s" % Version)

# dependencies
ExtraRepos = [
	"http://maven.cuchazinteractive.com"
]
LibDeps = [
	ssjb.ivy.Dep("com.google.guava:guava:17.0"),
	ssjb.ivy.Dep("org.javassist:javassist:3.19.0-GA"),
	ssjb.ivy.Dep("org.bitbucket.mstrobel:procyon-decompiler:0.5.28-enigma")
]
StandaloneDeps = LibDeps + [
	ssjb.ivy.Dep("de.sciss:syntaxpane:1.1.4")
]
ProguardDep = ssjb.ivy.Dep("net.sf.proguard:proguard-base:5.1")
TestDeps = [
	ssjb.ivy.Dep("junit:junit:4.12"),
	ssjb.ivy.Dep("org.hamcrest:hamcrest-all:1.3")
]

# functions

def buildTestJar(name, glob):

	ssjb.file.mkdir(os.path.join(DirBuild, "test-inputs"))
	ssjb.file.mkdir(os.path.join(DirBuild, "test-obf"))
	pathJar = os.path.join(DirBuild, "test-inputs/%s.jar" % name)
	pathObfJar = os.path.join(DirBuild, "test-obf/%s.jar" % name)

	# build the unobf jar
	with ssjb.file.TempDir("tmp") as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin, "cuchaz/enigma/inputs/Keep.class"))
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin, glob))
		ssjb.jar.makeJar(pathJar, dirTemp)

	# build the obf jar
	ssjb.callJavaJar(
		os.path.join(DirLib, "proguard.jar"),
		["@proguard-test.conf", "-injars", pathJar, "-outjars", pathObfJar]
	)

def buildDeobfTestJar(outPath, inPath):
	ssjb.callJava(
		[DirBin, os.path.join(DirLib, "deps.jar")],
		"cuchaz.enigma.CommandMain",
		["deobfuscate", inPath, outPath]
	)

def applyReadme(dirTemp):
	ssjb.file.copy(dirTemp, "license.APL2.txt")
	ssjb.file.copy(dirTemp, "license.LGPL3.txt")
	ssjb.file.copy(dirTemp, "readme.txt")

def buildStandaloneJar(dirOut):
	with ssjb.file.TempDir(os.path.join(dirOut, "tmp")) as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin))
		for path in ssjb.ivy.getJarPaths(StandaloneDeps, ExtraRepos):
			ssjb.jar.unpackJar(dirTemp, path)
		ssjb.file.delete(os.path.join(dirTemp, "LICENSE.txt"))
		ssjb.file.delete(os.path.join(dirTemp, "META-INF/maven"))
		applyReadme(dirTemp)
		manifest = ssjb.jar.buildManifest(
			ArtifactStandalone.artifactId,
			ArtifactStandalone.version,
			Author,
			"cuchaz.enigma.Main"
		)
		pathFatJar = os.path.join(DirBuild, "%s-fat.jar" % ArtifactStandalone.getName()) 
		ssjb.jar.makeJar(pathFatJar, dirTemp, manifest=manifest)

		# proguard the jar (without obfuscating) to remove some bloat
		# the guava library is particularly bad...
		pathDietJar = os.path.join(DirBuild, "%s.jar" % ArtifactStandalone.getName())
		ssjb.callJavaJar(
			os.path.join(DirLib, "proguard.jar"),
			["@proguard-build.conf", "-injars", pathFatJar, "-outjars", pathDietJar]
		)

		ssjb.ivy.deployJarToLocalMavenRepo(PathLocalMavenRepo, pathDietJar, ArtifactStandalone)


def buildLibJar(dirOut):
	with ssjb.file.TempDir(os.path.join(dirOut, "tmp")) as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin))
		applyReadme(dirTemp)
		pathJar = os.path.join(DirBuild, "%s.jar" % ArtifactLib.getName()) 
		ssjb.jar.makeJar(pathJar, dirTemp)
		ssjb.ivy.deployJarToLocalMavenRepo(PathLocalMavenRepo, pathJar, ArtifactLib, deps=LibDeps)


# tasks

def taskGetDeps():
	"""Get all the Deps."""
	ssjb.file.mkdir(DirLib)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "deps.jar"), StandaloneDeps, extraRepos=ExtraRepos)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "test-deps.jar"), TestDeps)
	ssjb.ivy.makeJar(os.path.join(DirLib, "proguard.jar"), ProguardDep)

def taskBuildTestJars():
	"""Build the test jar."""
	buildTestJar("loneClass", "cuchaz/enigma/inputs/loneClass/*.class")
	buildTestJar("constructors", "cuchaz/enigma/inputs/constructors/*.class")
	buildTestJar("inheritanceTree", "cuchaz/enigma/inputs/inheritanceTree/*.class")
	buildTestJar("innerClasses", "cuchaz/enigma/inputs/innerClasses/*.class")
	taskBuildTranslationTestJar()

def taskBuildTranslationTestJar():
	"""Build the Translation test jar."""
	buildTestJar("translation", "cuchaz/enigma/inputs/translation/*.class")
	buildDeobfTestJar(os.path.join(DirBuild, "test-deobf/translation.jar"), os.path.join(DirBuild, "test-obf/translation.jar"))

def taskBuild():
	"""Build the engima jar."""
	ssjb.file.delete(DirBuild)
	ssjb.file.mkdir(DirBuild)
	buildStandaloneJar(DirBuild)
	buildLibJar(DirBuild)

ssjb.registerTask("getDeps", taskGetDeps)
ssjb.registerTask("buildTestJars", taskBuildTestJars)
ssjb.registerTask("buildTranslationTestJar", taskBuildTranslationTestJar)
ssjb.registerTask("build", taskBuild)
ssjb.run()

