package com.hubspot.jinjava.lib.filter;

import com.hubspot.jinjava.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;

public class CenterFilter implements Filter {

  @Override
  public String getName() {
    return "center";
  }

  @Override
  public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
    String str = Objects.toString(var, "");
    
    int size = 80;
    if(args.length > 0) {
      size = NumberUtils.toInt(args[0], 80);
    }
    
    return StringUtils.center(str, size);
  }

}
