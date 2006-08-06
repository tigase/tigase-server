(jde-project-file-version "1.0")

(jde-set-variables
 '(jde-electric-return-p t)
 '(jde-enable-abbrev-mode t)
 '(jde-global-classpath
   (quote
    (
     "$JAVA_HOME/jre/lib/rt.jar"
     "$PROJECTS_HOME/tigase/server/classes/"
     "$PROJECTS_HOME/tigase/server/libs/junit.jar"
     "$PROJECTS_HOME/tigase/server/libs/tigase-xmltools.jar"
     )))
 '(jde-sourcepath
   (quote
    (
     "$PROJECTS_HOME/tigase/server/src"
     "$PROJECTS_HOME/tigase/server/tests/unittests/src"
     "$JAVA_HOME/share/src"
     "$JAVA_HOME/share/tests/src")))
 '(jde-gen-k&r t)
 '(user-mail-address "artur.hefczyc@tigase.org")
 '(indent-tabs-mode t)
)
