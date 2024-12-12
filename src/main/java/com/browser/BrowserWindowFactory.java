package com.browser;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class BrowserWindowFactory implements ToolWindowFactory {

    private static final Logger LOG = Logger.getInstance(BrowserWindowFactory.class);
    private static final String BOOKMARKS_FILE = "bookmarks.json";
    private List<String> bookmarks = new ArrayList<>();
    private boolean black = false;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        toolWindow.setSplitMode(true, null);
        // 创建一个面板来容纳浏览器视图和其他控件
        JPanel panel = new JPanel(new BorderLayout());
        // 设置工具窗口的内容
        toolWindow.getComponent().add(panel);
        // 判断所处的IDEA环境是否支持JCEF
        if (!JBCefApp.isSupported()) {
            panel.add(new JLabel("当前环境不支持JCEF", SwingConstants.CENTER));
            return;
        }

        // 创建 JBCefBrowser
        JBCefBrowser jbCefBrowser = new JBCefBrowser();
        JBCefClient jbCefClient = jbCefBrowser.getJBCefClient();
        jbCefClient.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (black) {
                    // 在地址改变时执行 JavaScript 添加遮罩
                    browser.executeJavaScript(
                            "requestAnimationFrame(function() {" +
                                    "    var mask = document.createElement('div');" +
                                    "    mask.id = 'loading-mask';" +
                                    "    mask.style.position = 'fixed';" +
                                    "    mask.style.top = '0';" +
                                    "    mask.style.left = '0';" +
                                    "    mask.style.width = '100%';" +
                                    "    mask.style.height = '100%';" +
                                    "    mask.style.backgroundColor = '#2B2B2B';" +
                                    "    mask.style.zIndex = '9999';" +
                                    "    document.body.appendChild(mask);" +
                                    "});",
                            browser.getURL(),
                            0
                    );
                }
            }
        }, jbCefBrowser.getCefBrowser());
        jbCefClient.addLoadHandler(new CefLoadHandlerAdapter() {

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                // 确保只有主框架加载完成时才执行脚本
                if (frame.isMain() && black) {
                    // 要注入的JavaScript代码
                    String script = """
                    // 在页面加载时隐藏所有内容，显示加载提示
                    document.body.style.display = 'none';
                    
                    // 创建并显示一个加载提示
                    const loadingDiv = document.createElement('div');
                    loadingDiv.id = 'loading';
                    loadingDiv.style.position = 'fixed';
                    loadingDiv.style.top = '0';
                    loadingDiv.style.left = '0';
                    loadingDiv.style.width = '100vw';
                    loadingDiv.style.height = '100vh';
                    loadingDiv.style.backgroundColor = 'rgba(0, 0, 0, 0.7)';
                    loadingDiv.style.color = '#fff';
                    loadingDiv.style.fontSize = '24px';
                    loadingDiv.style.display = 'flex';
                    loadingDiv.style.alignItems = 'center';
                    loadingDiv.style.justifyContent = 'center';
                    loadingDiv.innerText = '加载中...';
                    document.body.appendChild(loadingDiv);
                    
                    // 执行主题修改操作
                    document.body.style.backgroundColor = '#2B2B2B'; // 深灰色背景
                    document.body.style.color = '#A9B7C6'; // 浅灰蓝色文字
        
                    // 处理所有图片元素，添加灰度效果
                    const images = document.querySelectorAll('img');
                    images.forEach(img => {
                        img.style.filter = 'grayscale(100%)'; // 图片变灰
                    });
        
                    // 处理页面头部、导航、页脚等常见元素
                    const headerFooterNav = document.querySelectorAll('header, footer, nav');
                    headerFooterNav.forEach(element => {
                        element.style.backgroundColor = '#333333'; // 设置头部、尾部、导航背景为深灰色
                        element.style.color = '#A9B7C6'; // 设置文本颜色为浅灰蓝色
                    });
                    
                      // 处理列表项和卡片
                    const listItems = document.querySelectorAll('.card, .list-item, .article-item, .article-card');
                    listItems.forEach(element => {
                        element.style.backgroundColor = '#2B2B2B'; // 深色主题背景
                        element.style.color = '#A9B7C6'; // 深色主题文字
                    });
        
                    // 处理页面中的所有元素，检查并修改白色背景的元素
                    const elements = document.querySelectorAll('*');
                    elements.forEach(element => {
                        const currentBgColor = window.getComputedStyle(element).backgroundColor;
        
                        // 如果背景是白色或透明，替换为深色主题背景
                        if (currentBgColor === 'rgb(255, 255, 255)' || currentBgColor === 'rgba(255, 255, 255, 1)' || currentBgColor === 'rgba(0, 0, 0, 0)') {
                            element.style.backgroundColor = '#2B2B2B'; // 深色主题背景
                            element.style.color = '#A9B7C6'; // 深色主题文字
                        }
        
                        // 处理选中背景颜色
                        const selectionBgColor = window.getComputedStyle(element).getPropertyValue('background-color');
                        if (selectionBgColor === 'rgb(214, 214, 214)' || selectionBgColor === 'rgba(214, 214, 214, 1)') {
                            element.style.backgroundColor = '#214283'; // 设置为深蓝灰色选中背景
                        }
                        // 移除内联样式
                        if (element.style.backgroundColor === 'rgb(255, 255, 255)' || element.style.backgroundColor === 'rgba(255, 255, 255, 1)') {
                            element.style.backgroundColor = '';
                        }
                        if (element.style.color === 'rgb(0, 0, 0)' || element.style.color === 'rgba(0, 0, 0, 1)') {
                            element.style.color = '';
                        }
                    });
        
                    // 处理iframe，如果有嵌入的frame也进行处理
                    const iframes = document.querySelectorAll('iframe');
                    iframes.forEach(iframe => {
                        try {
                              const iframeDoc = iframe.contentDocument || iframe.contentWindow.document;
                              const iframeBody = iframeDoc.body;
                              iframeBody.style.backgroundColor = '#2B2B2B'; // 修改iframe的背景颜色
                              iframeBody.style.color = '#A9B7C6'; // 修改iframe的文字颜色
        
                              // 处理iframe内的所有元素
                              const iframeElements = iframeDoc.querySelectorAll('*');
                              iframeElements.forEach(element => {
                                  const currentBgColor = window.getComputedStyle(element).backgroundColor;
        
                                  // 如果背景是白色或透明，替换为深色主题背景
                                  if (currentBgColor === 'rgb(255, 255, 255)' || currentBgColor === 'rgba(255, 255, 255, 1)' || currentBgColor === 'rgba(0, 0, 0, 0)') {
                                      element.style.backgroundColor = '#2B2B2B'; // 深色主题背景
                                      element.style.color = '#A9B7C6'; // 深色主题文字
                                  }
        
                                  // 处理选中背景颜色
                                  const selectionBgColor = window.getComputedStyle(element).getPropertyValue('background-color');
                                  if (selectionBgColor === 'rgb(214, 214, 214)' || selectionBgColor === 'rgba(214, 214, 214, 1)') {
                                      element.style.backgroundColor = '#214283'; // 设置为深蓝灰色选中背景
                                  }
        
                                  // 移除内联样式
                                  if (element.style.backgroundColor === 'rgb(255, 255, 255)' || element.style.backgroundColor === 'rgba(255, 255, 255, 1)') {
                                      element.style.backgroundColor = '';
                                  }
                                  if (element.style.color === 'rgb(0, 0, 0)' || element.style.color === 'rgba(0, 0, 0, 1)') {
                                      element.style.color = '';
                                  }
                              });
                          } catch (e) {
                              // 如果iframe跨域无法访问，则跳过
                              console.log("无法访问iframe内容：" + e);
                          }
                    });
                        
                    // 移除已知的广告元素
                    const knownAdSelectors = [
                        '.ad, .ads',
                        '.advertisement',
                        '.ad-container',
                        '.ad-slot',
                        '.ad-unit',
                        '.ad-wrapper',
                        '[id^="google_ads"]', // Google Ads
                        '[class^="ad-"]',
                        '[class*="ad-"]',
                        '[id^="ad-"]',
                        '[id*="ad-"]',
                        '[data-ad-id]',
                        '[data-google-query-id]'
                    ];
                
                    knownAdSelectors.forEach(selector => {
                        const elements = document.querySelectorAll(selector);
                        elements.forEach(element => element.remove());
                    });
                
       // 使用 MutationObserver 监控 DOM 变化并移除新添加的广告元素
            const observer = new MutationObserver((mutationsList, observer) => {
                mutationsList.forEach(mutation => {
                    if (mutation.type === 'childList') {
                        mutation.addedNodes.forEach(node => {
                            if (node.nodeType === Node.ELEMENT_NODE) {
                                knownAdSelectors.forEach(selector => {
                                    const elements = node.querySelectorAll(selector);
                                    elements.forEach(element => element.remove());
                                });

                                // 处理新添加的元素
                                const newElements = node.querySelectorAll('*');
                                newElements.forEach(element => {
                                    const currentBgColor = window.getComputedStyle(element).backgroundColor;

                                    // 如果背景是白色或透明，替换为深色主题背景
                                    if (currentBgColor === 'rgb(255, 255, 255)' || currentBgColor === 'rgba(255, 255, 255, 1)' || currentBgColor === 'rgba(0, 0, 0, 0)') {
                                        element.style.backgroundColor = '#2B2B2B'; // 深色主题背景
                                        element.style.color = '#A9B7C6'; // 深色主题文字
                                    }

                                    // 处理选中背景颜色
                                    const selectionBgColor = window.getComputedStyle(element).getPropertyValue('background-color');
                                    if (selectionBgColor === 'rgb(214, 214, 214)' || selectionBgColor === 'rgba(214, 214, 214, 1)') {
                                        element.style.backgroundColor = '#214283'; // 设置为深蓝灰色选中背景
                                    }

                                    // 移除内联样式
                                    if (element.style.backgroundColor === 'rgb(255, 255, 255)' || element.style.backgroundColor === 'rgba(255, 255, 255, 1)') {
                                        element.style.backgroundColor = '';
                                    }
                                    if (element.style.color === 'rgb(0, 0, 0)' || element.style.color === 'rgba(0, 0, 0, 1)') {
                                        element.style.color = '';
                                    }
                                });
                            }
                        });
                    }
                });
            });
                
                    observer.observe(document.body, { childList: true, subtree: true });
                    
                    // 函数：移除所有链接的 target="_blank"
                    function removeBlankTargets() {
                        document.querySelectorAll('a[target="_blank"]').forEach(link => {
                            link.removeAttribute('target');
                        });
                    }
                
                    // 初始调用：处理当前页面已有的链接
                    removeBlankTargets();
                
                    // 使用 MutationObserver 监听 DOM 的变化
                    const observerBlank = new MutationObserver(mutations => {
                        mutations.forEach(mutation => {
                            if (mutation.type === "childList") {
                                // 对新增的节点检查并移除 target="_blank"
                                mutation.addedNodes.forEach(node => {
                                    if (node.nodeType === 1) { // 确保是元素节点
                                        // 如果是 <a> 元素，直接移除 target="_blank"
                                        if (node.tagName === "A" && node.target === "_blank") {
                                            node.removeAttribute('target');
                                        }
                                        // 如果有嵌套的 <a>，移除其 target="_blank"
                                        node.querySelectorAll?.('a[target="_blank"]').forEach(link => {
                                            link.removeAttribute('target');
                                        });
                                    }
                                });
                            }
                        });
                    });
                
                    // 开始监听整个文档的变化
                    observerBlank.observe(document.body, {
                        childList: true, // 监听直接子节点的变化
                        subtree: true    // 监听所有后代节点
                    });
                            
                    
                    // 在主题应用完成后，隐藏加载提示并显示页面内容
                    document.body.style.display = ''; // 显示页面内容
                    document.getElementById('loading').style.display = 'none'; // 隐藏加载提示
                     // 使用 MutationObserver 监控页面加载完成
                    const loadObserver = new MutationObserver((mutationsList, observer) => {
                        mutationsList.forEach(mutation => {
                            if (mutation.type === 'childList') {
                                mutation.addedNodes.forEach(node => {
                                    if (node.nodeType === Node.ELEMENT_NODE) {
                                        // 检查页面是否已经加载完成
                                        if (document.readyState === 'complete') {
                                            // 在主题应用完成后，隐藏加载提示并显示页面内容
                                            document.body.style.display = ''; // 显示页面内容
                                            document.getElementById('loading').style.display = 'none'; // 隐藏加载提示
                                            document.getElementById('loading-mask').remove();
                                        }
                                    }
                                });
                            }
                        });
                    });
    
                    loadObserver.observe(document, { childList: true, subtree: true });
                """;

                    // 在浏览器中执行 JavaScript
                    browser.getMainFrame().executeJavaScript(script, frame.getURL(), 0);
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                LOG.error("Load failed with error code: " + errorCode + ", error text: " + errorText + ", URL: " + failedUrl);
            }
        }, jbCefBrowser.getCefBrowser());

        // 将 JBCefBrowser 的UI控件设置到Panel中
        panel.add(jbCefBrowser.getComponent(), BorderLayout.CENTER);

        // 添加地址栏
        JTextField urlField = new JTextField("https://www.baidu.com");
        JButton loadButton = new JButton("Load");
        loadButton.addActionListener(e -> jbCefBrowser.loadURL(urlField.getText()));

        // 支持回车键加载网页
        urlField.addActionListener(e -> jbCefBrowser.loadURL(urlField.getText()));
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel browserPanel = new JPanel(new BorderLayout());
        JPanel browserButtonPanel = new JPanel(new BorderLayout());
        browserPanel.add(browserButtonPanel, BorderLayout.WEST);
        browserPanel.add(urlField, BorderLayout.CENTER);
        browserPanel.add(loadButton, BorderLayout.EAST);
        topPanel.add(browserPanel, BorderLayout.CENTER);
        JButton backButton = new JButton("Back");
        backButton.addActionListener(e -> jbCefBrowser.getCefBrowser().goBack());
        browserButtonPanel.add(backButton, BorderLayout.WEST);

        JButton forwardButton = new JButton("Forward");
        forwardButton.addActionListener(e -> jbCefBrowser.getCefBrowser().goForward());
        browserButtonPanel.add(forwardButton, BorderLayout.EAST);

        JButton blackButton = new JButton(black ? "White" : "Black");
        blackButton.addActionListener(e -> {
            black = !black;
            blackButton.setText(black ? "White" : "Black");
            jbCefBrowser.getCefBrowser().reload();
        });

        topPanel.add(blackButton, BorderLayout.EAST);

//        JSlider opacitySlider = new JSlider(0, 100, 0); // 0% to 100%
//        opacitySlider.addChangeListener(e -> {
//            float opacity = opacitySlider.getValue() / 100f;
//            jbCefBrowser.getComponent().setOpaque(false);
//            jbCefBrowser.getComponent().setBackground(new Color(0, 0, 0, (int) (255 * opacity)));
//        });
//        topPanel.add(opacitySlider, BorderLayout.EAST);

        JPanel bookmarkPanel = new JPanel(new BorderLayout());

        // 添加折叠按钮
        JToggleButton toggleBookmarkPanelButton = new JToggleButton("Toggle Bookmarks");
        toggleBookmarkPanelButton.addActionListener(e -> {
            bookmarkPanel.setVisible(toggleBookmarkPanelButton.isSelected());
        });
        topPanel.add(toggleBookmarkPanelButton, BorderLayout.WEST); // 或者添加到其他合适的位置

        panel.add(topPanel, BorderLayout.NORTH);

        // 读取收藏的网址
        readBookmarks();

        // 添加书签管理
        DefaultListModel<String> bookmarkListModel = new DefaultListModel<>();
        for (String bookmark : bookmarks) {
            bookmarkListModel.addElement(bookmark);
        }
        JList<String> bookmarkList = new JList<>(bookmarkListModel);
        JButton addBookmarkButton = new JButton("Add Bookmark");
        addBookmarkButton.addActionListener(e -> {
            String url = urlField.getText();
            if (!bookmarks.contains(url)) {
                bookmarks.add(url);
                bookmarkListModel.addElement(url);
                saveBookmarks();
            }
        });

        JButton deleteBookmarkButton = new JButton("Delete Bookmark");
        deleteBookmarkButton.addActionListener(e -> {
            int selectedIndex = bookmarkList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedUrl = bookmarkList.getSelectedValue();
                bookmarks.remove(selectedUrl);
                bookmarkListModel.remove(selectedIndex);
                saveBookmarks();
            }
        });

        bookmarkList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUrl = bookmarkList.getSelectedValue();
                if (selectedUrl != null) {
                    jbCefBrowser.loadURL(selectedUrl);
                    urlField.setText(selectedUrl);
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addBookmarkButton);
        buttonPanel.add(deleteBookmarkButton);

        bookmarkPanel.add(new JScrollPane(bookmarkList), BorderLayout.CENTER);
        bookmarkPanel.add(buttonPanel, BorderLayout.SOUTH);

        // 初始化时书签面板默认显示
        bookmarkPanel.setVisible(true);

        panel.add(bookmarkPanel, BorderLayout.WEST);
    }


    private void readBookmarks() {
        Path path = Paths.get(PathManager.getPluginsPath(), "browser", BOOKMARKS_FILE);
        File file = path.toFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Gson gson = new Gson();
                Type listType = new TypeToken<List<String>>() {}.getType();
                bookmarks = gson.fromJson(reader, listType);
            } catch (IOException e) {
                LOG.error("Failed to read bookmarks", e);
            }
        }
    }

    private void saveBookmarks() {
        Path path = Paths.get(PathManager.getPluginsPath(), "browser", BOOKMARKS_FILE);
        File file = path.toFile();
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new Gson();
            gson.toJson(bookmarks, writer);
        } catch (IOException e) {
            LOG.error("Failed to save bookmarks", e);
        }
    }
}
