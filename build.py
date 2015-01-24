
import os
import sys

# settings
PathSsjb = "../ssjb"
Author = "Cuchaz"

DirBin = "bin"
DirLib = "lib"
DirBuild = "build"
DirTemp = os.path.join(DirBuild, "tmp")
PathLocalMavenRepo = "../maven"


# import ssjb
sys.path.insert(0, PathSsjb)
import ssjb
import ssjb.ivy


ThisArtifact = ssjb.ivy.Dep("cuchaz:enigma:0.6b")

# dependencies
ExtraRepos = [
	"http://maven.cuchazinteractive.com"
]
Deps = [
	ssjb.ivy.Dep("com.google.guava:guava:17.0"),
	ssjb.ivy.Dep("de.sciss:jsyntaxpane:1.0.0"),
	ssjb.ivy.Dep("org.javassist:javassist:3.18.1-GA"),
	ssjb.ivy.Dep("org.bitbucket.mstrobel:procyon-decompiler:0.5.26-enigma")
]
ProguardDep = ssjb.ivy.Dep("net.sf.proguard:proguard-base:5.1")
TestDeps = [
	ssjb.ivy.Dep("junit:junit:4.12"),
	ssjb.ivy.Dep("org.hamcrest:hamcrest-all:1.3")
]

# functions

def buildTestJar(name, glob):

	pathJar = os.path.join(DirBuild, "%s.jar" % name)
	pathObfJar = os.path.join(DirBuild, "%s.obf.jar" % name)

	# build the deobf jar
	with ssjb.file.TempDir(DirTemp) as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin, "cuchaz/enigma/inputs/Keep.class"))
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin, glob))
		ssjb.jar.makeJar(pathJar, dirTemp)

	# build the obf jar
	ssjb.callJavaJar(
		os.path.join(DirLib, "proguard.jar"),
		["@proguard.conf", "-injars", pathJar, "-outjars", pathObfJar]
	)


# tasks

def taskGetDeps():
	ssjb.file.mkdir(DirLib)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "deps.jar"), Deps, extraRepos=ExtraRepos)
	ssjb.ivy.makeLibsJar(os.path.join(DirLib, "test-deps.jar"), TestDeps)
	ssjb.ivy.makeJar(os.path.join(DirLib, "proguard.jar"), ProguardDep)

def taskBuildTestJars():
	buildTestJar("testLoneClass", "cuchaz/enigma/inputs/loneClass/*.class")
	buildTestJar("testConstructors", "cuchaz/enigma/inputs/constructors/*.class")
	buildTestJar("testInheritanceTree", "cuchaz/enigma/inputs/inheritanceTree/*.class")
	buildTestJar("testInnerClasses", "cuchaz/enigma/inputs/innerClasses/*.class")

def taskBuild():

	# make the build directory
	ssjb.file.delete(DirBuild)
	ssjb.file.mkdir(DirBuild)

	# make the main jar
	with ssjb.file.TempDir(DirTemp) as dirTemp:
		ssjb.file.copyTree(dirTemp, DirBin, ssjb.file.find(DirBin))
		ssjb.jar.unpackJar(dirTemp, os.path.join(DirLib, "deps.jar"))
		ssjb.file.delete(os.path.join(dirTemp, "LICENSE.txt"))
		ssjb.file.delete(os.path.join(dirTemp, "META-INF/maven"))
		ssjb.file.copy(dirTemp, "license.APL2.txt")
		ssjb.file.copy(dirTemp, "license.GPL3.txt")
		ssjb.file.copy(dirTemp, "readme.txt")
		manifest = ssjb.jar.buildManifest(
			ThisArtifact.artifactId,
			ThisArtifact.version,
			Author,
			"cuchaz.enigma.Main"
		)
		pathJar = os.path.join(DirBuild, "%s.jar" % ThisArtifact.getName()) 
		ssjb.jar.makeJar(pathJar, dirTemp, manifest=manifest)
	
	ssjb.ivy.deployJarToLocalMavenRepo(
		PathLocalMavenRepo,
		pathJar,
		ThisArtifact
	)

ssjb.registerTask("getDeps", taskGetDeps)
ssjb.registerTask("buildTestJars", taskBuildTestJars)
ssjb.registerTask("build", taskBuild)
ssjb.run()

