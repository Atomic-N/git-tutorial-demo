<%@ page import="com.aitef.forum.model.service.DirectMessageService" %>
<%@ page import="com.aitef.forum.model.entity.DirectMessageThread" %>
<%@ page import="java.util.List" %>
<%@ page pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>私信系统测试</title>
</head>
<body>
<h1>私信系统测试页面</h1>
<%
    try {
        DirectMessageService service = new DirectMessageService();
        Long userId = 1L; // 测试用户ID
        out.println("<h2>开始测试...</h2>");

        List<DirectMessageThread> threads = service.listThreads(userId);
        out.println("<p>成功获取到 " + threads.size() + " 个会话</p>");

        for (DirectMessageThread thread : threads) {
            out.println("<div style='border:1px solid #ccc; padding:10px; margin:10px 0;'>");
            out.println("<p>Thread ID: " + thread.getId() + "</p>");
            out.println("<p>Partner: " + thread.getPartnerDisplayName() + " (@" + thread.getPartnerUsername() + ")</p>");
            out.println("<p>Latest: " + thread.getLatestPreview() + "</p>");
            out.println("<p>Unread: " + thread.isUnread() + " (" + thread.getUnreadCount() + ")</p>");
            out.println("</div>");
        }
    } catch (Exception e) {
        out.println("<h2 style='color:red;'>发生错误：</h2>");
        out.println("<pre style='background:#f5f5f5; padding:10px;'>");
        e.printStackTrace(new java.io.PrintWriter(out));
        out.println("</pre>");
    }
%>
</body>
</html>
