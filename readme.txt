== Installation ==

1. Run 'mvn install' in this directory.

2. Create a new Java Maven project.
     - New Project -> Maven -> Java application
b) Console::
     - Run 'mvn archetype:create  -DgroupId=[your project's group id] -DartifactId=[your project's artifact id]'
     - groupId should be your highlevel 'package' description under which you place the project
     - artifactId will be you project name

3. Add this dependency to your pom.xml:
<dependency>
    <groupId>de.dfki</groupId>
    <artifactId>jade-rock-fipa-proxy</artifactId>
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
       Arguments: -gui -agents tcpMtpAgent:de.dfki.jade_rock_fipa_proxy.TcpMtpAgent
       (you can leave '-gui' out, if you don't need the jade GUI)
     - Run the new configuration
b) Console:
     - Run 'mvn assembly:assembly' in your project's directory (where your pom.xml is).
     - Run 'java -jar ./target/<yourproject>-jar-with-dependencies.jar -gui -agents tcpMtpAgent:de.dfki.jade_rock_fipa_proxy.TcpMtpAgent'

Alter the command to include your agents as necessary. Alternatively,
create your own main class, that calls the Jade main class with these arguments.

== Testing ==

Ruby test for usage with multiagent/orogen/fipa_services can be found in this project in: 

    src/main/ruby


== Development ==

When sending a message from Jade to Rock, the tcp address must be known.
The AMS can be searched for that address, as all foreign agents are registered
there. To obtain a valid AID by just knowing the name, consider the following
code, executed inside an agent ('this' refers to an agent):

AID aid = new AID("rock_agent", true);
AMSAgentDescription ad = new AMSAgentDescription();
ad.setName(aid);
// Check it is registered.
AMSAgentDescription[] res = AMSService.search(this, ad);
if(res.length > 0) {
    aid = res[0].getName();
}

== Technical details ==    

Jade can forward FIPA envelopes to foreign platforms with 'Message Transports'. The standard transports
defined in the FIPA standard which Jade implements are HTTP and IIOS. Rock used to support only UDT, but
TCP has been added as well. The class de.dfki.jade_rock_fipa_proxy.mtp.TcpMtp implements the tcp Message
Transport in Jade. It's task is to send outgoing messages whereas TcpServer (same package) receives
incoming messages. The best encoding combination which seems to be the least error-prone, is XML encoding
for the envelopes and String encoding for the ACL-Messages. The de.dfki.jade_rock_fipa_proxy.TcpMtpAgent
starts and registers the Mtp and starts other central behaviours.

To be able to find the tcp addresses of other agents, Rock uses DNS-SD, also known as zeroconf/bonjour/avahi.
Using the Java library JMDNS, this behaviour is also implemented in de.dfki.jade_rock_fipa_proxy.JMDNSManager.
All Jade agent are registered with it and spawning non-Jade agents (in the same service directory) are 
propagated using the AMS agent.

The classe de.dfki.jade_rock_fipa_proxy.JadeproxyAgent and de.dfki.jade_rock_fipa_proxy.RockDummyAgent are
deprecated.

See http://fipa.org/ and http://jade.tilab.com/ for more information.

=== Open problems ===

JMDNS cannot handle services containing a dot ('.') in their service name. It can neither publish nor resolve
such services. As the global name of Jade agents contains the IP address, e.g. 'jadeAgent@10.0.0.1:1099/JADE',
this is a problem. To be able to differentiate between Jade agents with the same name in different systems,
the local name does not suffice. Therefore, the Jade agents are published replacing all dots with '-dot-'.

To address agents correctly, all outgoing messages not to be modified to include replace dots, and the same 
has to be done vice versa when receiving messages.


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
