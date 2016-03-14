/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.graphaware.integration.es.plugin;

import io.searchbox.annotations.JestId;

/**
 *
 * @author ale
 */
public class JestMsgResult
{

  @JestId
  private String documentId;
  
  private String msg;
  
  public String getDocumentId()
  {
    return documentId;
  }
  public String getMsg()
  {
    return msg;
  }
  public void setMsg(String name)
  {
    this.msg = name;
  }
}
