How to generate installer:
---------------------------

This directory contains files needed to configure and extend IzPack installer in 
order to create Tigase installer. 
To generate installer:

1. Install chosen version of IzPack including source code.

2. In order to compile custom Tigase panels you need to first compile IzPack
classes. You can use the included build.xml which is in the src directory of 
IzPack install. Just enter this dir and type: ant all 

3. Make sure that the bin/panels directory of IzPack is writable by 
generate-installer.sh script. Compiled custom panels will be placed here before 
running installer compiler. 

4. Modify the script/generate-installer.sh. Change the IZPACK_DIR
variable to point to the IzPack instalation directory e.g.
IZPACK_DIR="/usr/local/IzPack421"

5. To start the installation process run the scripts/generate-installer.sh 
file you will find in the main server source code directory. You should start 
it from the server root dir.

6. Generated files (jar and exe) will be placed in the packages dir of 
Tigase codebase.
