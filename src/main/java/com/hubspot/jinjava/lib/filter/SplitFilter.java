package com.hubspot.jinjava.lib.filter;

import com.hubspot.jinjava.util.Objects;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;

/**
 * split(separator=' ', limit=0)
 * 
 * Splits the input string into a list on the given separator
 * 
 * separator: defaults to space
 * limit: defaults to 0, limits resulting list by putting remainder of string into last list item
 * 
 * @author jstehler
 */
public class SplitFilter implements Filter {

  @Override
  public String getName() {
    return "split";
  }

  @Override
  public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
    Splitter splitter;
    
    if(args.length > 0) {
      splitter = Splitter.on(args[0]);
    }
    else {
      splitter = Splitter.on(CharMatcher.WHITESPACE);
    }
    
    if(args.length > 1) {
      int limit = NumberUtils.toInt(args[1], 0);
      if(limit > 0) {
        splitter = splitter.limit(limit);
      }
    }
    
    return Lists.newArrayList(splitter.omitEmptyStrings().trimResults().split(Objects.toString(var, "")));
  }

}
