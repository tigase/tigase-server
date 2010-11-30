How to generate installer:
---------------------------

Tigase installer modifications are of two kinds:
1. Tigase panels are provided in src/main/izpack/java directory for easy tracking in svn.

2. Modifications related to the IzPack installer are provided as a patch file. Unfortunately one of 
src/main/izpack/changes.patch

important: build.xml for izpack has conflicting changes related to both TigasePanels and to generic IzPack
functionality. This is why it is only provided in the patch. If you add/remove panels you will need to
modify the patch too. 


The first time after checking Tigase server source code and after you change some installation code i.e. 
improve IzPack installer or add some panels, you will need to build the patched installer.
You can use the scripts/prepare-installer.sh to download needed IzPack version, install it in a local
directory, automaticaly patch it and build at the end. If the IzPack version of installer gets
downloaded you can comment the svn checkout line to not download it every time when you prepare
installer generator. 


After creating compiler generator it will sit in the installer/izpack.patched directory. You can use
the script/generate-installer.sh to create TigaseInstaller from it without regeneratting installer
generator every time.

Requirements to build the installer (various steps):
- git
- python2
- the docutils module (see http://docutils.sourceforge.net/)
- a LaTeX distribution to invoke 'pdflatex' (MikTeX, TeXLive, teTeX, ...)