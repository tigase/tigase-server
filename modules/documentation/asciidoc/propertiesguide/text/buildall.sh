#!/bin/sh

asciidoc --verbose Properties\ Guide\ 01\ -\ --admins.txt
asciidoc --verbose Properties\ Guide\ 02\ -\ --auth-db.txt
asciidoc --verbose Properties\ Guide\ 03\ -\ --auth-db-uri.txt
asciidoc --verbose Properties\ Guide\ 04\ -\ --auth-domain-repo-pool.txt
asciidoc --verbose Properties\ Guide\ 05\ -\ --auth-repo-pool.txt
asciidoc --verbose Properties\ Guide\ 06\ -\ --auth-repo-pool-size.txt
asciidoc --verbose Properties\ Guide\ 07\ -\ --bind-ext-hostnames.txt
asciidoc --verbose Properties\ Guide\ 08\ -\ --bosh-close-connection.txt
asciidoc --verbose Properties\ Guide\ 09\ -\ --bosh-extra-headers-file.txt
asciidoc --verbose Properties\ Guide\ 10\ -\ --cl-conn-repo-class.txt
asciidoc --verbose Properties\ Guide\ 11\ -\ --client-access-policy-file.txt
asciidoc --verbose Properties\ Guide\ 12\ -\ --cluster-connect-all.txt
asciidoc --verbose Properties\ Guide\ 13\ -\ --cluster-mode.txt
asciidoc --verbose Properties\ Guide\ 14\ -\ --cluster-nodes.txt
asciidoc --verbose Properties\ Guide\ 15\ -\ --cm-ht-traffic-throttling.txt
asciidoc --verbose Properties\ Guide\ 16\ -\ --cm-see-other-host.txt
asciidoc --verbose Properties\ Guide\ 17\ -\ --cm-traffic-throttling.txt
asciidoc --verbose Properties\ Guide\ 18\ -\ --cmpname-ports.txt
asciidoc --verbose Properties\ Guide\ 19\ -\ --comp-class.txt
asciidoc --verbose Properties\ Guide\ 20\ -\ --comp-name.txt
asciidoc --verbose Properties\ Guide\ 21\ -\ --cross-domain-policy-file.txt
asciidoc --verbose Properties\ Guide\ 22\ -\ --data-repo-pool-size.txt
asciidoc --verbose Properties\ Guide\ 23\ -\ --debug.txt
asciidoc --verbose Properties\ Guide\ 24\ -\ --debug-packages.txt
asciidoc --verbose Properties\ Guide\ 25\ -\ --domain-filter-policy.txt
asciidoc --verbose Properties\ Guide\ 26\ -\ --elements-number-limit.txt
asciidoc --verbose Properties\ Guide\ 27\ -\ --ext-comp.txt
asciidoc --verbose Properties\ Guide\ 28\ -\ --extcomp-repo-class.txt
asciidoc --verbose Properties\ Guide\ 29\ -\ --external.txt
asciidoc --verbose Properties\ Guide\ 30\ -\ --hardened-mode.txt
asciidoc --verbose Properties\ Guide\ 31\ -\ --max-queue-size.txt
asciidoc --verbose Properties\ Guide\ 32\ -\ --monitoring.txt
asciidoc --verbose Properties\ Guide\ 33\ -\ --net-buff-high-throughput.txt
asciidoc --verbose Properties\ Guide\ 34\ -\ --net-buff-standard.txt
asciidoc --verbose Properties\ Guide\ 35\ -\ --new-connections-throttling.txt
asciidoc --verbose Properties\ Guide\ 36\ -\ --nonpriority-queue.txt
asciidoc --verbose Properties\ Guide\ 37\ -\ --queue-implementation.txt
asciidoc --verbose Properties\ Guide\ 38\ -\ --roster-implementation.txt
asciidoc --verbose Properties\ Guide\ 39\ -\ --s2s-ejabberd-bug-workaround-active.txt
asciidoc --verbose Properties\ Guide\ 40\ -\ --s2s-secret.txt
asciidoc --verbose Properties\ Guide\ 41\ -\ --s2s-skip-tls-hostnames.txt
asciidoc --verbose Properties\ Guide\ 42\ -\ --script-dir.txt
asciidoc --verbose Properties\ Guide\ 43\ -\ --sm-cluster-strategy-class.txt
asciidoc --verbose Properties\ Guide\ 44\ -\ --sm-plugins.txt
asciidoc --verbose Properties\ Guide\ 45\ -\ --sm-threads-pool.txt
asciidoc --verbose Properties\ Guide\ 46\ -\ --ssl-certs-location.txt
asciidoc --verbose Properties\ Guide\ 47\ -\ --ssl-container-class.txt
asciidoc --verbose Properties\ Guide\ 48\ -\ --ssl-def-cert-domain.txt
asciidoc --verbose Properties\ Guide\ 49\ -\ --stats-archiv.txt
asciidoc --verbose Properties\ Guide\ 50\ -\ --stats-history.txt
asciidoc --verbose Properties\ Guide\ 51\ -\ --stringprep-processor.txt
asciidoc --verbose Properties\ Guide\ 52\ -\ --test.txt
asciidoc --verbose Properties\ Guide\ 53\ -\ --tigase-config-repo-class.txt
asciidoc --verbose Properties\ Guide\ 54\ -\ --tigase-config-repo-uri.txt
asciidoc --verbose Properties\ Guide\ 55\ -\ --tls-jdk-nss-bug-workaround-active.txt
asciidoc --verbose Properties\ Guide\ 56\ -\ --trusted.txt
asciidoc --verbose Properties\ Guide\ 57\ -\ --user-db.txt
asciidoc --verbose Properties\ Guide\ 58\ -\ --user-db-uri.txt
asciidoc --verbose Properties\ Guide\ 59\ -\ --user-domain-repo-pool.txt
asciidoc --verbose Properties\ Guide\ 60\ -\ --user-repo-pool.txt
asciidoc --verbose Properties\ Guide\ 61\ -\ --user-repo-pool-size.txt
asciidoc --verbose Properties\ Guide\ 62\ -\ --vhost-anonymous-enabled.txt
asciidoc --verbose Properties\ Guide\ 63\ -\ --vhost-max-users.txt
asciidoc --verbose Properties\ Guide\ 64\ -\ --vhost-message-forward-jid.txt
asciidoc --verbose Properties\ Guide\ 65\ -\ --vhost-presence-forward-jid.txt
asciidoc --verbose Properties\ Guide\ 66\ -\ --vhost-register-enabled.txt
asciidoc --verbose Properties\ Guide\ 67\ -\ --vhost-tls-required.txt
asciidoc --verbose Properties\ Guide\ 68\ -\ --virt-hosts.txt
asciidoc --verbose Properties\ Guide\ 69\ -\ --watchdog_delay.txt
asciidoc --verbose Properties\ Guide\ 70\ -\ --watchdog_ping_type.txt
asciidoc --verbose Properties\ Guide\ 71\ -\ --watchdog_timeout.txt
asciidoc --verbose Properties\ Guide\ 72\ -\ config-type.txt
asciidoc --verbose PropertiesGuide.txt

sed -i 's/^.*Reformatted for AsciiDoc.*$//' PropertiesGuide.html
sed -i 's/^.*:toc:.*$//' PropertiesGuide.html
sed -i 's/^.*:numbered:.*$//' PropertiesGuide.html
sed -i 's/^.*:website:.*$//' PropertiesGuide.html
sed -i 's/^.*:Date:.*$/<\/p><\/div>/' PropertiesGuide.html

/bin/mv *.html ../html/

