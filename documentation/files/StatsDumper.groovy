//CLASSPATH=/home/tigase/tigase-server/jars/tigase-server.jar
import tigase.stats.JavaJMXProxy
import tigase.stats.JMXProxyListener
import tigase.stats.StatisticsProviderMBean

if (args.size() > 0) {
  if (args[0] == "-h") {
    println ("Parameters are: [hostname] [username] [password] [dir] [port] [delay(ms)] [interval(ms)] [loadhistory(bool)]")
    System.exit(0)
  }
}


def hostname = args.size() > 0 ? args[0] : "0.0.0.0"
def username = args.size() > 1 ? args[1] : "admin"
def password = args.size() > 2 ? args[2] : "pass"
def dir = args.size() > 3 ? args[3] : "stats"
def port = args.size() > 4 ? args[4].toInteger() : 9050
def delay = args.size() > 5 ? args[5].toLong() : 10000
def interval = args.size() > 6 ? args[6].toLong() : 10000
def loadHistory = args.size() > 7 ? args[7].toBoolean() : false

if (!new File(dir).exists()) {
  new File(dir).mkdir()
}

def proxy = new JavaJMXProxy(hostname, hostname, port, username, password, delay, interval, loadHistory)
proxy.start()

sleep delay

while (true) {
  try {
    def allStats = proxy.getAllStats(0)
    if (allStats != null) {
      def timeToken = (int) System.currentTimeMillis() / 1000.toInteger() / 60.toInteger()
      def file1 = new File("${dir}/stats_${new Date().format('yyyy-MM-dd_hh:mm:ss')}.txt")
      println "Saving stats to the file ${file1.path}"
      allStats.each { key, value -> file1 << key + "     " + value + "\n" }
    }
  } catch(all) {
    println("An exception: " + all)
  }
  sleep interval
}
