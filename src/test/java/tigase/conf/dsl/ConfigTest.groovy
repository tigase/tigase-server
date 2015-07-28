/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
def repoUrl = "jdbc:mysql://"
beans {
	userRepository {
		url = repoUrl
	}
	userRepo2(class:"tigase.db.jdbc.JDBCRepository", url:repoUrl) {
		url = repoUrl + "alaMaKota";
	}
}

test = "ala ma kota"

c2s("tigase.server.xmppclient.ClientConnectionManager") {
	processors {
		'urn:xmpp:sm:3' { 
			"ack-request-count" 10
		}
	}
}

s2s {
	ports = [5269]
}

muc1("tigase.conf.dsl.Dummy") {
	beans {
		userRepository {
			url = "jdbc:postgresql://"
		}
	}
}

muc2("tigase.conf.dsl.Dummy") {
	beans {
		userRepository {
			url = "jdbc:postgresql://"
		}
	}
}