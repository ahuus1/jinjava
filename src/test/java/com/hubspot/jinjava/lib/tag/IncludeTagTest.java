package com.hubspot.jinjava.lib.tag;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import com.hubspot.jinjava.util.StandardCharsets;

import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Splitter;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;


public class IncludeTagTest {

  Jinjava jinjava;
  
  @Before
  public void setup() {
    jinjava = new Jinjava();
  }
  
  @Test
  public void itAvoidsSimpleIncludeCycles() throws IOException {
    String result = jinjava.render(Resources.toString(Resources.getResource("tags/includetag/includes-self.jinja"), StandardCharsets.UTF_8), 
        new HashMap<String, Object>());
    assertThat(result).containsSequence("hello world", "hello world");
  }
  
  @Test
  public void itAvoidsNestedIncludeCycles() throws IOException {
    String result = jinjava.render(Resources.toString(Resources.getResource("tags/includetag/a-includes-b.jinja"), StandardCharsets.UTF_8), 
        new HashMap<String, Object>());
    assertThat(result).containsSequence("A", "B");
  }
  
  @Test
  public void itAllowsSameIncludeMultipleTimesInATemplate() throws IOException {
    String result = jinjava.render(Resources.toString(Resources.getResource("tags/includetag/c-includes-d-twice.jinja"), StandardCharsets.UTF_8), 
        new HashMap<String, Object>());
    assertThat(Splitter.on('\n').omitEmptyStrings().trimResults().split(result)).containsExactly("hello", "hello");
  }
  
}
