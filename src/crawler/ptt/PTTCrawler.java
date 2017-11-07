package crawler.ptt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.RefreshHandler;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomAttr;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.DomText;
import com.gargoylesoftware.htmlunit.html.FrameWindow;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlDivision;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlHiddenInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlMeta;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

/**
 * 
 * @author Ming_Wu
 *
 */
public class PTTCrawler {

	/**
	 * 
	 * @param boardName
	 * @param pageLimit
	 * @param forceTitleList
	 * @param disableTitleList
	 * @param printInfoFlag
	 */
	public static void processBoard(String boardName, int pageLimit, 
			ArrayList<String> forceTitleList, ArrayList<String> disableTitleList, boolean printInfoFlag) {
		PTTCrawler JSoupPTTData = new PTTCrawler();
		try {
			String url1 = "https://www.ptt.cc";
			String url2 = "/bbs/";
			String url3 = "/index";			
			String url4 = ".html";
			
			WebClient webClient = new WebClient(BrowserVersion.CHROME);
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			ThreadedRefreshHandler rh = new ThreadedRefreshHandler();			  
		    webClient.setRefreshHandler(rh);
			System.out.println(boardName);
			StringBuilder sb = new StringBuilder();
			String pageNumber = "";
//			String pageUrl = "";
			int newestPageNum = 0;
			int crawlNum = 0;
			boolean crawlFlag = true;
			boolean newestPageNumFlag = true;
			while(crawlFlag) {
				boolean recordFlag = true;
				crawlNum++;
				if ((crawlNum-pageLimit) == 0) break;
				
				String boardUrl = url1 + url2 + boardName + url3 + pageNumber + url4;
				if (printInfoFlag) System.out.println(boardUrl);
		        HtmlPage htmlPage = webClient.getPage(boardUrl);
				String over18Url = "https://www.ptt.cc/ask/over18?from=%2Fbbs%2FGossiping%2Findex.html";
		        if (htmlPage.getBaseURL().toString().equals(over18Url)) {		        	
			        HtmlButton button = htmlPage.getElementByName("yes");
			        htmlPage = button.click();
		        }
		        
		        boolean breakFlag = false;
				for (HtmlElement e1: htmlPage.getHtmlElementDescendants()) {
					if (breakFlag) break;
					breakFlag = true;
					for (HtmlElement e2: e1.getElementsByAttribute("div", "class", "r-ent")) {
						for (HtmlElement e3: e2.getElementsByAttribute("div", "class", "title")) {
							for (DomElement child: e3.getChildElements()) {
								DomAttr attributeNode = child.getAttributeNode("href");
								String text = child.asText();
								if (!forceTitleList.isEmpty()) {
									recordFlag = false;
									for (String forceTag: forceTitleList) {
										if (text.contains(forceTag)) {
											recordFlag = true;
										}
									}
								}
								if (!disableTitleList.isEmpty()) {
									for (String disableTag: disableTitleList) {
										if (text.contains(disableTag)) {
											recordFlag = false;
										}
									}
								}
								if (recordFlag) {
									if (printInfoFlag) System.out.println(text + " " + attributeNode.getValue());
									sb.append(text + " " + attributeNode.getValue() + "\n");
								}
								/*
								 * 爬文章
								 * [公告]
								 */
								HtmlPage artPage = webClient.getPage(url1 + attributeNode.getValue());
								outloop:
								for (HtmlElement e4: artPage.getHtmlElementDescendants()) {
									for (HtmlElement e5: e4.getElementsByAttribute("div", "id", "main-content")) {
										if (recordFlag) sb.append(e5.asText()+"\n\n");
										break outloop;						
									}
								}
							}
						}
					}
				}
				breakFlag = false;
				for (HtmlElement htmlElementDescendant : htmlPage.getHtmlElementDescendants()) {
					if (breakFlag) break;
					breakFlag = true;
					for (HtmlElement e : htmlElementDescendant.getElementsByAttribute("div", "class", "btn-group btn-group-paging")) {
						for (DomElement child : e.getChildElements()) {
							DomAttr attributeNode = child.getAttributeNode("href");

							String text = child.asText();
							if (text.endsWith("上頁")) {
								if (attributeNode != null) {
//									pageUrl = attributeNode.getValue();
									pageNumber = attributeNode.getValue().replace(url2 + boardName + url3, "").replace(url4, "");
									if (newestPageNumFlag) {
										newestPageNum = Integer.parseInt(pageNumber);
//										System.out.println(pageNumber);
										newestPageNumFlag = false;
									}
								} else {
									crawlFlag = false;
								}
							}
						}
					}
				}
			}
			JSoupPTTData.insertJSONtoFile(boardName, sb.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static void processHotBoard() {
		PTTCrawler JSoupPTTData = new PTTCrawler();
		try {
			String hotboardUrl = "https://www.ptt.cc/hotboard.html";
			Document hotboardDoc = Jsoup.connect(hotboardUrl).userAgent("Chrome").get();
			ArrayList<String> visitingList = new ArrayList<String>();
			Elements visitings = hotboardDoc.select("td[width=100]");
			for (Element visiting: visitings) {
				visitingList.add(visiting.text());
			}
			ArrayList<String> boardList = new ArrayList<String>();
			Elements boards = hotboardDoc.select("td[width=120]");
			for (int i = 0 ; i < boards.size(); i++) {
				if (i%2 == 0) {
					Element board = boards.get(i);
					Element boardChild = board.child(0);
					String absUrl = boardChild.absUrl("href");
					boardList.add(board.text());
				}
			}			
			ArrayList<String> boardSubList = new ArrayList<String>();
			Elements boardSubs = hotboardDoc.select("td[width=400]");
			for (Element boardSub: boardSubs) {
				boardSubList.add(boardSub.text());
			}
			
			String url1 = "https://www.ptt.cc";
			String url2 = "/bbs/";
			String url3 = "/index";			
			String url4 = ".html";
			
			WebClient webClient = new WebClient(BrowserVersion.CHROME);
			webClient.getOptions().setCssEnabled(false);
			webClient.getOptions().setJavaScriptEnabled(false);
			ThreadedRefreshHandler rh = new ThreadedRefreshHandler();			  
		    webClient.setRefreshHandler(rh);
//		    visitingList = new ArrayList<String>(Arrays.asList("GetMarry"));
			for (int i = 0; i < visitingList.size(); i++) {
				try {
					String boardName = boardList.get(i);
					System.out.println(boardName);
					StringBuilder sb = new StringBuilder();
					String pageNumber = "";
					String pageUrl = "";
					int newestPageNum = 0;
					int pageNum = 0;
					int crawlNum = 0;
					int stopPageCounter = 100;
					boolean crawlFlag = true;
					boolean printFlag = true;
					while(crawlFlag) {
						crawlNum++;
						if ((crawlNum-stopPageCounter) == 0) break;
						
//						String boardUrl = url1 + url2 + boardName + url3 + pageNumber + url4;
						String boardUrl = url1 + pageUrl;
				        HtmlPage htmlPage = webClient.getPage(boardUrl);
						String over18Url = "https://www.ptt.cc/ask/over18?from=%2Fbbs%2FGossiping%2Findex.html";
				        if (htmlPage.getBaseURL().toString().equals(over18Url)) {		        	
					        HtmlButton button = htmlPage.getElementByName("yes");
					        htmlPage = button.click();
//							System.out.println(visitingList.get(i) + " " + boardList.get(i) + " " + boardSubList.get(i) + " " + boardUrl);
				        }
//				        String pageString = htmlPage.getWebResponse().getContentAsString();
//				        Document doc= Jsoup.parse(pageString);
//				        Elements board = doc.select("a[href^=/bbs]");		        
//				        for (Element b: board) {
//				        	b.text();
//				        }
				        boolean breakFlag = false;
						for (HtmlElement e1: htmlPage.getHtmlElementDescendants()) {
							if (breakFlag) break;
							breakFlag = true;
							for (HtmlElement e2: e1.getElementsByAttribute("div", "class", "r-ent")) {
//								for (HtmlElement e3: e2.getElementsByTagName("span")) {
//									sb.append(e3.asText() + " ");
//								}
								for (HtmlElement e3: e2.getElementsByAttribute("div", "class", "title")) {
									for (DomElement child: e3.getChildElements()) {
										DomAttr attributeNode = child.getAttributeNode("href");
										String text = child.asText();
										sb.append(text + " " + attributeNode.getValue() + "\n");
										/*
										 * 爬文章
										 */
										HtmlPage artPage = webClient.getPage(url1 + attributeNode.getValue());
										outloop:
										for (HtmlElement e4: artPage.getHtmlElementDescendants()) {
											for (HtmlElement e5: e4.getElementsByAttribute("div", "id", "main-content")) {
												sb.append(e5.asText()+"\n\n");
												break outloop;
//												List<HtmlElement> htmlElementList = e5.getElementsByAttribute("div", "class", "article-metaline");
//												for (int j = 0; j < htmlElementList.size(); j++) {
//													HtmlElement e6 = htmlElementList.get(j);
//													for (HtmlElement e7: e6.getElementsByAttribute("span", "class", "article-meta-tag")) {
//														sb.append(e7.asText()+": ");
//													}
//													for (HtmlElement e7: e6.getElementsByAttribute("span", "class", "article-meta-value")) {
//														sb.append(e7.asText()+"\n");
//													}
//													
//													if (j == 2) {
//														/*
//														 * 推文也在這邊爬
//														 * </div><div class="push"><span class="f1 hl push-tag">→ </span>
//														 * <span class="f3 hl push-userid">OyAlbert</span>
//														 * <span class="f3 push-content">: 用迴紋針夾起來</span>
//														 * <span class="push-ipdatetime"> 11/11 17:27</span>
//														 */
//														boolean queryDataFlag = true;
//														DomNode domNode = e6.getNextSibling();
//														String textContent = domNode.getTextContent();
//														sb.append(textContent);
//														int counter = 0;
//														while (queryDataFlag) {
//															DomNode domNode1 = domNode.getNextSibling();														
//															if (domNode1 == null || domNode1.asText().contains("(ptt.cc), 來自:")) {
//																queryDataFlag = false;
//																break outloop;
//															}
//															
//															String textContent1 = domNode1.getTextContent();
//															sb.append(textContent1);
//															domNode = domNode1;
//
//															counter++;
//															if (counter % 10 == 0) {
//																System.out.println(textContent1);
//															}
//														}
//													}													
//												}												
											}
										}
//										System.out.println(text + " " + attributeNode.getValue());
									}
								}
//								System.out.println(sb);
//								sb = new StringBuilder();
							}
						}
						breakFlag = false;
				        for (HtmlElement htmlElementDescendant: htmlPage.getHtmlElementDescendants()) {
				        	if (breakFlag) break;
							breakFlag = true;
				        	 for (HtmlElement e: htmlElementDescendant.getElementsByAttribute("div", "class", "btn-group btn-group-paging")) {
				        		 for (DomElement child: e.getChildElements()) {		        			 
				        			 DomAttr attributeNode = child.getAttributeNode("href");
				        			 
				        			 String text = child.asText();
				        			 if (text.endsWith("上頁")) {
				        				 if (attributeNode != null) {
				        					 pageUrl = attributeNode.getValue();
				        					 pageNumber = attributeNode.getValue().replace(url2+boardName+url3, "").replace(url4, "");
				        					 if (printFlag) {
				        						 newestPageNum = Integer.parseInt(pageNumber);
				        						 System.out.println(pageNumber);
				        						 printFlag = false;
				        					 }
				        				 } else {
				        					 crawlFlag = false;
				        				 }
				        			 }
					        	}
				        	 }
				        }
					}
	//	        	List<HtmlAnchor> anchors = htmlPage.getAnchors();
	//		        for (HtmlAnchor anchor: anchors) {
	//		        	DomNode parentNode = anchor.getParentNode();
	//		        	NamedNodeMap attributes = anchor.getAttributes();
	//		        	
	//		        	System.out.println(anchor.asText() + " " + anchor.getHrefAttribute());
	//		        }
					
	//				System.out.println("================================================================================");
					JSoupPTTData.insertJSONtoFile(boardName, sb.toString());
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("[PTTCrawler] [ERROR] Crawl board error: "+ boardList.get(i));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void insertJSONtoFile(String boardName, String string) {
		BufferedWriter fw = null;
		try {
			File file = new File("D://"+boardName+".txt");
			fw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), "UTF-8")); // 指點編碼格式，以免讀取時中文字符異常
			fw.write(string);
			fw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		String boardName = "NBA";
		int pageLimit = 10;
		ArrayList<String> forceTagList = new ArrayList<String>();
		ArrayList<String> disableTagList = new ArrayList<String>(Arrays.asList("[BOX ]","[公告]"));
		boolean printInfoFlag = true;
		
		PTTCrawler.processBoard(boardName, pageLimit, forceTagList, disableTagList, printInfoFlag);
//		PTTCrawler.processHotBoard();
	}	
}
