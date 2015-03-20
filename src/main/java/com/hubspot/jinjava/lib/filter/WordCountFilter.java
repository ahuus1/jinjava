package com.hubspot.jinjava.lib.filter;

import com.hubspot.jinjava.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;

public class WordCountFilter implements Filter {

  private static final int UNICODE_CHARACTER_CLASS = 0x100;

  @Override
  public String getName() {
    return "wordcount";
  }

  @Override
  public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
    Matcher matcher = WORD_RE.matcher(Objects.toString(var, ""));

    int count = 0;
    
    while(matcher.find()) {
      count++;
    }
    
    return count;
  }

  private static final Pattern WORD_RE = Pattern.compile("\\w+", UNICODE_CHARACTER_CLASS | Pattern.MULTILINE);

}
