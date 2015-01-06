
import os
import ssjb

# settings
projectName = "enigma"
version = "0.6b"
author = "Cuchaz"

dirBin = "bin"
dirBuild = "build"
dirTemp = os.path.join(dirBuild, "tmp")


def getJarFullName(name) :
    return "%s-%s-%s.jar" % (projectName, name, version)

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
    ssjb.copyFile(dirTemp, "readme.gui.txt", renameTo="readme.txt")
    manifest = ssjb.buildManifest("%s-%s" % (projectName, jarName), version, author, "cuchaz.enigma.Main")
    ssjb.jar(os.path.join(dirBuild, getJarFullName(jarName)), dirTemp, manifest=manifest)
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


ssjb.registerTask("main", taskMain)


if __name__ == "__main__":
    ssjb.run()

