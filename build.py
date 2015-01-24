
import os
import sys

# settings
PathSsjb = "../ssjb"
GroupId = "cuchaz"
ProjectName = "enigma"
Version = "0.6b"
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
ProguardDeps = [
	ssjb.ivy.Dep("net.sf.proguard:proguard-base:5.1")
]
TestDeps = [
	ssjb.ivy.Dep("junit:junit:4.12"),
	ssjb.ivy.Dep("org.hamcrest:hamcrest-all:1.3")
]

# functions

def getJarFullName(name=None) :
	if name is not None:
		return "%s-%s-%s.jar" % (ProjectName, name, Version)
	else:
		return "%s-%s.jar" % (ProjectName, Version)

def buildJar():
	os.makedirs(DirTemp)
	ssjb.copyFiles(DirTemp, DirBin, ssjb.findFiles(DirBin))
	# TODO: teach ssjb where to find ivy jars
	ssjb.unpackJars(DirTemp, "ivy/bundles", recursive=True)
	ssjb.unpackJars(DirTemp, "ivy/jars", recursive=True)
	ssjb.unpackJars(DirTemp, "libs", recursive=True)
	ssjb.delete(os.path.join(DirTemp, "LICENSE.txt"))
	ssjb.delete(os.path.join(DirTemp, "META-INF/maven"))
	ssjb.copyFile(DirTemp, "license.APL2.txt")
	ssjb.copyFile(DirTemp, "license.GPL3.txt")
	ssjb.copyFile(DirTemp, "readme.txt")
	manifest = ssjb.buildManifest(ProjectName, Version, Author, "cuchaz.enigma.Main")
	pathJar = os.path.join(DirBuild, getJarFullName()) 
	ssjb.jar(pathJar, DirTemp, manifest=manifest)
	ssjb.delete(DirTemp)
	ssjb.deployJarToLocalMavenRepo(
		PathLocalMavenRepo,
		pathJar,
		"%s:%s:%s" % (GroupId, ProjectName, Version)
	)

def taskMain():
	ssjb.delete(DirBuild)
	os.makedirs(DirBuild)
	buildJar()

def makeTestJar(name, glob):

	pathJar = os.path.join(DirBuild, "%s.jar" % name)
	pathObfJar = os.path.join(DirBuild, "%s.obf.jar" % name)

	# build the deobf jar
	ssjb.file.delete(DirTemp)
	ssjb.file.mkdir(DirTemp)
	ssjb.file.copyTree(DirTemp, DirBin, ssjb.file.find(DirBin, "cuchaz/enigma/inputs/Keep.class"))
	ssjb.file.copyTree(DirTemp, DirBin, ssjb.file.find(DirBin, glob))
	ssjb.jar.makeJar(pathJar, DirTemp)
	ssjb.file.delete(DirTemp)

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
	ssjb.ivy.makeJar(os.path.join(DirLib, "proguard.jar"), ProguardDeps)

def taskBuildTestJars():
	makeTestJar("testLoneClass", "cuchaz/enigma/inputs/loneClass/*.class")
	makeTestJar("testConstructors", "cuchaz/enigma/inputs/constructors/*.class")
	makeTestJar("testInheritanceTree", "cuchaz/enigma/inputs/inheritanceTree/*.class")
	makeTestJar("testInnerClasses", "cuchaz/enigma/inputs/innerClasses/*.class")


ssjb.registerTask("getDeps", taskGetDeps)
ssjb.registerTask("main", taskMain)
ssjb.registerTask("buildTestJars", taskBuildTestJars)
ssjb.run()

