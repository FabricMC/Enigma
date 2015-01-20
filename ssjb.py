
# stupidly simple jar builder
# Jeff Martin
# 2015-01-05

import os
import sys
import shutil
import subprocess
import zipfile
import tempfile
import re
import fnmatch


# setup tasks
tasks = {}

def registerTask(name, func):
	tasks[name] = func

def run():

	# get the task name
	taskName = "main"
	if len(sys.argv) > 1:
		taskName = sys.argv[1]
	
	# find that task
	try:
		task = tasks[taskName]
	except:
		print "Couldn't find task: %s" % taskName
		return

	# run it!
	print "Running task: %s" % taskName
	task()


# set up the default main task
def mainTask():
	print "The main task doesn't do anything by default"

registerTask("main", mainTask)


# library of useful functions

def findFiles(dirSrc, pattern=None):
	out = []
	for root, dirs, files in os.walk(dirSrc):
		for file in files:
			path = os.path.join(root, file)[len(dirSrc) + 1:]
			if pattern is None or fnmatch.fnmatch(path, pattern):
				out.append(path)
	return out

def copyFile(dirDest, pathSrc, renameTo=None):
	(dirParent, filename) = os.path.split(pathSrc)
	if renameTo is None:
		renameTo = filename
	pathDest = os.path.join(dirDest, renameTo)
	shutil.copy2(pathSrc, pathDest)

def copyFiles(dirDest, dirSrc, paths):
	for path in paths:
		pathSrc = os.path.join(dirSrc, path)
		pathDest = os.path.join(dirDest, path)
		dirParent = os.path.dirname(pathDest)
		if not os.path.isdir(dirParent):
			os.makedirs(dirParent)
		shutil.copy2(pathSrc, pathDest)

def patternStringToRegex(patternString):

	# escape special chars
	patternString = re.escape(patternString)
	
	# process ** and * wildcards
	replacements = {
		re.escape("**"): ".*",
		re.escape("*"): "[^" + os.sep + "]+"
	}
	def getReplacement(match):
		print "matched", match
		return "a"
	patternString = re.compile("(\\\\\*)+").sub(lambda m: replacements[m.group(0)], patternString)

	return re.compile("^" + patternString + "$")

def matchesAnyPath(path, patternStrings):
	for patternString in patternStrings:
		pattern = patternStringToRegex(patternString)
		# TEMP
		print path, pattern.match(path) is not None
		if pattern.match(path) is not None:
			return True
	return False

def delete(path):
	try:
		if os.path.isdir(path):
			shutil.rmtree(path)
		elif os.path.isfile(path):
			os.remove(path)
	except:
		# don't care if it failed
		pass

def buildManifest(title, version, author, mainClass=None):
	manifest = {
		"Title": title,
		"Version": version,
		"Created-by": author
	}
	if mainClass is not None:
		manifest["Main-Class"] = mainClass
	return manifest

def jar(pathOut, dirIn, dirRoot=None, manifest=None):

	# build the base args
	if dirRoot is None:
		dirRoot = dirIn
		dirIn = "."
	invokeArgs = ["jar"]
	filesArgs = ["-C", dirRoot, dirIn]

	if manifest is not None:
		# make a temp file for the manifest
		tempFile, tempFilename = tempfile.mkstemp(text=True)
		try:
			# write the manifest
			for (key, value) in manifest.iteritems():
				os.write(tempFile, "%s: %s\n" % (key, value))
			os.close(tempFile)

			# build the jar with a manifest
			subprocess.call(invokeArgs + ["cmf", tempFilename, pathOut] + filesArgs)

		finally:
			os.remove(tempFilename)
	else:
		# just build the jar without a manifest
		subprocess.call(invokeArgs + ["cf", pathOut] + filesArgs)

	print "Wrote jar: %s" % pathOut

def unpackJar(dirOut, pathJar):
	with zipfile.ZipFile(pathJar) as zf:
		for member in zf.infolist():
			zf.extract(member, dirOut)
	print "Unpacked jar: %s" % pathJar

def unpackJars(dirOut, dirJars, recursive=False):
	for name in os.listdir(dirJars):
		path = os.path.join(dirJars, name)
		if os.path.isfile(path):
			if name[-4:] == ".jar":
				unpackJar(dirOut, path)
		elif os.path.isdir(path) and recursive:
			unpackJars(dirOut, path, recursive)

def callJava(classpath, className, javaArgs):
	subprocess.call(["java", "-cp", classpath, className] + javaArgs)

def callJavaJar(jar, javaArgs):
	subprocess.call(["java", "-jar", jar] + javaArgs)

