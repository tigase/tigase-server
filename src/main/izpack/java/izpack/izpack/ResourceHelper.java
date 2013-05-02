package com.izforge.izpack.installer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class ResourceHelper {
	
	public String getResourceAsString(String resourceId) 
	throws ResourceNotFoundException
	{
        try
        {
            URL url = ResourceManager.getInstance().getURL(resourceId);
            if (url == null) 
            {
            	throw new RuntimeException("Invalid resource URL");
            }
            
            InputStreamReader reader = new InputStreamReader(url.openStream());
            BufferedReader bufferedReader = new BufferedReader(reader);
            return new IOHelper().readAll(bufferedReader);
        }
        catch (Exception ex)
        {
            throw new ResourceNotFoundException(
            		"Error loading resource " + resourceId);
        }
	}

}