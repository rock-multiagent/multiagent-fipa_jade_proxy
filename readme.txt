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


== Additional information ==
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


== Testing ==

Ruby test for usage with multiagent/orogen/fipa_services can be found in this project in: 

    src/main/ruby

== Troubleshooting ==

== mvn assembly::assembly fails ==

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
