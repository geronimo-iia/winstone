
# Winstone


## What is Winstone ?


Winstone is a servlet container that was written out of a desire to provide servlet functionality without the bloat that full J2EE compliance introduces.

It is not intended to be a completely fully functional J2EE style servlet container (by this I mean supporting extraneous APIs unrelated to Servlets, such as JNDI, JavaMail, EJBs, etc) - this is left to Tomcat, Jetty, Resin, JRun, Weblogic et al.

Sometimes you want just a simple servlet container - without all the other junk - that just goes.

This is where Winstone is best suited.

You could find some documentation and support on this site: 
 
 - [Github Wiki](https://github.com/geronimo-iia/winstone/wiki)
 - [Blog] http://intelligents-ia.com/index.php/category/technique/Winstone


The original goals in writing Winstone were:

- Supply fast, reliable servlet container functionality for a single webapp per server
- Keep the size of the core distribution jar as low as possible
- Keep configuration files to an absolute minimum, using command line options to optionally override sensible compiled in defaults.
- Optionally support JSP compilation using Apache's Jasper. (http://jakarta.apache.org)

##Why is it called Winstone ?

The initial versions of Winstone were created by Rick Knowles (The homepage for this project is at 'http://winstone.sourceforge.net').

He actually stopped developing and supporting Winstone, due to other projects. Since then, with his permission, we take the support of this "small" project that has titillate our neurons.

We hope that our little fingers instill enough magic in this project so that you are likely to use it.


# Latest Release

2015/01/08 - release 1.7.0 Winstone artifact availaible on maven central.

2014/04/07 - release 1.0.6

2012/12/07 - release 1.0.5

2012/03/13 - release 1.0.4

# Getting Winstone


Since 1.7.0, Winstone is available on [maven central](http://mvnrepository.com/artifact/org.intelligents-ia.winstone).

Winstone Server Bootstrap can be downloaded [here](http://mvnrepository.com/artifact/org.intelligents-ia.winstone/winstone-boot).


Winstone Core Module:

```
<groupId>org.intelligents-ia.winstone</groupId>
<artifactId>winstone</artifactId>
<version>1.7.0</version>
```

Winstone BootStrap Jar:

```
<groupId>org.intelligents-ia.winstone</groupId>
<artifactId>winstone-boot</artifactId>
<version>1.7.0</version>
```



