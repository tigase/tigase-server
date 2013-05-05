package com.izforge.izpack.installer;

import java.util.Properties;

import com.izforge.izpack.util.VariableSubstitutor;


public class VariablesHelper {
	public String expand(String string_to_parse, Properties variables) {
        try
        {
            // Initialize the variable substitutor
            VariableSubstitutor vs = new VariableSubstitutor(variables);

            // Parses the info text
            string_to_parse = vs.substitute(string_to_parse, null);
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }
        return string_to_parse;
	}
	
	public String expand(String string_to_parse, AutomatedInstallData idata) {
		return expand(string_to_parse, idata.getVariables());
	}
}