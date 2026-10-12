<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" language="java" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!doctype html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="Cache-Control" content="no-store, no-cache, must-revalidate, max-age=0">
    <meta http-equiv="Pragma" content="no-cache">
    <meta http-equiv="Expires" content="0">
    <title><c:out value="${pageTitle != null ? pageTitle : siteName}"/> - <c:out value="${siteName}"/></title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/app.css?v=${empty assetVersion ? '20260612-sidebarfix-2' : assetVersion}">
    <script>
        window.APP_CONTEXT = "${pageContext.request.contextPath}";
        window.DEFAULT_AVATAR_URL = "${pageContext.request.contextPath}/static/images/default-avatar.svg";
        window.AUTO_SAVE_DRAFTS = ${empty currentPreference || currentPreference.autoSaveDrafts ? 'true' : 'false'};
        window.CSRF_TOKEN = "${csrfToken}";
        window.toggleSidebarFallback = function (event) {
            if (event) event.preventDefault();
            const shell = document.querySelector(".app-shell");
            const sidebar = document.getElementById("appSidebar");
            const toggle = document.querySelector(".sidebar-toggle");
            const content = document.querySelector(".content");
            if (!shell || !toggle) return false;

            const collapsed = !shell.classList.contains("sidebar-collapsed");
            shell.classList.toggle("sidebar-collapsed", collapsed);
            shell.style.gridTemplateColumns = collapsed ? "0 minmax(0, 1fr)" : "";
            toggle.setAttribute("aria-expanded", collapsed ? "false" : "true");
            toggle.setAttribute("aria-label", collapsed ? "展开侧边栏" : "收起侧边栏");
            try {
                window.localStorage.setItem("ai-tef-sidebar-collapsed", collapsed ? "true" : "false");
            } catch (ignored) {
            }

            if (sidebar) {
                sidebar.toggleAttribute("hidden", collapsed);
                sidebar.setAttribute("aria-hidden", collapsed ? "true" : "false");
                sidebar.style.display = collapsed ? "none" : "";
                sidebar.style.width = collapsed ? "0" : "";
                sidebar.style.maxWidth = collapsed ? "0" : "";
                sidebar.style.minWidth = collapsed ? "0" : "";
                sidebar.style.padding = collapsed ? "0" : "";
                sidebar.style.borderRightWidth = collapsed ? "0" : "";
                sidebar.style.overflow = collapsed ? "hidden" : "";
            }

            if (content) {
                content.style.gridColumn = collapsed ? "1 / -1" : "";
            }
            return false;
        };
    </script>
    <script defer src="https://unpkg.com/lucide@latest/dist/umd/lucide.min.js"></script>
    <script defer src="https://cdn.jsdelivr.net/npm/axios@1.7.7/dist/axios.min.js"></script>
    <script defer charset="UTF-8" src="${pageContext.request.contextPath}/static/js/forum.js?v=${empty assetVersion ? '20260624-like-sync-1' : assetVersion}"></script>
</head>
<body>
<header class="topbar">
    <div class="topbar-left">
        <button class="icon-button sidebar-toggle" type="button" aria-label="收起侧边栏" aria-controls="appSidebar" aria-expanded="true" onclick="return window.toggleSidebarFallback ? window.toggleSidebarFallback(event) : false;"><i data-lucide="menu"></i></button>
        <a class="brand" href="${pageContext.request.contextPath}/latest">
            <span class="brand-mark"><span></span></span>
            <span><c:out value="${siteName}"/></span>
        </a>
    </div>
    <div class="topbar-actions">
        <a class="icon-link" href="${pageContext.request.contextPath}/search" title="搜索"><i data-lucide="search"></i></a>
        <c:choose>
            <c:when test="${not empty currentUser}">
                <a class="notification-link" href="${pageContext.request.contextPath}/notifications" title="通知">
                    <i data-lucide="message-circle"></i>
                    <c:if test="${unreadNotificationCount > 0}">
                        <span class="dot-count">${unreadNotificationCount}</span>
                    </c:if>
                </a>
                <c:if test="${allowPrivateMessages}">
                    <a class="notification-link" href="${pageContext.request.contextPath}/messages" title="私信">
                        <i data-lucide="mail"></i>
                        <c:if test="${unreadDirectMessageCount > 0}">
                            <span class="dot-count">${unreadDirectMessageCount}</span>
                        </c:if>
                    </a>
                </c:if>
                <a class="avatar-mini" href="${pageContext.request.contextPath}/u/${currentUser.username}">
                    <img src="${currentUser.avatarUrl}" alt="">
                </a>
            </c:when>
            <c:otherwise>
                <a class="btn btn-ghost" href="${pageContext.request.contextPath}/login">登录</a>
                <a class="btn btn-primary" href="${pageContext.request.contextPath}/register">注册</a>
            </c:otherwise>
        </c:choose>
    </div>
</header>
<div class="app-shell">
    <aside class="sidebar" id="appSidebar">
        <nav class="side-section">
            <a class="side-link ${activeNav == 'topics' ? 'active' : ''}" href="${pageContext.request.contextPath}/latest"><i data-lucide="layers-3"></i><span>话题</span><c:if test="${not empty currentUser && unreadTopicCount > 0}"><b></b></c:if></a>
            <c:if test="${not empty currentUser}">
                <a class="side-link ${activeNav == 'profile' ? 'active' : ''}" href="${pageContext.request.contextPath}/u/${currentUser.username}"><i data-lucide="user"></i><span>我的帖子</span></a>
                <a class="side-link ${activeNav == 'notifications' ? 'active' : ''}" href="${pageContext.request.contextPath}/notifications"><i data-lucide="inbox"></i><span>我的消息</span></a>
                <c:if test="${allowPrivateMessages}">
                    <a class="side-link ${activeNav == 'messages' ? 'active' : ''}" href="${pageContext.request.contextPath}/messages"><i data-lucide="mail"></i><span>私信</span></a>
                </c:if>
                <a class="side-link" href="${pageContext.request.contextPath}/bookmarks"><i data-lucide="bookmark"></i><span>书签</span></a>
            </c:if>
            <a class="side-link ${activeNav == 'search' ? 'active' : ''}" href="${pageContext.request.contextPath}/search"><i data-lucide="search"></i><span>搜索</span></a>
            <a class="side-link ${activeNav == 'tags' ? 'active' : ''}" href="${pageContext.request.contextPath}/tags"><i data-lucide="tags"></i><span>标签</span></a>
            <a class="side-link ${activeNav == 'badges' ? 'active' : ''}" href="${pageContext.request.contextPath}/badges"><i data-lucide="award"></i><span>徽章</span></a>
            <a class="side-link ${activeNav == 'chat' ? 'active' : ''}" href="${pageContext.request.contextPath}/chat"><i data-lucide="messages-square"></i><span>聊天频道</span></a>
            <c:if test="${not empty currentUser && currentUser.admin}">
                <a class="side-link ${activeNav == 'admin' ? 'active' : ''}" href="${pageContext.request.contextPath}/admin/dashboard"><i data-lucide="shield-check"></i><span>管理后台</span></a>
            </c:if>
        </nav>
        <div class="side-title"><i data-lucide="chevron-down"></i><span>类别</span></div>
        <nav class="side-section category-nav">
            <c:forEach var="cat" items="${categories}">
                <a class="side-link ${category != null && category.id == cat.id ? 'active' : ''}" href="${pageContext.request.contextPath}/c/${cat.slug}">
                    <i data-lucide="${cat.icon}" style="color:${cat.color}"></i>
                    <span><c:out value="${cat.name}"/></span>
                    <c:if test="${not empty currentUser && unreadCategoryCounts[cat.id] > 0}"><b></b></c:if>
                </a>
            </c:forEach>
        </nav>
        <div class="sidebar-bottom">
            <a class="chat-entry" href="${pageContext.request.contextPath}/chat"><i data-lucide="messages-square"></i><span>聊天</span></a>
            <c:if test="${not empty currentUser}">
                <form action="${pageContext.request.contextPath}/logout" method="post">
                    <input type="hidden" name="csrfToken" value="${csrfToken}">
                    <button class="side-tool" type="submit"><i data-lucide="log-out"></i><span>退出</span></button>
                </form>
            </c:if>
        </div>
    </aside>
    <main class="content">
        <div class="notice-strip"><strong><c:out value="${siteName}"/></strong> · <c:out value="${noticeText}"/> <a href="${pageContext.request.contextPath}${communityGuidelinesUrl}">《社区准则》</a></div>
