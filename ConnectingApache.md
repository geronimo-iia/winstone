## Connecting Winstone to Apache ##

### AJP13 Connector ###
You could find a good "How to" on [Tomcat Apache With AJP13 connector](http://tomcat.apache.org/connectors-doc/generic_howto/quick.html).
Winstone command line switch are :
```
--ajp13Port              = set the ajp13 listening port. -1 to disable, Default is 8009
--ajp13ListenAddress     = set the ajp13 listening address. Default is all interfaces
```

### HTTP Connector ###
Another solution is to use "mod\_proxy" module of apache webs server.

#Activate proxy module on apache
```
LoadModule proxy_module  {path-to-modules}/mod_proxy.so
AddModule  mod_proxy.c
```
#Include directive for each web application that you wish to forward to winstone:
```
ProxyPass         /myapp  http://localhost:8080/myapp
ProxyPassReverse  /myapp  http://localhost:8080/myapp
```

Winstone commande line swicth are:
```
--httpPort               = set the http listening port. -1 to disable, Default is 8080
--httpListenAddress      = set the http listening address. Default is all interfaces
```