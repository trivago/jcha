package com.trivago.jcha.core;

public enum Version
{
	JCHA("1.2"), JCHAGUI("0.8");
	
	private final String version;

	Version(String version)
	{
		this.version = version;
	}

	public String versionString()
	{
		return "v" + version;
	}
}
