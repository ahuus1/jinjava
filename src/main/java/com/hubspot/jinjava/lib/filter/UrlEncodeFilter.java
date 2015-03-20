package com.hubspot.jinjava.lib.filter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.hubspot.jinjava.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Throwables;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;

public class UrlEncodeFilter implements Filter {

  @Override
  public String getName() {
    return "urlencode";
  }

  @Override
  public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
    if(var == null && args.length == 0) {
      return "";
    }
    
    if(var != null) {
      if(Map.class.isAssignableFrom(var.getClass())) {
        @SuppressWarnings("unchecked")
        Map<Object, Object> dict = (Map<Object, Object>) var;
        
        List<String> paramPairs = new ArrayList<String>();

        for(Map.Entry<Object, Object> param : dict.entrySet()) {
          StringBuilder paramPair = new StringBuilder();
          paramPair.append(urlEncode(Objects.toString(param.getKey())));
          paramPair.append("=");
          paramPair.append(urlEncode(Objects.toString(param.getValue())));
          
          paramPairs.add(paramPair.toString());
        }
        
        return StringUtils.join(paramPairs, "&");
      }
      
      return urlEncode(var.toString());
    }

    return urlEncode(args[0]);
  }

  private String urlEncode(String s) {
    try {
      return URLEncoder.encode(s, "UTF-8");
    }
    catch(UnsupportedEncodingException e) {
      throw Throwables.propagate(e);
    }
  }
  
}
