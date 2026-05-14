package com.sandec.mdfx.impl;

import com.vladsch.flexmark.ast.*;
import com.vladsch.flexmark.ext.attributes.AttributeNode;
import com.vladsch.flexmark.ext.attributes.AttributesExtension;
import com.vladsch.flexmark.ext.attributes.AttributesNode;
import com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListItem;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Pair;
import com.vladsch.flexmark.util.ast.*;
import com.vladsch.flexmark.ext.tables.*;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.misc.Extension;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GPMarkdownViewBuilder extends VBox {
  String mdString;

  GPMarkdownView parent;

  final static String ITALICE_CLASS_NAME = "markdown-italic";
  final static String BOLD_CLASS_NAME = "markdown-bold";
  final static String STRIKETHROUGH_CLASS_NAME = "markdown-strikethrough";

  List<String> elemStyleClass = new LinkedList<String>();

  List<Consumer<Pair<Node,String>>> elemFunctions = new LinkedList<Consumer<Pair<Node,String>>>();

  Boolean nodePerWord = false;

  List<String> styles = new LinkedList<String>();

  VBox root = new VBox();

  GridPane grid = null;
  int gridx = 0;
  int gridy = 0;
  TextFlow flow = null;

  boolean isListOrdered = false;
  int orderedListCounter = 0;

  int[] currentChapter = new int[6];

  private static final Map<String, String> HTML_ENTITY_TO_UNICODE = new HashMap<>();
  static {
    HTML_ENTITY_TO_UNICODE.put("&amp;", "\u0026");
    HTML_ENTITY_TO_UNICODE.put("&lt;", "\u003C");
    HTML_ENTITY_TO_UNICODE.put("&gt;", "\u003E");
    HTML_ENTITY_TO_UNICODE.put("&quot;", "\"");
    HTML_ENTITY_TO_UNICODE.put("&apos;", "'");
    HTML_ENTITY_TO_UNICODE.put("&nbsp;", "\u00A0");
    HTML_ENTITY_TO_UNICODE.put("&ndash;", "\u2013");
    HTML_ENTITY_TO_UNICODE.put("&mdash;", "\u2014");
    HTML_ENTITY_TO_UNICODE.put("&lsquo;", "\u2018");
    HTML_ENTITY_TO_UNICODE.put("&rsquo;", "\u2019");
    HTML_ENTITY_TO_UNICODE.put("&ldquo;", "\u201C");
    HTML_ENTITY_TO_UNICODE.put("&rdquo;", "\u201D");
    HTML_ENTITY_TO_UNICODE.put("&bull;", "\u2022");
    HTML_ENTITY_TO_UNICODE.put("&hellip;", "\u2026");
    HTML_ENTITY_TO_UNICODE.put("&trade;", "\u2122");
    HTML_ENTITY_TO_UNICODE.put("&copy;", "\u00A9");
    HTML_ENTITY_TO_UNICODE.put("&reg;", "\u00AE");
    HTML_ENTITY_TO_UNICODE.put("&euro;", "\u20AC");
    HTML_ENTITY_TO_UNICODE.put("&pound;", "\u00A3");
    HTML_ENTITY_TO_UNICODE.put("&yen;", "\u00A5");
    HTML_ENTITY_TO_UNICODE.put("&cent;", "\u00A2");
    HTML_ENTITY_TO_UNICODE.put("&laquo;", "\u00AB");
    HTML_ENTITY_TO_UNICODE.put("&raquo;", "\u00BB");
    HTML_ENTITY_TO_UNICODE.put("&larr;", "\u2190");
    HTML_ENTITY_TO_UNICODE.put("&rarr;", "\u2192");
    HTML_ENTITY_TO_UNICODE.put("&uarr;", "\u2191");
    HTML_ENTITY_TO_UNICODE.put("&darr;", "\u2193");
    HTML_ENTITY_TO_UNICODE.put("&times;", "\u00D7");
    HTML_ENTITY_TO_UNICODE.put("&divide;", "\u00F7");
    HTML_ENTITY_TO_UNICODE.put("&minus;", "\u2212");
    HTML_ENTITY_TO_UNICODE.put("&plusmn;", "\u00B1");
    HTML_ENTITY_TO_UNICODE.put("&deg;", "\u00B0");
    HTML_ENTITY_TO_UNICODE.put("&micro;", "\u00B5");
    HTML_ENTITY_TO_UNICODE.put("&para;", "\u00B6");
    HTML_ENTITY_TO_UNICODE.put("&sect;", "\u00A7");
    HTML_ENTITY_TO_UNICODE.put("&frac14;", "\u00BC");
    HTML_ENTITY_TO_UNICODE.put("&frac12;", "\u00BD");
    HTML_ENTITY_TO_UNICODE.put("&frac34;", "\u00BE");
    HTML_ENTITY_TO_UNICODE.put("&iquest;", "\u00BF");
    HTML_ENTITY_TO_UNICODE.put("&iexcl;", "\u00A1");
    HTML_ENTITY_TO_UNICODE.put("&lsaquo;", "\u2039");
    HTML_ENTITY_TO_UNICODE.put("&rsaquo;", "\u203A");
    HTML_ENTITY_TO_UNICODE.put("&infin;", "\u221E");
    HTML_ENTITY_TO_UNICODE.put("&ne;", "\u2260");
    HTML_ENTITY_TO_UNICODE.put("&le;", "\u2264");
    HTML_ENTITY_TO_UNICODE.put("&ge;", "\u2265");
    HTML_ENTITY_TO_UNICODE.put("&sum;", "\u2211");
    HTML_ENTITY_TO_UNICODE.put("&prod;", "\u220F");
    HTML_ENTITY_TO_UNICODE.put("&radic;", "\u221A");
    HTML_ENTITY_TO_UNICODE.put("&delta;", "\u03B4");
    HTML_ENTITY_TO_UNICODE.put("&Delta;", "\u0394");
    HTML_ENTITY_TO_UNICODE.put("&pi;", "\u03C0");
    HTML_ENTITY_TO_UNICODE.put("&alpha;", "\u03B1");
    HTML_ENTITY_TO_UNICODE.put("&beta;", "\u03B2");
    HTML_ENTITY_TO_UNICODE.put("&gamma;", "\u03B3");
    HTML_ENTITY_TO_UNICODE.put("&omega;", "\u03C9");
    HTML_ENTITY_TO_UNICODE.put("&Omega;", "\u03A9");
    HTML_ENTITY_TO_UNICODE.put("&lambda;", "\u03BB");
    HTML_ENTITY_TO_UNICODE.put("&sigma;", "\u03C3");
    HTML_ENTITY_TO_UNICODE.put("&theta;", "\u03B8");
    HTML_ENTITY_TO_UNICODE.put("&phi;", "\u03C6");
    HTML_ENTITY_TO_UNICODE.put("&psi;", "\u03C8");
    HTML_ENTITY_TO_UNICODE.put("&chi;", "\u03C7");
    HTML_ENTITY_TO_UNICODE.put("&mu;", "\u03BC");
  }

  static String resolveHtmlEntity(String entity) {
    String unicode = HTML_ENTITY_TO_UNICODE.get(entity);
    if (unicode != null) {
      return unicode;
    }
    // Handle numeric entities: &#123; or &#x1F4A9;
    if (entity.startsWith("&#") && entity.endsWith(";")) {
      try {
        int codePoint;
        String inner = entity.substring(2, entity.length() - 1);
        if (inner.startsWith("x") || inner.startsWith("X")) {
          codePoint = Integer.parseInt(inner.substring(1), 16);
        } else {
          codePoint = Integer.parseInt(inner);
        }
        return new String(Character.toChars(codePoint));
      } catch (NumberFormatException e) {
        // fall through to return original
      }
    }
    return entity;
  }

  public boolean shouldShowContent() {
    return parent.showChapter(currentChapter);
  }

  public void newParagraph() {
    TextFlow newFlow = new TextFlow();
    newFlow.getStyleClass().add("markdown-normal-flow");
    root.getChildren().add(newFlow);
    flow = newFlow;
  }

  public GPMarkdownViewBuilder(GPMarkdownView parent, String mdstring) {
    this.parent = parent;

    root.getStyleClass().add("markdown-paragraph-list");
    root.setFillWidth(true);

    LinkedList<Extension> extensions = new LinkedList<>();
    extensions.add(TablesExtension.create());
    //extensions.add(AttributesExtension.create());
    extensions.add(StrikethroughExtension.create());
    extensions.add(TaskListExtension.create());
    Parser parser = Parser.builder().extensions(extensions).build();

    Document node = parser.parse(mdstring);

    new MDParser(node).visitor.visitChildren(node);

    this.getChildren().add(root);
  }


  class MDParser {

    Document document;

    MDParser(Document document) {
      this.document = document;
    }

    NodeVisitor visitor = new NodeVisitor(
            //new VisitHandler<>(com.vladsch.flexmark.ast.Node.class, this::visit),
            new VisitHandler<>(Code.class, this::visit),
            new VisitHandler<>(BlockQuote.class, this::visit),
            //new VisitHandler<>(Quotes.class, this::visit),
            new VisitHandler<>(Block.class, this::visit),
            new VisitHandler<>(Document.class, this::visit),
            new VisitHandler<>(Emphasis.class, this::visit),
            new VisitHandler<>(StrongEmphasis.class, this::visit),
            new VisitHandler<>(FencedCodeBlock.class, this::visit),
            new VisitHandler<>(SoftLineBreak.class, this::visit),
            new VisitHandler<>(HardLineBreak.class, this::visit),
            new VisitHandler<>(Heading.class, this::visit),
            new VisitHandler<>(ListItem.class, this::visit),
            new VisitHandler<>(BulletListItem.class, this::visit),
            new VisitHandler<>(OrderedListItem.class, this::visit),
            new VisitHandler<>(TaskListItem.class, this::visit),
            new VisitHandler<>(BulletList.class, this::visit),
            new VisitHandler<>(OrderedList.class, this::visit),
            new VisitHandler<>(Paragraph.class, this::visit),
            new VisitHandler<>(com.vladsch.flexmark.ast.Image.class, this::visit),
            new VisitHandler<>(Link.class, this::visit),
            new VisitHandler<>(com.vladsch.flexmark.ast.TextBase.class, this::visit),
            new VisitHandler<>(com.vladsch.flexmark.ast.Text.class, this::visit),
            new VisitHandler<>(com.vladsch.flexmark.ext.gfm.strikethrough.Strikethrough.class, this::visit),
            new VisitHandler<>(TableHead.class, this::visit),
            new VisitHandler<>(TableBody.class, this::visit),
            new VisitHandler<>(TableRow.class, this::visit),
            new VisitHandler<>(TableCell.class, this::visit),
            new VisitHandler<>(HtmlEntity.class, this::visit)
    );

    public void visit(HtmlEntity htmlEntity) {
      String entity = htmlEntity.getChars().normalizeEOL();
      String resolved = resolveHtmlEntity(entity);
      addText(resolved, resolved);
    }

    public void visit(Code code) {

      Label label = new Label(code.getText().normalizeEOL());
      label.getStyleClass().add("markdown-code");

      Region bgr1 = new Region();
      bgr1.setManaged(false);
      bgr1.getStyleClass().add("markdown-code-background");
      label.boundsInParentProperty().addListener((p, oldV, newV) -> {
        bgr1.setTranslateX(newV.getMinX() + 2);
        bgr1.setTranslateY(newV.getMinY() - 2);
        bgr1.resize(newV.getWidth() - 4, newV.getHeight() + 4);
      });

      flow.getChildren().add(bgr1);
      flow.getChildren().add(label);

      //visitor.visitChildren(code);
    }

    public void visit(BlockQuote customBlock) {
      VBox oldRoot = root;
      root = new VBox();
      root.getStyleClass().add("markdown-normal-block-quote");
      oldRoot.getChildren().add(root);

      visitor.visitChildren(customBlock);

      root = oldRoot;
      newParagraph();
    }

    public void visit(Block customBlock) {
      flow.getChildren().add(new Text("\n\n"));
      visitor.visitChildren(customBlock);
    }


    public void visit(Document document) {
      visitor.visitChildren(document);
    }

    public void visit(Emphasis emphasis) {
      elemStyleClass.add(ITALICE_CLASS_NAME);
      visitor.visitChildren(emphasis);
      elemStyleClass.remove(ITALICE_CLASS_NAME);
    }

    public void visit(StrongEmphasis strongEmphasis) {
      elemStyleClass.add(BOLD_CLASS_NAME);
      visitor.visitChildren(strongEmphasis);
      elemStyleClass.remove(BOLD_CLASS_NAME);
    }

    public void visit(Strikethrough strikethrough) {
      elemStyleClass.add(STRIKETHROUGH_CLASS_NAME);
      visitor.visitChildren(strikethrough);
      elemStyleClass.remove(STRIKETHROUGH_CLASS_NAME);
    }


    public void visit(FencedCodeBlock fencedCodeBlock) {

      if(!shouldShowContent()) return;

      Label label = new Label(fencedCodeBlock.getContentChars().toString());
      label.getStyleClass().add("markdown-codeblock");
      VBox vbox = new VBox(label);
      vbox.getStyleClass().add("markdown-codeblock-box");

      root.getChildren().add(vbox);
      //flow.styleClass ::= "markdown-normal-flow"
      //flow <++ new Text("\n")
      //visitor.visitChildren(fencedCodeBlock);
    }

    public void visit(SoftLineBreak softLineBreak) {
      //flow <++ new Text("\n")
      addText(" ", "");
      visitor.visitChildren(softLineBreak);
    }

    public void visit(HardLineBreak hardLineBreak) {
      flow.getChildren().add(new Text("\n"));
      visitor.visitChildren(hardLineBreak);
    }


    public void visit(Heading heading) {

      if (heading.getLevel() == 1 || heading.getLevel() == 2) {
        currentChapter[heading.getLevel()] += 1;

        for(int i=heading.getLevel() + 1; i <= currentChapter.length - 1; i+=1) {
          currentChapter[i] = 0;
        };
      }

      if (shouldShowContent()) {
        newParagraph();

        flow.getStyleClass().add("markdown-heading-" + heading.getLevel());
        flow.getStyleClass().add("markdown-heading");

        visitor.visitChildren(heading);
      }
    }

    public void visitListItem(String text, com.vladsch.flexmark.util.ast.Node node) {
      VBox oldRoot = root;

      VBox newRoot = new VBox();
      newRoot.getStyleClass().add("markdown-vbox1");
      newRoot.getStyleClass().add("markdown-paragraph-list");
      newRoot.setFillWidth(true);

      orderedListCounter += 1;

      Label dot = new Label(text);
      dot.getStyleClass().add("markdown-listitem-dot");
      dot.getStyleClass().add("markdown-text");

      HBox hbox = new HBox();
      hbox.getStyleClass().add("markdown-hbox1");
      hbox.getChildren().add(dot);
      hbox.setAlignment(Pos.TOP_LEFT);
      HBox.setHgrow(newRoot,Priority.ALWAYS);
      newRoot.setPrefWidth(1.0); // This way, it doesn't take space from the "dot" label
      hbox.getChildren().add(newRoot);

      oldRoot.getChildren().add(hbox);

      root = newRoot;

      visitor.visitChildren(node);
      root = oldRoot;
    }

    public void visit(TaskListItem listItem) {
      if(!shouldShowContent()) return;
      String text = listItem.isItemDoneMarker() ? "☑" : "☐";
      visitListItem(text,listItem);
    }

    public void visit(ListItem listItem) {
      if(!shouldShowContent()) return;

      String text = isListOrdered ? (" " + (orderedListCounter+1) + ". ") : " • ";

      visitListItem(text,listItem);
    }

    public void visit(BulletList bulletList) {

      if(!shouldShowContent()) return;
      boolean prevIsListOrdered = isListOrdered;
      int prevListCounter = orderedListCounter;
      isListOrdered = false;
      VBox oldRoot = root;
      root = new VBox();
      oldRoot.getChildren().add(root);
      newParagraph();
      flow.getStyleClass().add("markdown-normal-flow");
      visitor.visitChildren(bulletList);
      isListOrdered = prevIsListOrdered;
      orderedListCounter = prevListCounter;
      root = oldRoot;
    }

    public void visit(OrderedList orderedList) {
      int previousCounter = orderedListCounter;
      boolean prevIsListOrdered = isListOrdered;
      int prevListCounter = orderedListCounter;
      orderedListCounter = 0;
      isListOrdered = true;
      VBox oldRoot = root;
      root = new VBox();
      oldRoot.getChildren().add(root);
      newParagraph();
      flow.getStyleClass().add("markdown-normal-flow");
      visitor.visitChildren(orderedList);
      orderedListCounter = previousCounter;
      isListOrdered = prevIsListOrdered;
      orderedListCounter = prevListCounter;
      root = oldRoot;
    }

    public void visit(Paragraph paragraph) {
        if(!shouldShowContent()) return;

      List<AttributesNode> atts = AttributesExtension.NODE_ATTRIBUTES.getFrom(document).get(paragraph);
      newParagraph();
      flow.getStyleClass().add("markdown-normal-flow");
      setAttrs(atts,true);
      visitor.visitChildren(paragraph);
      setAttrs(atts,false);
    }

    public void visit(com.vladsch.flexmark.ast.Image image) {
      String url = image.getUrl().toString();
      //System.out.println("imgUrl: " + image.getUrl());
      //System.out.println("img.getUrlContent: " + image.getUrlContent());
      //System.out.println("img.nodeName: " + image.getNodeName());
      Node node = parent.generateImage(url);
      addFeatures(node,"");
      flow.getChildren().add(node);
      //visitor.visitChildren(image);
    }

    public void visit(Link link) {

      LinkedList<Node> nodes = new LinkedList<>();

      Consumer<Pair<Node, String>> addProp = (pair) -> {
        Node node = pair.getKey();
        String txt = pair.getValue();
        nodes.add(node);

        node.getStyleClass().add("markdown-link");
        parent.setLink(node, link.getUrl().normalizeEOL(), txt);
      };
      Platform.runLater(() -> {
        BooleanProperty lastValue = new SimpleBooleanProperty(false);
        Runnable updateState = () -> {
          boolean isHover = false;
          for(Node node: nodes) {
            if(node.isHover()) {
              isHover = true;
            }
          }
          if (isHover != lastValue.get()) {
            lastValue.set(isHover);
            for(Node node: nodes) {
              if (isHover) {
                node.getStyleClass().add("markdown-link-hover");
              } else {
                node.getStyleClass().remove("markdown-link-hover");
              }

            };
          }

        };

        for(Node node: nodes) {
          node.hoverProperty().addListener((p, o, n) -> updateState.run());
        };
        updateState.run();
      });

      boolean oldNodePerWord = nodePerWord;
      nodePerWord = true;
      elemFunctions.add(addProp);
      visitor.visitChildren(link);
      nodePerWord = oldNodePerWord;
      elemFunctions.remove(addProp);
    }

    public void visit(com.vladsch.flexmark.ast.TextBase text) {
      List<AttributesNode> atts = AttributesExtension.NODE_ATTRIBUTES.getFrom(document).get(text);
      setAttrs(atts,true);
      visitor.visitChildren(text);
      setAttrs(atts,false);
    }

    public void visit(com.vladsch.flexmark.ast.Text text) {
      visitor.visitChildren(text);

      String wholeText = text.getChars().normalizeEOL();

      String[] textsSplitted = null;
      if (nodePerWord) {
        // split with " " but keep the " " in the array
        textsSplitted = text.getChars().normalizeEOL().split("(?<= )");
        // Combine splitted texts, which only contain a space:
        for(int i = 0; i <= textsSplitted.length - 1; i+=1) {
          if (textsSplitted[i].equals(" ")) {
            if(i == 0) {
              // add to next one, is possible
              if(i+1 <= textsSplitted.length - 1) {
                textsSplitted[i+1] = " " + textsSplitted[i+1];
                textsSplitted[i] = "";
              }
            } else {
              textsSplitted[i-1] = textsSplitted[i-1] + textsSplitted[i];
              textsSplitted[i] = "";
            }
          }
        }
      } else {
        textsSplitted = new String[1];
        textsSplitted[0] = text.getChars().normalizeEOL();
      }
      final String[] textsSplittedFinal = textsSplitted;

      for(int i = 0; i <= textsSplitted.length - 1; i+=1) {
        if(!textsSplittedFinal[i].equals("")) {
          addText(textsSplittedFinal[i], wholeText);
        }
      }
    }

    public void visit(TableHead customNode) {

      if(!shouldShowContent()) return;

      TextFlow oldFlow = flow;
      grid = new GridPane();
      grid.getStyleClass().add("markdown-table-table");
      gridx = 0;
      gridy = -1;
      root.getChildren().add(grid);

      visitor.visitChildren(customNode);

      for(int i=1; i <= gridx; i+=1) {
        ColumnConstraints constraint = new ColumnConstraints();
        if (i == gridx) {
          constraint.setPercentWidth(100.0 * (2.0 / (gridx + 1.0)));
        }
        grid.getColumnConstraints().add(constraint);
      }

      flow = oldFlow;
      newParagraph();
      //flow.styleClass ::= "markdown-normal-flow"
    }

    public void visit(TableBody customNode) {
      if(!shouldShowContent()) return;
      visitor.visitChildren(customNode);
      //} else if(customNode instanceof TableBlock) {
      //  super.visit(customNode);
    }

    public void visit(TableRow customNode) {
      if(customNode.getRowNumber() != 0) {
        gridx = 0;
        gridy += 1;
        visitor.visitChildren(customNode);
      }
    }

    public void visit(TableCell customNode) {
      TextFlow oldFlow = flow;
      flow = new TextFlow();
      flow.getStyleClass().add("markdown-normal-flow");
      TextFlow container = flow;
      //  println("grid: " + grid)
      //  javafx.scene.layout.GridPane.setHgrow(container,Priority.ALWAYS)
      flow.setPrefWidth(9999);
      flow.getStyleClass().add("markdown-table-cell");
      if (gridy == 0) {
        flow.getStyleClass().add("markdown-table-cell-top");
      }
      if (gridy % 2 == 0) {
        flow.getStyleClass().add("markdown-table-odd");
      } else {
        flow.getStyleClass().add("markdown-table-even");
      }
      grid.add(container, gridx, gridy);
      gridx += 1;
      visitor.visitChildren(customNode);
    }

    public void setAttrs(List<AttributesNode> atts, boolean add) {
      if(atts == null) return;

      List<com.vladsch.flexmark.util.ast.Node> atts2 = new LinkedList<>();
      for(AttributesNode att: atts) {
        for(com.vladsch.flexmark.util.ast.Node attChild: att.getChildren()) {
          atts2.add(attChild);
        }
      }


      List<AttributeNode> atts3 = (List<AttributeNode>) (Object) atts2;

      atts3.forEach(att -> {
        if(att.getName().toLowerCase().equals("style")) {
          if(add) styles.add(att.getValue().toString());
          else styles.remove(att.getValue().toString());
        }
        if(att.isClass()) {
          if(add) elemStyleClass.add(att.getValue().toString());
          else elemStyleClass.remove(att.getValue().toString());
        }
      });
    }

  }

  public void addText(String text, String wholeText) {
    if(!text.isEmpty()) {

      Text toAdd = new Text(text);

      toAdd.getStyleClass().add("markdown-text");

      addFeatures(toAdd,wholeText);

      flow.getChildren().add(toAdd);
    }
  }

  public void addFeatures(Node toAdd, String wholeText) {
    for(String elemStyleClass: elemStyleClass) {
      toAdd.getStyleClass().add(elemStyleClass);
    };
    for(Consumer<Pair<Node,String>> f: elemFunctions) {
      f.accept(new Pair(toAdd,wholeText));
    };
    if(!styles.isEmpty()) {
      String tmp = "";
      for(String style: styles) {
        tmp = tmp + style + ";";
      }
      toAdd.setStyle(toAdd.getStyle() + tmp);
    }
  }
}
