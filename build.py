
import os
import ssjb

# settings
GroupId = "cuchaz"
ProjectName = "enigma"
Version = "0.6b"
Author = "Cuchaz"

DirBin = "bin"
DirBuild = "build"
DirTemp = os.path.join(DirBuild, "tmp")
PathLocalMavenRepo = "../maven"


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
	ssjb.jar(os.path.join(DirBuild, getJarFullName()), DirTemp, manifest=manifest)
	ssjb.delete(DirTemp)
	ssjb.deployJarToLocalMavenRepo(
		PathLocalMavenRepo,
		getJarFullName(),
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
	ssjb.delete(DirTemp)
	os.makedirs(DirTemp)
	ssjb.copyFiles(DirTemp, DirBin, ssjb.findFiles(DirBin, "cuchaz/enigma/inputs/Keep.class"))
	ssjb.copyFiles(DirTemp, DirBin, ssjb.findFiles(DirBin, glob))
	ssjb.jar(pathJar, DirTemp)
	ssjb.delete(DirTemp)

	# build the obf jar
	ssjb.callJavaJar("libs/proguard.jar", ["@proguard.conf", "-injars", pathJar, "-outjars", pathObfJar])

def taskBuildTestJars():
	makeTestJar("testLoneClass", "cuchaz/enigma/inputs/loneClass/*.class")
	makeTestJar("testConstructors", "cuchaz/enigma/inputs/constructors/*.class")
	makeTestJar("testInheritanceTree", "cuchaz/enigma/inputs/inheritanceTree/*.class")
	makeTestJar("testInnerClasses", "cuchaz/enigma/inputs/innerClasses/*.class")


ssjb.registerTask("main", taskMain)
ssjb.registerTask("buildTestJars", taskBuildTestJars)
ssjb.run()

