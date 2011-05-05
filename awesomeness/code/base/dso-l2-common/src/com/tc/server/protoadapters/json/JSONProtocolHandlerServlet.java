/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server.protoadapters.json;

import org.codehaus.jackson.map.ObjectMapper;

import com.tc.server.protoadapters.GlobalStorageManager;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class JSONProtocolHandlerServlet extends HttpServlet {

  public static final String   GLOBAL_STORAGE_MGR_ATTRIBUTE = JSONProtocolHandlerServlet.class.getName()
                                                              + ".globalStorageManager";

  private GlobalStorageManager globalStorageManager;

  @Override
  public void init() {
    globalStorageManager = (GlobalStorageManager) getServletContext().getAttribute(GLOBAL_STORAGE_MGR_ATTRIBUTE);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ServletOutputStream out = response.getOutputStream();
    out.println("Hello - " + new Date());
    String msg = request.getParameter("msg");
    if (null == msg || "".equals(msg)) {
      out.println("Unknown message: " + msg);
    }
    ObjectMapper mapper = new ObjectMapper();
    Map<Object, Object> msgMap = null;
    Exception error = null;
    try {
      msgMap = mapper.readValue(msg, Map.class);
    } catch (Exception e) {
      error = e;
    }
    Map<Object, Object> result = null;
    if (error != null) {
      result = GlobalStorageManager.createErrorResponseFor("There were some error parsing your message - "
                                                           + error.getMessage());
    } else {
      result = globalStorageManager.processMessage(msgMap);
    }
    mapper.writeValue(out, result);
    response.flushBuffer();
  }

  public static void main(String[] args) throws Exception {
    new JSONProtocolHandlerServlet().run();
  }

  private void run() throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    Map readValue = mapper
        .readValue("{\"operation\": \"GET\", \"key\": \"key1\", \"value\": {\"one\": 1, \"two\": 2, \"boolean\": false, \"array\":[1, 2, 3, 4, 5]}}",
                   Map.class);
    System.out.println(GlobalStorageManager.toString(readValue));

    mapper.writeValue(System.out, readValue);
  }
}
