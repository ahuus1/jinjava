package com.hubspot.jinjava.lib.filter;

import static com.hubspot.jinjava.util.Logging.ENGINE_LOG;

import com.hubspot.jinjava.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeVisitor;

import com.hubspot.jinjava.interpret.JinjavaInterpreter;

public class TruncateHtmlFilter implements Filter {
  private static final int DEFAULT_TRUNCATE_LENGTH = 255;
  private static final String DEFAULT_END = "...";
  
  @Override
  public String getName() {
    return "truncatehtml";
  }

  @Override
  public Object filter(Object var, JinjavaInterpreter interpreter, String... args) {
    if(var instanceof String) {
      int length = DEFAULT_TRUNCATE_LENGTH;
      String ends = DEFAULT_END;
      
      if (args.length > 0) {
        try {
          length = Integer.valueOf(Objects.toString(args[0]));
        } catch (Exception e) {
          ENGINE_LOG.warn("truncatehtml(): error setting length for {}, using default {}", args[0], DEFAULT_TRUNCATE_LENGTH);
        }
      }

      if (args.length > 1) {
        ends = Objects.toString(args[1]);
      }

      Document dom = Jsoup.parseBodyFragment((String) var);
      ContentTruncatingNodeVisitor visitor = new ContentTruncatingNodeVisitor(length, ends);
      dom.select("body").traverse(visitor);
      dom.select(".__deleteme").remove();

      return dom.select("body").html();
    }
    
    return var;
  }

  private static class ContentTruncatingNodeVisitor implements NodeVisitor {
    private int maxTextLen;
    private int textLen;
    private String ending;
    
    public ContentTruncatingNodeVisitor(int maxTextLen, String ending) {
      this.maxTextLen = maxTextLen;
      this.ending = ending;
    }
    
    @Override
    public void head(Node node, int depth) {
      if(node instanceof TextNode) {
        TextNode text = (TextNode) node;
        String textContent = text.text();

        if(textLen >= maxTextLen){
          text.text("");
        }
        else if(textLen + textContent.length() > maxTextLen) {
          text.text(textContent.substring(0, (maxTextLen - textLen)) + ending);
          textLen = maxTextLen;
        }
        else {
          textLen += textContent.length();
        }
      }
    }

    @Override
    public void tail(Node node, int depth) {
      if(node instanceof Element) {
        Element el = (Element) node;
        if(StringUtils.isBlank(el.text())) {
          el.addClass("__deleteme");
        }
      }
    }
  }
}
