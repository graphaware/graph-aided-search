/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.integration.es.plugin.query;

import java.util.HashMap;
import java.util.Map;
import org.elasticsearch.script.ScriptService;

/**
 *
 * @author ale
 */
public class ScriptInfo
{
  protected final static ScriptInfo NO_SCRIPT_INFO = new ScriptInfo();

  private String script;

  private String lang;

  private ScriptService.ScriptType scriptType;

  private Map<String, Object> settings = new HashMap<>();

  private int reorderSize;
  private String neo4jHost;

  ScriptInfo()
  {
    // nothing
  }

  ScriptInfo(final String hostname)
  {
    this.neo4jHost = hostname;
  }

  public String getScript()
  {
    return script;
  }

  public String getLang()
  {
    return lang;
  }

  public ScriptService.ScriptType getScriptType()
  {
    return scriptType;
  }

  public Map<String, Object> getSettings()
  {
    return settings;
  }

  public void addSettings(String name, Object value)
  {
    settings.put(name, value);
  }

  public int getReorderSize()
  {
    return reorderSize;
  }

  @Override
  public String toString()
  {
    return "ScriptInfo [script=" + script + ", lang=" + lang
            + ", scriptType=" + scriptType + ", settings=" + settings
            + ", reorderSize=" + reorderSize + "]";
  }

  public String getNeo4jHost()
  {
    return neo4jHost;
  }
}
