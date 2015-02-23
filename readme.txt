== Installation ==

1. Run 'mvn install' in this directory.
    a. If the dependency to tilab cannot be resolved check the version and update
the pom.xml accordingly
      - http://jade.tilab.com/maven/com/tilab/jade/jade/  will list a folder of
        the correct version

2. Create a new Java Maven project.
     - New Project -> Maven -> Java application
b) Console::
     - Run 'mvn archetype:create  -DgroupId=[your project's group id] -DartifactId=[your project's artifact id]'
     - groupId should be your highlevel 'package' description under which you place the project
     - artifactId will be your project name

3. Add this dependency to your pom.xml:
<dependency>
    <groupId>multiagent</groupId>
    <artifactId>fipa_services</artifactId>
    <version>0.0.1</version>
</dependency>

4. To be able to create an executable, standalone jar file, add the following to your pom.xml:
<build>
    <plugins>
        <plugin>
            <artifactId>maven-assembly-plugin</artifactId>
            <configuration>
                <descriptorRefs>
                    <descriptorRef>jar-with-dependencies</descriptorRef>
                </descriptorRefs>
                <archive>
                    <manifest>
                        <mainClass>jade.Boot</mainClass>
                    </manifest>
                </archive>
            </configuration>
        </plugin>
    </plugins>
</build>

If you have your own main class (that calls Jade's main class), change that accordingly.

5. Running your application:
a) Netbeans: 
     - Add a new Run configuration with 
       Main Class: jade.Boot
       Arguments: -gui -agents tcpMtpAgent:multiagent.fipa_services.TcpMtpAgent
       (you can leave '-gui' out, if you don't need the jade GUI)
     - Run the new configuration
b) Console:
     - Run 'mvn assembly:assembly' in your project's directory (where your pom.xml is).
     - Run: bash startProxy.sh or 'java -jar ./target/<yourproject>-jar-with-dependencies.jar -gui -agents tcpMtpAgent:multiagent.fipa_services.TcpMtpAgent'

Alter the command to include your agents as necessary. Alternatively,
create your own main class, that calls the Jade main class with these arguments.

== Testing ==

Ruby test for usage with multiagent/orogen/fipa_services can be found in this project in: 

    src/main/ruby


== Development ==

The extensively documented class multiagent.fipa_services.example.EchoAgent
should give a good start. 
For main concepts of Jade development, check http://jade.tilab.com/

== Technical details ==    

Jade can forward FIPA envelopes to foreign platforms with 'Message Transports'. The standard transports
defined in the FIPA standard which Jade implements are HTTP and IIOS. Rock used to support only UDT, but
TCP has been added as well. The class multiagent.fipa_services.mtp.TcpMtp implements the tcp Message
Transport in Jade. It's task is to send outgoing messages whereas TcpServer (same package) receives
incoming messages. The best encoding combination which seems to be the least error-prone, is XML encoding
for the envelopes and String encoding for the ACL-Messages. The multiagent.fipa_services.TcpMtpAgent
starts and registers the Mtp and starts other central behaviours.

To be able to find the tcp addresses of other agents, Rock uses DNS-SD, also known as zeroconf/bonjour/avahi.
Using the Java library JMDNS, this behaviour is also implemented in multiagent.fipa_services.JMDNSManager.
All Jade agent are registered with it and spawning non-Jade agents (in the same service directory) are 
propagated using the AMS agent.

See http://fipa.org/ and http://jade.tilab.com/ for more information.

=== Details ===

JMDNS cannot handle services containing a dot ('.') in their service name. It can neither publish nor resolve
such services. As the global name of Jade agents contains the IP address, e.g. 'jadeAgent@10.0.0.1:1099/JADE',
this is a problem. To be able to differentiate between Jade agents with the same name in different systems,
the local name does not suffice. Therefore, the Jade agents are published replacing all dots with '?'.

However, there is not need to adapt the address of the agent address since
conversion is done transparently for users.


== Troubleshooting ==

=== mvn assembly::assembly fails ===

Check that the groupId is correct: http://maven.apache.org/guides/mini/guide-naming-conventions.html
For example: org.rock-robotics is an invalid groupId name

=== Use a different java version ===
Change the source/target specification to the desired java version in the following block in the pom.xml
<build>
    <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.1</version>
          <configuration>
            <source>1.6</source>
            <target>1.6</target>
          </configuration>
        </plugin>
    </plugins>
</build>
