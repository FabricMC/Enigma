
import os
import ssjb

# settings
projectName = "enigma"
version = "0.6b"
author = "Cuchaz"

dirBin = "bin"
dirBuild = "build"
dirTemp = os.path.join(dirBuild, "tmp")


def getJarFullName(name=None) :
	if name is not None:
		return "%s-%s-%s.jar" % (projectName, name, version)
	else:
		return "%s-%s.jar" % (projectName, version)

def buildGuiJar():
	jarName = "gui"
	os.makedirs(dirTemp)
	ssjb.copyFiles(dirTemp, dirBin, ssjb.findFiles(dirBin))
	ssjb.unpackJars(dirTemp, "ivy/bundles", recursive=True)
	ssjb.unpackJars(dirTemp, "ivy/jars", recursive=True)
	ssjb.unpackJars(dirTemp, "libs", recursive=True)
	ssjb.delete(os.path.join(dirTemp, "LICENSE.txt"))
	ssjb.copyFile(dirTemp, "license.APL2.txt")
	ssjb.copyFile(dirTemp, "license.GPL3.txt")
	ssjb.copyFile(dirTemp, "readme.txt")
	manifest = ssjb.buildManifest(projectName, version, author, "cuchaz.enigma.Main")
	ssjb.jar(os.path.join(dirBuild, getJarFullName()), dirTemp, manifest=manifest)
	ssjb.delete(dirTemp)

def buildTranslateJar():
	jarName = "translate"
	os.makedirs(dirTemp)
	files = ssjb.findFiles(dirBin, "cuchaz/enigma/mapping/*")
	files += ssjb.findFiles(dirBin, "cuchaz/enigma/bytecode/*")
	ssjb.copyFiles(dirTemp, dirBin, files)
	ssjb.copyFile(dirTemp, "license.GPL3.txt", renameTo="license.txt")
	ssjb.copyFile(dirTemp, "readme.translate.txt", renameTo="readme.txt")
	manifest = ssjb.buildManifest("%s-%s" % (projectName, jarName), version, author)
	ssjb.jar(os.path.join(dirBuild, getJarFullName(jarName)), dirTemp, manifest=manifest)
	ssjb.delete(dirTemp)

def taskMain():
	ssjb.delete(dirBuild)
	os.makedirs(dirBuild)
	buildGuiJar()
	buildTranslateJar()


def makeTestJar(name, glob):

	pathJar = os.path.join(dirBuild, "%s.jar" % name)
	pathObfJar = os.path.join(dirBuild, "%s.obf.jar" % name)

	# build the deobf jar
	ssjb.delete(dirTemp)
	os.makedirs(dirTemp)
	ssjb.copyFiles(dirTemp, dirBin, ssjb.findFiles(dirBin, "cuchaz/enigma/inputs/Keep.class"))
	ssjb.copyFiles(dirTemp, dirBin, ssjb.findFiles(dirBin, glob))
	ssjb.jar(pathJar, dirTemp)
	ssjb.delete(dirTemp)

	# build the obf jar
	ssjb.callJavaJar("libs/proguard.jar", ["@proguard.conf", "-injars", pathJar, "-outjars", pathObfJar])

def taskBuildTestJars():
	makeTestJar("testLoneClass", "cuchaz/enigma/inputs/loneClass/*.class")
	makeTestJar("testConstructors", "cuchaz/enigma/inputs/constructors/*.class")
	makeTestJar("testInheritanceTree", "cuchaz/enigma/inputs/inheritanceTree/*.class")
	makeTestJar("testInnerClasses", "cuchaz/enigma/inputs/innerClasses/*.class")

ssjb.registerTask("main", taskMain)
ssjb.registerTask("buildTestJars", taskBuildTestJars)


if __name__ == "__main__":
	ssjb.run()

