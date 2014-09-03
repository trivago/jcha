/*********************************************************************************
 * Copyright 2014-present trivago GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **********************************************************************************/

package com.trivago.jcha.remote;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.trivago.jcha.apps.JavaClassHistogramAnalyzer;

public class DiagnosticMBean
{
    static final Logger logger = LogManager.getLogger(JavaClassHistogramAnalyzer.class);
    JMXConnector jmxc = null;
    MBeanServerConnection mbsc = null;
    final String host;
    final int port;
    
    public DiagnosticMBean(String hostPort)
    {
    	String[] hostPortSplit = hostPort.split(":");
    	if (hostPortSplit.length != 2)
    	{
    		throw new IllegalArgumentException("JMX address must be in the format host:port, but is: " + hostPort);
    	}
    	this.host = hostPortSplit[0];
    	try
    	{
    		this.port = Integer.parseInt(hostPortSplit[1]);
    	}
    	catch (NumberFormatException nfe)
    	{
    		throw new IllegalArgumentException("JMX port must be numeric but is: " + hostPortSplit[1]);
    	}
    }
    
    public void connect()
	{
		JMXServiceURL url;
		String serviceURL = "service:jmx:rmi:///jndi/rmi://" + host + ":" + port + "/jmxrmi";
		try
		{
			url = new JMXServiceURL(serviceURL);
			JMXConnector jmxc = JMXConnectorFactory.connect(url, null);
	    	jmxc.connect(null);
	    	mbsc = jmxc.getMBeanServerConnection();
		}
		catch (Exception e)
		{
			jmxc = null;
			logger.error("Failed to connect to JVM on " + serviceURL);
		}
	}
    
    final String[] EmptyArgs = new String[0];
    public String readHistogram() throws Exception
    {
    	if (mbsc == null)
    		throw new IllegalStateException("No MBeanServerConnection. This is a bug - please call connect() first.");
    	
    	
        ObjectName mbeanName = new ObjectName("com.sun.management:type=DiagnosticCommand");
        Object reply = mbsc.invoke(mbeanName, "gcClassHistogram", new Object[] {EmptyArgs}, new String[] {String[].class.getName()});
        // We know the interface. "gcClassHistogram" returns a single String
        return (String)reply;
    }
}
