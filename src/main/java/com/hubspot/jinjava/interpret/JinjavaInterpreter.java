/**********************************************************************
Copyright (c) 2014 HubSpot Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 **********************************************************************/
package com.hubspot.jinjava.interpret;

import static com.hubspot.jinjava.util.Logging.ENGINE_LOG;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import javax.el.ELContext;
import javax.el.ExpressionFactory;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.hubspot.jinjava.Jinjava;
import com.hubspot.jinjava.JinjavaConfig;
import com.hubspot.jinjava.el.ExpressionResolver;
import com.hubspot.jinjava.el.JinjavaELContext;
import com.hubspot.jinjava.el.JinjavaInterpreterResolver;
import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import com.hubspot.jinjava.parse.TokenParser;
import com.hubspot.jinjava.tree.Node;
import com.hubspot.jinjava.tree.NodeList;
import com.hubspot.jinjava.tree.TreeParser;
import com.hubspot.jinjava.util.Objects;
import com.hubspot.jinjava.util.Variable;
import com.hubspot.jinjava.util.JinjavaPropertyNotResolvedException;
import com.hubspot.jinjava.util.WhitespaceUtils;

import de.odysseus.el.util.SimpleContext;

public class JinjavaInterpreter {

  private final Multimap<String, NodeList> blocks = ArrayListMultimap.create();
  private final LinkedList<Node> extendParentRoots = new LinkedList<Node>();
  
  private Context context;
  private final JinjavaConfig config;

  private final ExpressionResolver expressionResolver;
  private final Jinjava application;
  
  private int lineNumber = -1;
  private final List<TemplateError> errors = new LinkedList<TemplateError>();
  
  
  public JinjavaInterpreter(Jinjava application, Context context, JinjavaConfig renderConfig) {
    this.context = context;
    this.config = renderConfig;
    this.application = application;
    
    this.expressionResolver = new ExpressionResolver(this, createELContext());
  }

  public JinjavaInterpreter(JinjavaInterpreter orig) {
    this(orig.application, new Context(orig.context), orig.config);
  }
  
  public JinjavaConfig getConfiguration() {
    return config;
  }

  public void addExtendParentRoot(Node root) {
    extendParentRoots.add(root);
  }

  public void addBlock(String name, NodeList value) {
    blocks.put(name, value);
  }
  
  public void enterScope() {
    context = new Context(context); 
  }
  
  public void leaveScope() {
    Context parent = context.getParent();
    if(parent != null) {
      context = parent;
    }
  }
  
  public Node parse(String template) {
    return TreeParser.parseTree(new TokenParser(this, template));
  }
  
  public String renderString(String template) {
    Integer depth = (Integer) context.get("hs_render_depth", 0);
    if (depth == null) {
      depth = 0;
    }

    try {
      if (depth > config.getMaxRenderDepth()) {
        ENGINE_LOG.warn("Max render depth exceeded: {}", depth);
        return template;
      } else {
        context.put("hs_render_depth", depth + 1);
        return render(parse(template), false);
      }
    } finally {
      context.put("hs_render_depth", depth);
    }
  }
  
  public String render(Node root) {
    return render(root, true);
  }
  
  public String render(String template) {
    ENGINE_LOG.debug(template);
    return render(parse(template), true);
  }
  
  public String render(Node root, boolean processExtendRoots) {
    StringBuilder buff = new StringBuilder();

    for (Node node : root.getChildren()) {
      buff.append(node.render(this));
    } 

    // render all extend parents, keeping the last as the root output
    if(processExtendRoots) {
      while(!extendParentRoots.isEmpty()) {
        Node parentRoot = extendParentRoots.removeFirst();
        buff = new StringBuilder();
        
        for(Node node : parentRoot.getChildren()) {
          buff.append(node.render(this));
        }
      }
    }
    
    return resolveBlockStubs(buff);
  }
  
  String resolveBlockStubs(CharSequence content) {
    StringBuilder result = new StringBuilder(content.length() + 256);
    int pos = 0, start, end, stubStartLen = BLOCK_STUB_START.length();
    
    while((start = StringUtils.indexOf(content, BLOCK_STUB_START, pos)) != -1) {
      end = StringUtils.indexOf(content, BLOCK_STUB_END, start + stubStartLen);

      String blockName = content.subSequence(start + stubStartLen, end).toString();
      
      String blockValue = "";
      
      Collection<NodeList> blockChain = blocks.get(blockName);
      NodeList block = Iterables.getFirst(blockChain, null);
      
      if(block != null) {
        NodeList superBlock = Iterables.get(blockChain, 1, null);
        context.put("__superbl0ck__", superBlock);
        
        StringBuilder blockValueBuilder = new StringBuilder();
        
        for(Node child : block) {
          blockValueBuilder.append(child.render(this));
        }
        
        blockValue = resolveBlockStubs(blockValueBuilder);
        
        context.remove("__superbl0ck__");
      }
      
      result.append(content.subSequence(pos, start));
      result.append(blockValue);
      pos = end + 1;
    }
    
    result.append(content.subSequence(pos, content.length()));
    
    return result.toString();
  }
  
  /**
   * Resolve a variable from the interpreter context, returning null if not found. This method 
   * updates the template error accumulators when a variable is not found.
   * 
   * @param variable name of variable in context
   * @param lineNumber current line number, for error reporting
   * @return resolved value for variable
   */
  public Object retraceVariable(String variable, int lineNumber) {
    if(StringUtils.isBlank(variable)) {
      return "";
    }
    Variable var = new Variable(this, variable);
    String varName = var.getName();
    Object obj = context.get(varName);
    if (obj != null) {
      try {
      obj = var.resolve(obj);
      }
      catch(JinjavaPropertyNotResolvedException e) {
        addError(TemplateError.fromUnknownProperty(obj, variable, lineNumber));
      }
    }
    return obj;
  }

  /**
   * Resolve a variable into an object value. If given a string literal (e.g. 'foo' or "foo"), 
   * this method returns the literal unquoted. If the variable is undefined in the context,
   * this method returns the given variable string.
   * 
   * @param variable name of variable in context
   * @param lineNumber current line number, for error reporting
   * @return resolved value for variable
   */
  public Object resolveObject(String variable, int lineNumber) {
    if(StringUtils.isBlank(variable)) {
      return "";
    }
    if(WhitespaceUtils.isQuoted(variable)) {
      return WhitespaceUtils.unquote(variable);
    } else {
      Object val = retraceVariable(variable, lineNumber);
      if (val == null){
        return variable;
      }
      return val;
    }
  }

  /**
   * Resolve a variable into a string value. If given a string literal (e.g. 'foo' or "foo"), 
   * this method returns the literal unquoted. If the variable is undefined in the context,
   * this method returns the given variable string.
   * 
   * @param variable name of variable in context
   * @param lineNumber current line number, for error reporting
   * @return resolved value for variable
   */
  public String resolveString(String variable, int lineNumber) {
    return Objects.toString(resolveObject(variable, lineNumber), "");
  }

  public Context getContext() {
    return context;
  }

  public String getResource(String resource) throws IOException {
    return application.getResourceLocator().getString(resource, config.getCharset(), this);
  }
  
  public JinjavaConfig getConfig() {
    return config;
  }
  
  public ExpressionFactory getExpressionFactory() {
    return application.getExpressionFactory();
  }
  
  public Object resolveELExpression(String expr, int lineNumber) {
    return expressionResolver.resolve(expr, lineNumber);
  }

  private ELContext createELContext() {
    SimpleContext expContext = new JinjavaELContext(new JinjavaInterpreterResolver(this));

    for(ELFunctionDefinition fn : context.getAllFunctions()) {
      expContext.setFunction(fn.getNamespace(), fn.getLocalName(), fn.getMethod());
    }
    
    return expContext;
  }
  
  public void addError(TemplateError templateError) {
    this.errors.add(templateError);
  }
  
  public List<TemplateError> getErrors() {
    return errors;
  }
  
  public int getLineNumber() {
    return lineNumber;
  }
  public void setLineNumber(int lineNumber) {
    this.lineNumber = lineNumber;
  }
  
  
  private static final ThreadLocal<Stack<JinjavaInterpreter>> CURRENT_INTERPRETER = new ThreadLocal<Stack<JinjavaInterpreter>>() {
    protected java.util.Stack<JinjavaInterpreter> initialValue() {
      return new Stack<JinjavaInterpreter>();
    }
  };

  public static JinjavaInterpreter getCurrent() {
    if(CURRENT_INTERPRETER.get().isEmpty()) {
      return null;
    }
    
    return CURRENT_INTERPRETER.get().peek();
  }
  
  public static void pushCurrent(JinjavaInterpreter interpreter) {
    CURRENT_INTERPRETER.get().push(interpreter);
  }
  
  public static void popCurrent() {
    if(!CURRENT_INTERPRETER.get().isEmpty()) {
      CURRENT_INTERPRETER.get().pop();
    }
  }
  

  public static final String INSERT_FLAG = "'IS\"INSERT";
  
  public static final String BLOCK_STUB_START = "___bl0ck___~";
  public static final String BLOCK_STUB_END = "~";
  
}
