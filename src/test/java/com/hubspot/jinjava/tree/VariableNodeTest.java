package com.hubspot.jinjava.tree;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.jinjava.util.StandardCharsets;


import org.junit.Before;
import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.interpret.Context;
import com.hubspot.jinjava.interpret.JinjavaInterpreter;
import com.hubspot.jinjava.parse.TokenParser;


public class VariableNodeTest {

  private Context context;
  private JinjavaInterpreter interpreter;
  
  @Before
  public void setup() {
    Jinjava jinjava = new Jinjava();
    context = new Context();
    interpreter = new JinjavaInterpreter(jinjava, context, jinjava.getGlobalConfig());
  }
  
  @Test
  public void itRendersResultAsTemplateWhenContainingVarBlocks() throws Exception {
    context.put("myvar", "hello {{ place }}");
    context.put("place", "world");
    
    VariableNode node = fixture("simplevar");
    assertThat(node.render(interpreter)).isEqualTo("hello world");
  }
  
  @Test
  public void itAvoidsInfiniteRecursionWhenVarsContainBraceBlocks() throws Exception {
    context.put("myvar", "hello {{ place }}");
    context.put("place", "{{ place }}");
    
    VariableNode node = fixture("simplevar");
    assertThat(node.render(interpreter)).isEqualTo("hello {{ place }}");
  }
  
  @Test
  public void valueExprWithOr() throws Exception {
    context.put("a", "foo");
    context.put("b", "bar");
    context.put("c", "");
    context.put("d", 0);

    assertThat(val("{{ a or b }}")).isEqualTo("foo");
    assertThat(val("{{ c or a }}")).isEqualTo("foo");
    assertThat(val("{{ d or b }}")).isEqualTo("bar");
  }
  
  private String val(String jinja) {
    return parse(jinja).render(interpreter);
  }
  
  private VariableNode parse(String jinja) {
    return (VariableNode) TreeParser.parseTree(new TokenParser(interpreter, jinja)).getChildren().getFirst();
  }
  
  private VariableNode fixture(String name) {
    try {
      return parse(Resources.toString(Resources.getResource(String.format("varblocks/%s.html", name)), StandardCharsets.UTF_8));
    }
    catch(Exception e) {
      throw Throwables.propagate(e);
    }
  }
  
}
