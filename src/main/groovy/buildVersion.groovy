import java.nio.file.Paths
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipFile

String root = System.getProperty("user.dir");

def jarDir = "./target/dist/jars/"
def tigaseLibraries = Paths.get(root, jarDir).toFile().listFiles(new FileFilter() {

	@Override
	boolean accept(File pathname) {
		def filename = pathname.toPath().getFileName().toString()
		return (filename.endsWith(".jar") && (filename.startsWith("tigase-") || filename.startsWith("jaxmpp-"))) ||
				filename.equals("licence-lib.jar")
	}
})

AtomicInteger build = new AtomicInteger(0);

tigaseLibraries.each { file ->
	def zipFile = new ZipFile(file)
	def entry = zipFile.getEntry("META-INF/MANIFEST.MF")
	def reader = new InputStreamReader(zipFile.getInputStream(entry))
	def lines = reader.readLines()
	def implementationBuild = lines.find { line -> line.startsWith("Implementation-Build") }
	if (implementationBuild != null) {
		def buildNumber = implementationBuild.replaceAll("Implementation-Build: (\\d+)/.*", "\$1")
		try {
			build.addAndGet(Integer.valueOf(buildNumber))
		} catch (NumberFormatException e) {
			log.warn("Wrong format for: " + file.toPath().getFileName().toString() + ": " + buildNumber)
		}
	}
}

log.info("setting revision to: " + build)
project.properties.setProperty('gitVersion', build.toString())

