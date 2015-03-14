## What is Winstone ? ##

<p>Winstone is a servlet container that was written out of a desire to provide servlet functionality without the bloat that full J2EE compliance introduces.</p>
<p>It is not intended to be a completely fully functional J2EE style servlet container (by this I mean supporting extraneous APIs unrelated to Servlets, such as JNDI, JavaMail, EJBs, etc) - this is left to Tomcat, Jetty, Resin, JRun, Weblogic et al.</p>

<p>Sometimes you want just a simple servlet container - without all the other junk - that just goes.<br />
This is where Winstone is best suited.</p>

<p>You could find some documentation and support on this site: <a href='http://intelligents-ia.com/index.php/category/technique/Winstone'>http://intelligents-ia.com/index.php/category/technique/Winstone</a> )<br>
<br>
<p>The original goals in writing Winstone were:</p>
<ul>
<li>Supply fast, reliable servlet container functionality for a single webapp per server</li>
<li>Keep the size of the core distribution jar as low as possible</li>
<li>Keep configuration files to an absolute minimum, using command line options to optionally override sensible compiled in defaults.</li>
<li>Optionally support JSP compilation using Apache's Jasper. (<a href='http://jakarta.apache.org'>http://jakarta.apache.org</a>)</li>
</ul>

<h2>Why is it called Winstone ?</h2>

<p>The initial versions of Winstone were created by Rick Knowles (The homepage for this project is at '<a href='http://winstone.sourceforge.net'>http://winstone.sourceforge.net</a>').<br />
He actually stopped developing and supporting Winstone, due to other projects. Since then, with his permission, we take the support of this "small" project that has titillate our neurons.<br />
We hope that our little fingers instill enough magic in this project so that you are likely to use it.<br>
</p>

<h2>Latest Release</h2>

2015/01/08 - release 1.7.0<br>
Winstone artifact availaible on maven central.<br>
<br>
2014/04/07 - release 1.0.6<br>
<br>
2012/12/07 - release 1.0.5<br>
<br>
2012/03/13 - release 1.0.4<br>
<br>
<br>
<h2>Next Release</h2>

Comming soon.<br>
<br>
<br>
<h2>Would participate ?</h2>

You're very welcome, contact me by email/issue/tweeter, or whatever, and just explain me what you will want to do evolve/fix/doc ...<br>
It's open mind.<br>
<br>
<h2>Download Winstone</h2>

You could find winstone on maven central repository:<br>
<br>
<a href='http://mvnrepository.com/artifact/org.intelligents-ia.winstone'>http://mvnrepository.com/artifact/org.intelligents-ia.winstone</a>


<h3>Winstone Module</h3>


Winstone Core Module:<br>
<br>
<pre><code>&lt;groupId&gt;org.intelligents-ia.winstone&lt;/groupId&gt;<br>
&lt;artifactId&gt;winstone&lt;/artifactId&gt;<br>
&lt;version&gt;1.7.0&lt;/version&gt;<br>
</code></pre>

Winstone BootStrap Jar:<br>
<br>
<pre><code>&lt;groupId&gt;org.intelligents-ia.winstone&lt;/groupId&gt;<br>
&lt;artifactId&gt;winstone-boot&lt;/artifactId&gt;<br>
&lt;version&gt;1.7.0&lt;/version&gt;<br>
</code></pre>



